package logisticspipes.crafting;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    static final int ITEM_CAPACITY = 65536;
    static final int FLUID_CAPACITY = 16000000;
    private static final List<WeakReference<PatternCraftingBatchOutputs>> STORES = new ArrayList<>();

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
        if (batches.size() >= 1024) return false;
        if (!canUseTargets(plan)) return false;
        PatternRecipeSnapshot recipe = module.getPatternRecipe(plan.pattern());
        Batch batch = new Batch(plan, module.getEffectiveBlockingMode());
        long items = occupied(false);
        long fluids = occupied(true);
        for (int slot = 0; slot < recipe.getResultSlotCount(); slot++) {
            IPatternStack output = recipe.getOutput(slot);
            if (output == null || output.getAmount() <= 0) continue;
            long amount = (long) output.getAmount() * plan.sets();
            boolean fluid = PatternStackHelper.isFluid(output);
            if (fluid) fluids += amount;
            else items += amount;
            if (items > ITEM_CAPACITY || fluids > FLUID_CAPACITY || amount > Integer.MAX_VALUE) return false;
            PatternByproductTarget target = new PatternByproductTarget(
                    plan.patternSlot(),
                    slot,
                    fluid ? recipe.getFluidByproductSatelliteId(slot) : recipe.getByproductSatelliteId(slot),
                    fluid ? recipe.getFluidByproductSatelliteUuid(slot) : recipe.getByproductSatelliteUuid(slot),
                    fluid,
                    plan.ownerReference());
            if (target.isConfigured() && satellites.resolve(target) == null) {
                module.debugEventThrottled(
                        "BUFFER",
                        "slot=%d waiting for output satellite at output=%d",
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
        Iterator<WeakReference<PatternCraftingBatchOutputs>> stores = STORES.iterator();
        while (stores.hasNext()) {
            PatternCraftingBatchOutputs store = stores.next().get();
            if (store == null) {
                stores.remove();
                continue;
            }
            if (store.pipe.container == null || store.pipe.container.isInvalid()) continue;
            for (Batch active : store.batches.values()) {
                if (!active.drained() && !active.plan.batchReference().equals(plan.batchReference())
                        && plan.sharesTargets(active.plan)) {
                    if (!active.committed) return false;
                    var mode = module.getEffectiveBlockingMode();
                    if (mode == PipeItemsPatternCraftingLogistics.BlockingMode.BLOCKING
                            || active.mode == PipeItemsPatternCraftingLogistics.BlockingMode.BLOCKING)
                        return false;
                    if ((mode == PipeItemsPatternCraftingLogistics.BlockingMode.SMART
                            || active.mode == PipeItemsPatternCraftingLogistics.BlockingMode.SMART)
                            && !plan.sameRecipe(active.plan))
                        return false;
                }
            }
        }
        return true;
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

    private long occupied(boolean fluid) {
        long amount = 0;
        for (Batch batch : batches.values()) for (Output output : batch.outputs) if (output.target.isFluid() == fluid)
            amount += output.remaining + (long) output.inFlight + output.available;
        return amount;
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
            int replacementRoom = (int) Math.max(
                    0,
                    (output.target.isFluid() ? FLUID_CAPACITY : ITEM_CAPACITY) - occupied(output.target.isFluid()));
            int accepted = Math
                    .min(output.inFlight + Math.min(output.awaitingReplacement, replacementRoom), stack.getAmount());
            int fromTransit = Math.min(output.inFlight, accepted);
            output.inFlight -= fromTransit;
            output.awaitingReplacement -= accepted - fromTransit;
            output.available += accepted;
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
                int amount = Math.min(maxAmount, output.available);
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

    boolean manages(LogisticsOrder order) {
        PatternCraftingOrder source = PatternCraftingInstanceRegistry.find(order);
        if (source != null) return source.usesBatchExecution();
        PatternByproductTarget target = order.getByproductTarget();
        PatternCraftingReference owner = target != null && target.getSourceReference() != null
                ? target.getSourceReference()
                : order.getCraftingReference();
        if (owner != null && managedJobs.contains(owner.instanceId())) return true;
        for (Batch batch : batches.values())
            for (Output output : batch.outputs) if (matches(batch, output, order)) return true;
        return false;
    }

    void forgetJob(UUID instance) {
        if (!hasInstance(instance)) managedJobs.remove(instance);
    }

    void manageJob(UUID instance) {
        managedJobs.add(instance);
    }

    private logisticspipes.utils.tuples.Pair<Integer, Integer> storageReply(IPatternStack stack, int wanted) {
        if (stack instanceof PatternFluidStack fluid) {
            return logisticspipes.proxy.SimpleServiceLocator.logisticsFluidManager.getBestReply(
                    fluid.getFluid().makeFluidStack(wanted),
                    pipe.getRouter(),
                    java.util.Collections.emptyList());
        }
        List<Integer> exclude = new ArrayList<>();
        for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry.networkPatternModules(pipe.getRouter()))
            exclude.add(provider.getRouter().getSimpleID());
        var reply = pipe.hasDestination(PatternStackHelper.asSolidStack(stack).getItem(), true, exclude);
        return reply == null ? new logisticspipes.utils.tuples.Pair<>(0, 0)
                : new logisticspipes.utils.tuples.Pair<>(
                        reply.getValue1(),
                        reply.getValue2().maxNumberOfItems <= 0 ? wanted
                                : Math.min(wanted, reply.getValue2().maxNumberOfItems));
    }

    int storageRoom(IPatternStack stack, int wanted) {
        return Math.min(wanted, storageReply(stack, wanted).getValue2());
    }

    logisticspipes.logisticspipes.IRoutedItem sendToStorage(IPatternStack stack) {
        var reply = storageReply(stack, stack.getAmount());
        if (reply.getValue2() < stack.getAmount()) return null;
        ItemStack routed = stack instanceof PatternFluidStack fluid
                ? logisticspipes.proxy.SimpleServiceLocator.logisticsFluidManager
                        .getFluidContainer(fluid.getFluid().makeFluidStack(stack.getAmount())).makeNormalStack()
                : PatternStackHelper.asSolidStack(stack).makeNormalStack();
        return pipe.sendStack(
                routed,
                reply.getValue1(),
                logisticspipes.pipes.basic.CoreRoutedPipe.ItemSendMode.Normal,
                null);
    }

    void putBack(LogisticsOrder order, IPatternStack stack) {
        for (Batch batch : batches.values()) for (Output output : batch.outputs) if (matches(batch, output, order)) {
            output.available += stack.getAmount();
            module.markCraftingStateDirty();
            return;
        }
        throw new IllegalStateException("Collected output lost its producing batch");
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
            return "Waiting: delivering collected outputs or storage space";
        }
        return null;
    }

    long unreservedAmount(java.util.Set<UUID> admitted, boolean fluid) {
        long amount = 0;
        for (Batch batch : batches.values()) {
            if (admitted.contains(batch.plan.ownerReference().instanceId())) continue;
            for (Output output : batch.outputs) if (output.target.isFluid() == fluid)
                amount += output.remaining + (long) output.inFlight + output.available;
        }
        return amount;
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
    }

    void returnUnclaimedOutputs() {
        Map<UUID, Boolean> unclaimed = new LinkedHashMap<>();
        for (Batch batch : batches.values()) {
            UUID instance = batch.plan.ownerReference().instanceId();
            boolean noConsumers = unclaimed.computeIfAbsent(instance, id -> {
                if (!PatternCraftingInstanceRegistry.ordersForInstance(id).isEmpty()) return false;
                for (ModulePatternCrafting provider : PatternCraftingMonitorRegistry.networkPatternModules(
                        pipe.getRouter()))
                    if (provider.hasNonBatchWorkspaceWork(id)) return false;
                return true;
            });
            if (!batch.cancelled && !noConsumers) continue;
            for (Output output : batch.outputs) {
                if (output.awaitingReplacement > 0) {
                    output.awaitingReplacement = 0;
                    module.markCraftingStateDirty();
                }
                if (output.available <= 0) continue;
                IPatternStack stack = PatternStackHelper.copyWithAmount(
                        output.stack,
                        Math.min(
                                output.available,
                                output.target.isFluid()
                                        ? logisticspipes.config.Configs.MAX_LOGISTICS_FLUID_TRANSPORT_INNER_CAPACITY / 2
                                        : PatternStackHelper.asSolidStack(output.stack).getItem().getMaxStackSize()));
                int room = storageRoom(stack, stack.getAmount());
                if (room <= 0) continue;
                stack = PatternStackHelper.copyWithAmount(stack, room);
                if (sendToStorage(stack) == null) continue;
                output.available -= stack.getAmount();
                module.markCraftingStateDirty();
            }
        }
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
        out.append("  producing batches: ").append(batches.size()).append(" reservedItems=").append(occupied(false))
                .append(" reservedFluid=").append(occupied(true)).append('\n');
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
