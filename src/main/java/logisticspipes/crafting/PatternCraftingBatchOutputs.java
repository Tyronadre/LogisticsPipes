package logisticspipes.crafting;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidStack;

import logisticspipes.crafting.pattern.PatternRecipeSnapshot;
import logisticspipes.crafting.patternStack.IPatternStack;
import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternStackHelper;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.order.LogisticsOrder;
import logisticspipes.utils.item.ItemIdentifierStack;

/** Reserves and drains whole producing batches, independently of the lifetime of their consumer orders. */
final class PatternCraftingBatchOutputs {

    // Pipe modules are registered from both client and server threads.
    private static final List<WeakReference<PatternCraftingBatchOutputs>> STORES = new CopyOnWriteArrayList<>();

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private final AdjacentInventoryHandler adjacent;
    private final PatternByproductExtractionTargetCache satellites;
    private final Map<PatternCraftingReference, Batch> batches = new LinkedHashMap<>();
    private final java.util.Set<UUID> managedJobs = new java.util.HashSet<>();

    PatternCraftingBatchOutputs(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            AdjacentInventoryHandler adjacent) {
        this.module = module;
        this.pipe = pipe;
        this.adjacent = adjacent;
        satellites = new PatternByproductExtractionTargetCache(pipe);
        STORES.add(new WeakReference<>(this));
    }

    boolean prepare(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        if (batches.containsKey(plan.batchReference())) return true;
        if (!canUseTargets(plan)) return false;
        PatternRecipeSnapshot recipe = module.getPatternRecipe(plan.pattern());
        Batch batch = new Batch(plan, module.getEffectiveBlockingMode());
        for (int slot = 0; slot < recipe.getResultSlotCount(); slot++) {
            IPatternStack output = recipe.getOutput(slot);
            if (output == null || output.getAmount() <= 0) continue;
            long amount = (long) output.getAmount() * plan.sets();
            boolean fluid = PatternStackHelper.isFluid(output);
            if (amount > Integer.MAX_VALUE) return false;
            PatternByproductTarget target = new PatternByproductTarget(
                    plan.patternSlot(),
                    slot,
                    fluid ? recipe.getFluidByproductSatelliteId(slot) : recipe.getByproductSatelliteId(slot),
                    fluid ? recipe.getFluidByproductSatelliteUuid(slot) : recipe.getByproductSatelliteUuid(slot),
                    fluid,
                    plan.ownerReference());
            if ((target.isConfigured() && satellites.resolve(target) == null)
                    || (!target.isConfigured() && plan.localTarget() == null)) {
                module.debugEventThrottled(
                        "BUFFER",
                        "slot=%d waiting for output target at output=%d",
                        plan.patternSlot(),
                        slot);
                return false;
            }
            batch.outputs.add(new Output(PatternStackHelper.copyWithAmount(output, (int) amount), target));
        }
        if (batch.outputs.isEmpty()) return false;
        batches.put(plan.batchReference(), batch);
        managedJobs.add(plan.ownerReference().instanceId());
        module.markCraftingStateDirty();
        return true;
    }

    /** All pipes sharing a physical input inventory participate in the mode check. */
    boolean canUseTargets(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        for (WeakReference<PatternCraftingBatchOutputs> reference : STORES) {
            PatternCraftingBatchOutputs store = reference.get();
            if (store == null) {
                STORES.remove(reference);
                continue;
            }
            if (store.pipe.container == null || store.pipe.container.isInvalid()
                    || store.pipe.getWorld() != pipe.getWorld())
                continue;
            for (Batch active : store.batches.values()) {
                if (!active.drained() && !active.plan.batchReference().equals(plan.batchReference())
                        && plan.sharesTargets(active.plan)) {
                    if (!active.committed) return false;
                    var mode = module.getEffectiveBlockingMode();
                    if ((mode != PipeItemsPatternCraftingLogistics.BlockingMode.OFF
                            || active.mode != PipeItemsPatternCraftingLogistics.BlockingMode.OFF)
                            && !plan.sameRecipe(active.plan))
                        return false;
                }
            }
        }
        return true;
    }

    /** A second pattern may buffer its 64 sets, but cannot spend already-promised shared machine room. */
    boolean canOrderIntoTargets(PatternCraftingOrder order) {
        PatternSatelliteDispatchHandler.DispatchPlan candidate = module.orderingPlan(order);
        if (candidate == null) return false;
        for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry.networkPatternModules(pipe.getRouter())) {
            for (PatternCraftingOrder other : PatternCraftingInstanceRegistry.ordersForModule(provider)) {
                if (provider == module && other.patternSlot == order.patternSlot) continue;
                if (provider.pendingPatternSets(other.patternSlot) <= 64) continue;
                PatternSatelliteDispatchHandler.DispatchPlan reserved = provider.orderingPlan(other);
                if (reserved != null && candidate.sharesTargets(reserved)) return false;
            }
        }
        return true;
    }

    int firstActivePattern() {
        for (Batch batch : batches.values()) if (!batch.drained()) return batch.plan.patternSlot();
        return -1;
    }

    List<Integer> activePatternSlots() {
        List<Integer> slots = new ArrayList<>();
        for (Batch batch : batches.values())
            if (!batch.drained() && !slots.contains(batch.plan.patternSlot())) slots.add(batch.plan.patternSlot());
        return slots;
    }

    void committed(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        Batch batch = batches.get(plan.batchReference());
        if (batch != null) {
            batch.committed = true;
            module.markCraftingStateDirty();
        }
    }

    void discardPrepared(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        Batch batch = batches.get(plan.batchReference());
        if (batch != null && !batch.committed) {
            batches.remove(plan.batchReference());
            module.markCraftingStateDirty();
        }
    }

    void collect() {
        int itemsLeft = 64;
        int stacksLeft = 16;
        int fluidsLeft = logisticspipes.config.Configs.MAX_LOGISTICS_FLUID_TRANSPORT_INNER_CAPACITY / 2;
        for (Batch batch : new ArrayList<>(batches.values())) {
            if (!batch.committed) continue;
            for (Output output : batch.outputs) {
                if (output.remaining <= 0) continue;
                int wanted = Math.min(
                        output.remaining,
                        output.target.isFluid() ? fluidsLeft
                                : Math.min(
                                        itemsLeft,
                                        PatternStackHelper.asSolidStack(output.stack).getItem().getMaxStackSize()));
                if (wanted <= 0 || stacksLeft <= 0) continue;
                int extracted = 0;
                if (output.target.isConfigured()) {
                    PatternTargetInformation info = PatternTargetInformation.batchOutput(
                            batch.plan.patternSlot(),
                            output.target.getOutputSlot(),
                            batch.plan.batchReference());
                    PatternByproductExtractionResult result = output.target.isFluid()
                            ? satellites.extractFluid(
                                    output.target,
                                    PatternStackHelper.asFluid(output.stack),
                                    wanted,
                                    pipe.getRouter().getSimpleID(),
                                    info)
                            : satellites.extractItem(
                                    output.target,
                                    PatternStackHelper.asSolidStack(output.stack).getItem(),
                                    wanted,
                                    pipe.getRouter().getSimpleID(),
                                    info);
                    extracted = result.amount();
                    output.inFlight += extracted;
                } else if (output.stack instanceof PatternFluidStack fluid) {
                    var tile = batch.plan.localTarget();
                    if (tile != null) {
                        FluidStack drained = adjacent.extractFluid(tile, fluid, wanted);
                        if (drained != null && drained.amount > 0) {
                            extracted = drained.amount;
                        }
                    }
                    output.available += extracted;
                } else if (batch.plan.localTarget() != null) {
                    ItemStack extractedStack = adjacent.extract(
                            batch.plan.localTarget(),
                            new ItemResource(PatternStackHelper.asSolidStack(output.stack), pipe),
                            wanted);
                    extracted = extractedStack == null ? 0 : extractedStack.stackSize;
                    output.available += extracted;
                }
                if (extracted > 0) {
                    output.remaining -= extracted;
                    if (output.target.isFluid()) fluidsLeft -= extracted;
                    else itemsLeft -= extracted;
                    stacksLeft--;
                    module.markCraftingStateDirty();
                }
            }
        }
    }

    boolean arrival(ItemIdentifierStack arrived, PatternTargetInformation info) {
        if (!info.isBatchOutput()) return false;
        Batch batch = batches.get(info.orderReference());
        if (batch == null) {
            returnArrivalToStorage(arrived);
            return true;
        }
        IPatternStack stack = IPatternStack.fromItemStack(arrived.makeNormalStack());
        for (Output output : batch.outputs) {
            if (output.target.getOutputSlot() != info.outputSlot() || stack == null || !output.stack.canMerge(stack))
                continue;
            int accepted = Math.min(output.inFlight + output.awaitingReplacement, stack.getAmount());
            int fromTransit = Math.min(output.inFlight, accepted);
            output.inFlight -= fromTransit;
            output.awaitingReplacement -= accepted - fromTransit;
            output.available += accepted;
            if (batch.cancelled) {
                output.available -= accepted;
                returnArrivalToStorage(arrived);
                module.markCraftingStateDirty();
                return true;
            }
            if (accepted == stack.getAmount()) arrived.setStackSize(0);
            else if (stack instanceof PatternFluidStack fluid) {
                pipe.sendStack(
                        logisticspipes.proxy.SimpleServiceLocator.logisticsFluidManager
                                .getFluidContainer(fluid.getFluid().makeFluidStack(stack.getAmount() - accepted))
                                .makeNormalStack(),
                        -1,
                        logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode.Normal,
                        null);
                arrived.setStackSize(0);
            } else {
                arrived.setStackSize(arrived.getStackSize() - accepted);
                returnArrivalToStorage(arrived);
            }
            module.markCraftingStateDirty();
            return true;
        }
        returnArrivalToStorage(arrived);
        return true;
    }

    private void returnArrivalToStorage(ItemIdentifierStack stack) {
        if (stack.getStackSize() > 0) pipe.sendStack(
                stack.makeNormalStack(),
                -1,
                logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode.Normal,
                null);
        stack.setStackSize(0);
    }

    int missing(PatternTargetInformation info, IPatternStack stack) {
        Batch batch = batches.get(info.orderReference());
        if (batch == null) return 0;
        for (Output output : batch.outputs)
            if (output.target.getOutputSlot() == info.outputSlot() && output.stack.canMerge(stack))
                return output.inFlight + output.awaitingReplacement;
        return 0;
    }

    boolean lost(PatternTargetInformation info, IPatternStack stack) {
        Batch batch = batches.get(info.orderReference());
        if (batch == null || stack == null) return false;
        for (Output output : batch.outputs) {
            if (output.target.getOutputSlot() != info.outputSlot() || !output.stack.canMerge(stack)) continue;
            int lost = Math.min(output.inFlight, stack.getAmount());
            output.inFlight -= lost;
            if (!batch.cancelled) output.awaitingReplacement += lost;
            module.markCraftingStateDirty();
            return !batch.cancelled && lost > 0;
        }
        return false;
    }

    int incoming(IPatternStack stack) {
        long amount = 0;
        for (Batch batch : batches.values()) for (Output output : batch.outputs)
            if (output.stack.canMerge(stack)) amount += output.inFlight + (long) output.awaitingReplacement;
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    IPatternStack take(LogisticsOrder order, int maxAmount) {
        if (maxAmount <= 0) return null;
        for (Batch batch : batches.values()) {
            for (Output output : batch.outputs) {
                if (output.available <= 0 || !matches(batch, output, order)) continue;
                int amount = Math.min(maxAmount, batch.deliverable(output));
                if (amount <= 0) continue;
                output.available -= amount;
                module.markCraftingStateDirty();
                return PatternStackHelper.copyWithAmount(output.stack, amount);
            }
        }
        return null;
    }

    private boolean matches(Batch batch, Output output, LogisticsOrder order) {
        PatternByproductTarget target = order.getByproductTarget();
        if (target == null || target.getPatternSlot() != batch.plan.patternSlot()
                || target.getOutputSlot() != output.target.getOutputSlot()
                || target.isFluid() != output.target.isFluid()
                || !PatternStackHelper.matches(output.stack, order.getAsDisplayItem().getItem()))
            return false;
        PatternCraftingReference source = target.getSourceReference();
        if (source != null) return source.instanceId().equals(batch.plan.ownerReference().instanceId());
        PatternCraftingReference owner = order.getCraftingReference();
        return owner != null && owner.instanceId().equals(batch.plan.ownerReference().instanceId());
    }

    int futureClaims(LogisticsOrder order) {
        PatternByproductTarget target = order.getByproductTarget();
        if (target == null) return 0;
        PatternCraftingReference owner = target.getSourceReference() == null ? order.getCraftingReference()
                : target.getSourceReference();
        if (owner == null) return 0;
        IPatternStack output = target.isFluid()
                ? new PatternFluidStack(((logisticspipes.routing.order.LogisticsFluidOrder) order).getFluid(), 1)
                : new logisticspipes.crafting.patternStack.PatternItemStack(order.getAsDisplayItem());
        return futureClaims(target, output, owner.instanceId());
    }

    int futureClaims(PatternByproductTarget target, IPatternStack output) {
        return target == null || target.getSourceReference() == null ? 0
                : futureClaims(target, output, target.getSourceReference().instanceId());
    }

    private int futureClaims(PatternByproductTarget target, IPatternStack output, UUID producingJob) {
        java.util.Set<PatternCraftingBranch> visited = java.util.Collections
                .newSetFromMap(new java.util.IdentityHashMap<>());
        long amount = 0;
        for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry.networkPatternModules(pipe.getRouter())) {
            for (PatternCraftingOrder pending : PatternCraftingInstanceRegistry.ordersForModule(provider)) {
                for (PatternCraftingBranch branch : pending.ingredientBranches)
                    amount += branch.unrequestedOutputClaims(module, target, output, producingJob, visited);
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    boolean manages(LogisticsOrder order) {
        PatternCraftingOrder source = PatternCraftingInstanceRegistry.find(order);
        if (source != null && source.usesBatchExecution()) return true;
        PatternByproductTarget target = order.getByproductTarget();
        PatternCraftingReference owner = target != null && target.getSourceReference() != null
                ? target.getSourceReference()
                : order.getCraftingReference();
        if (owner != null && managedJobs.contains(owner.instanceId())) return true;
        for (Batch batch : batches.values())
            for (Output output : batch.outputs) if (matches(batch, output, order)) return true;
        return false;
    }

    void manageJob(UUID instance) {
        managedJobs.add(instance);
    }

    logisticspipes.logisticspipes.IRoutedItem sendToStorage(IPatternStack stack) {
        return pipe.sendStack(
                stack.makePatternStack(),
                -1,
                logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode.Normal,
                null);
    }

    boolean hasInstance(UUID instance) {
        for (Batch batch : batches.values()) if (instance.equals(batch.plan.ownerReference().instanceId())) return true;
        return false;
    }

    boolean cancelInstance(UUID instance) {
        boolean changed = false;
        for (Batch batch : batches.values()) {
            if (instance.equals(batch.plan.ownerReference().instanceId())) {
                batch.cancelled = true;
                for (Output output : batch.outputs) output.awaitingReplacement = 0;
                changed = true;
            }
        }
        if (changed) {
            PatternCraftingInstanceRegistry.recordCancellation(instance);
            module.markCraftingStateDirty();
        }
        return changed;
    }

    List<PatternCraftingMonitorEntry> monitorEntries() {
        Map<UUID, List<PatternCraftingMonitorNode>> nodes = new LinkedHashMap<>();
        for (Batch batch : batches.values()) {
            for (Output output : batch.outputs) {
                int pending = output.remaining + output.inFlight + output.available + output.awaitingReplacement;
                if (pending <= 0) continue;
                var display = PatternStackHelper
                        .makeDisplayStack(PatternStackHelper.copyWithAmount(output.stack, pending));
                nodes.computeIfAbsent(batch.plan.ownerReference().instanceId(), ignored -> new ArrayList<>())
                        .add(new PatternCraftingMonitorNode(display, 0, pending, !batch.drained()));
            }
        }
        List<PatternCraftingMonitorEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, List<PatternCraftingMonitorNode>> entry : nodes.entrySet())
            entries.add(new PatternCraftingMonitorEntry(entry.getKey(), entry.getValue()));
        return entries;
    }

    boolean activePattern(int slot) {
        for (Batch batch : batches.values()) if (batch.plan.patternSlot() == slot && !batch.drained()) return true;
        return false;
    }

    String patternStatus(int slot) {
        for (Batch batch : batches.values()) {
            if (batch.plan.patternSlot() != slot) continue;
            if (!batch.committed) return "Waiting: staging complete batch at satellites";
            if (!batch.drained()) return "Doing: crafting and draining complete batch";
            for (Output output : batch.outputs) if (output.inFlight > 0) return "Waiting: batch outputs in transit";
            return "Waiting: delivering complete result sets";
        }
        return null;
    }

    void cleanup() {
        Iterator<Batch> iterator = batches.values().iterator();
        while (iterator.hasNext()) {
            Batch batch = iterator.next();
            if (batch.empty()) {
                iterator.remove();
                module.markCraftingStateDirty();
            }
        }
        managedJobs.removeIf(
                id -> !hasInstance(id) && PatternCraftingInstanceRegistry.ordersForInstance(id).isEmpty()
                        && !module.hasPendingIngredientWork(id));
    }

    void returnUnclaimedOutputs() {
        Map<UUID, Boolean> unclaimed = new LinkedHashMap<>();
        for (Batch batch : batches.values()) {
            UUID instance = batch.plan.ownerReference().instanceId();
            boolean noConsumers = unclaimed.computeIfAbsent(instance, id -> {
                if (!PatternCraftingInstanceRegistry.ordersForInstance(id).isEmpty()) return false;
                for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry.networkPatternModules(
                        pipe.getRouter()))
                    if (provider.hasPendingIngredientWork(id)) return false;
                return true;
            });
            for (Output output : batch.outputs) {
                if ((batch.cancelled || noConsumers) && output.awaitingReplacement > 0) {
                    output.awaitingReplacement = 0;
                    module.markCraftingStateDirty();
                }
                int surplus = batch.cancelled ? output.available : unclaimedAmount(batch, output);
                if (surplus <= 0) continue;
                IPatternStack stack = PatternStackHelper.copyWithAmount(
                        output.stack,
                        Math.min(
                                surplus,
                                output.target.isFluid()
                                        ? logisticspipes.config.Configs.MAX_LOGISTICS_FLUID_TRANSPORT_INNER_CAPACITY / 2
                                        : PatternStackHelper.asSolidStack(output.stack).getItem().getMaxStackSize()));
                sendToStorage(stack);
                output.available -= stack.getAmount();
                module.markCraftingStateDirty();
            }
        }
    }

    /** Protect live consumer orders and promises in subtrees that have not been expanded yet. */
    private int unclaimedAmount(Batch batch, Output output) {
        long claimed = futureClaims(output.target, output.stack, batch.plan.ownerReference().instanceId());
        for (var order : pipe.getItemOrderManager()) if (matches(batch, output, order)) claimed += order.getAmount();
        for (var order : pipe.getPatternFluidOrderManager())
            if (matches(batch, output, order)) claimed += order.getAmount();
        long collected = 0;
        for (Batch other : batches.values()) {
            if (!other.plan.ownerReference().instanceId().equals(batch.plan.ownerReference().instanceId())
                    || other.plan.patternSlot() != batch.plan.patternSlot())
                continue;
            for (Output stock : other.outputs)
                if (stock.target.getOutputSlot() == output.target.getOutputSlot() && stock.stack.canMerge(output.stack))
                    collected += other.deliverable(stock);
        }
        return (int) Math.min(batch.deliverable(output), Math.max(0, collected - claimed));
    }

    void dropContents(net.minecraft.world.World world, int x, int y, int z) {
        PatternStackBufferHandler drops = new PatternStackBufferHandler(() -> {});
        for (Batch batch : batches.values()) for (Output output : batch.outputs) if (output.available > 0) drops.add(
                batch.plan.ownerReference(),
                batch.plan.patternSlot(),
                PatternStackHelper.copyWithAmount(output.stack, output.available));
        drops.dropContents(world, x, y, z);
        batches.clear();
        managedJobs.clear();
    }

    void readFromNBT(NBTTagCompound tag, PatternSatelliteDispatchHandler dispatcher) {
        batches.clear();
        managedJobs.clear();
        NBTTagList jobs = tag.getTagList("patternBatchJobs", 10);
        for (int i = 0; i < jobs.tagCount(); i++)
            managedJobs.add(UUID.fromString(jobs.getCompoundTagAt(i).getString("instance")));
        NBTTagList list = tag.getTagList("patternProducingBatches", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            Batch batch = new Batch(
                    dispatcher.readFromNBT(entry.getCompoundTag("dispatch")),
                    PipeItemsPatternCraftingLogistics.BlockingMode.values()[Math
                            .max(0, Math.min(2, entry.getInteger("mode")))]);
            batch.committed = entry.getBoolean("committed");
            batch.cancelled = entry.getBoolean("cancelled");
            if (batch.cancelled)
                PatternCraftingInstanceRegistry.recordCancellation(batch.plan.ownerReference().instanceId());
            NBTTagList outputs = entry.getTagList("outputs", 10);
            for (int j = 0; j < outputs.tagCount(); j++) {
                NBTTagCompound data = outputs.getCompoundTagAt(j);
                Output output = new Output(
                        IPatternStack.readFromNBT(data),
                        PatternByproductTarget.readFromNBT(data, "target"));
                output.remaining = Math.max(0, data.getInteger("remaining"));
                output.inFlight = Math.max(0, data.getInteger("inFlight"));
                output.available = Math.max(0, data.getInteger("available"));
                output.awaitingReplacement = batch.cancelled ? 0 : Math.max(0, data.getInteger("awaitingReplacement"));
                batch.outputs.add(output);
            }
            batches.put(batch.plan.batchReference(), batch);
            managedJobs.add(batch.plan.ownerReference().instanceId());
        }
    }

    void writeToNBT(NBTTagCompound tag) {
        NBTTagList jobs = new NBTTagList();
        for (UUID job : managedJobs) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("instance", job.toString());
            jobs.appendTag(entry);
        }
        tag.setTag("patternBatchJobs", jobs);
        NBTTagList list = new NBTTagList();
        for (Batch batch : batches.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("dispatch", batch.plan.writeToNBT());
            entry.setInteger("mode", batch.mode.ordinal());
            entry.setBoolean("committed", batch.committed);
            entry.setBoolean("cancelled", batch.cancelled);
            NBTTagList outputs = new NBTTagList();
            for (Output output : batch.outputs) {
                NBTTagCompound data = new NBTTagCompound();
                output.stack.writeToNBT(data);
                output.target.writeToNBT(data, "target");
                data.setInteger("remaining", output.remaining);
                data.setInteger("inFlight", output.inFlight);
                data.setInteger("available", output.available);
                data.setInteger("awaitingReplacement", output.awaitingReplacement);
                outputs.appendTag(data);
            }
            entry.setTag("outputs", outputs);
            list.appendTag(entry);
        }
        tag.setTag("patternProducingBatches", list);
    }

    void appendDebugState(StringBuilder out) {
        out.append("  producing batches: ").append(batches.size()).append('\n');
        for (Batch batch : batches.values()) {
            out.append("    batch=").append(batch.plan.batchReference()).append(" owner=")
                    .append(batch.plan.ownerReference()).append(" slot=").append(batch.plan.patternSlot())
                    .append(" committed=").append(batch.committed).append('\n');
            for (Output output : batch.outputs)
                out.append("      ").append(output.stack).append(" outputSlot=").append(output.target.getOutputSlot())
                        .append(" remaining=").append(output.remaining).append(" inFlight=").append(output.inFlight)
                        .append(" available=").append(output.available).append('\n');
            for (Output output : batch.outputs) if (output.awaitingReplacement > 0)
                out.append("      awaiting replacement=").append(output.awaitingReplacement).append('\n');
        }
    }

    private static final class Batch {

        final PatternSatelliteDispatchHandler.DispatchPlan plan;
        final PipeItemsPatternCraftingLogistics.BlockingMode mode;
        final List<Output> outputs = new ArrayList<>();
        boolean committed;
        boolean cancelled;

        Batch(PatternSatelliteDispatchHandler.DispatchPlan plan, PipeItemsPatternCraftingLogistics.BlockingMode mode) {
            this.plan = plan;
            this.mode = mode;
        }

        // Collect partial outputs so a small byproduct hatch cannot stop the machine. Only complete result
        // sets become available to consumers. Dispatched recipe quantities stay owned until then.
        int completedSets() {
            int sets = plan.sets();
            int complete = sets;
            for (Output output : outputs) {
                int perSet = output.stack.getAmount() / sets;
                int received = output.stack.getAmount() - output.remaining
                        - output.inFlight
                        - output.awaitingReplacement;
                complete = Math.min(complete, Math.max(0, received / perSet));
            }
            return complete;
        }

        int deliverable(Output output) {
            int received = output.stack.getAmount() - output.remaining - output.inFlight - output.awaitingReplacement;
            int delivered = received - output.available;
            return Math.max(
                    0,
                    Math.min(output.available, completedSets() * (output.stack.getAmount() / plan.sets()) - delivered));
        }

        boolean drained() {
            if (!committed) return false;
            for (Output output : outputs) if (output.remaining > 0) return false;
            return true;
        }

        boolean empty() {
            if (!drained()) return false;
            for (Output output : outputs)
                if (output.inFlight > 0 || output.available > 0 || output.awaitingReplacement > 0) return false;
            return true;
        }
    }

    private static final class Output {

        final IPatternStack stack;
        final PatternByproductTarget target;
        int remaining;
        int inFlight;
        int available;
        int awaitingReplacement;

        Output(IPatternStack stack, PatternByproductTarget target) {
            this.stack = stack;
            this.target = target;
            remaining = stack.getAmount();
        }
    }

    static void clear() {
        STORES.clear();
    }
}
