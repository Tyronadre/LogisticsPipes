package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;

import logisticspipes.crafting.pattern.PatternRecipeSnapshot;
import logisticspipes.crafting.patternStack.IPatternStack;
import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternItemStack;
import logisticspipes.crafting.patternStack.PatternStackHelper;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Builds and dispatches complete buffered ingredient sets for pattern crafting.
 * <p>
 * The main module owns the buffers and staged requests; this handler owns the target split between the local adjacent
 * inventory and configured pattern satellites.
 */
final class PatternSatelliteDispatchHandler {

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private final AdjacentInventoryHandler adjacentInventory;

    PatternSatelliteDispatchHandler(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            AdjacentInventoryHandler adjacentInventory) {
        this.module = module;
        this.pipe = pipe;
        this.adjacentInventory = adjacentInventory;
    }

    /**
     * Finds the largest buffered set count that can be sent to all configured targets right now.
     */
    DispatchPlan findInsertableBufferedPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern,
            int maxSets) {
        maxSets = Math.min(64, maxSets);
        PatternRecipeSnapshot recipe = module.getPatternRecipe(pattern);
        for (int slot = 0; slot < recipe.getResultSlotCount(); slot++) {
            IPatternStack output = recipe.getOutput(slot);
            if (output != null && output.getAmount() > 0)
                maxSets = Math.min(maxSets, Integer.MAX_VALUE / output.getAmount());
        }
        for (int sets = maxSets; sets > 0; sets--) {
            List<PatternIngredientAssignment> assignments = module
                    .buildBufferedIngredientPlan(ownerReference, patternSlot, pattern, sets);
            if (assignments == null) {
                continue;
            }
            DispatchPlan dispatchPlan = buildDispatchPlan(ownerReference, patternSlot, pattern, assignments);
            if (dispatchPlan != null && dispatchPlan.canDispatch()) {
                return dispatchPlan;
            }
        }
        return null;
    }

    /**
     * Returns how many full pattern sets are represented by a concrete dispatch plan.
     */
    int insertedSetsFromPlan(ItemStack pattern, List<PatternIngredientAssignment> plan) {
        if (pattern == null || plan == null || plan.isEmpty()) {
            return 0;
        }
        PatternRecipeSnapshot configuredPattern = module.getPatternRecipe(pattern);
        if (configuredPattern == null) {
            return 0;
        }
        int sets = Integer.MAX_VALUE;
        for (PatternIngredientAssignment assignment : plan) {
            IPatternStack ingredient = configuredPattern.getInput(assignment.inputSlot());
            if (ingredient == null || ingredient.getAmount() <= 0) {
                continue;
            }
            sets = Math.min(sets, assignment.stack().getAmount() / ingredient.getAmount());
        }
        return sets == Integer.MAX_VALUE ? 0 : sets;
    }

    /**
     * Calculates how many complete sets can be inserted into the currently configured local and satellite targets.
     */
    int maxDispatchableSets(PatternCraftingReference ownerReference, ItemStack pattern, int maxSets) {
        if (pattern == null || maxSets <= 0) {
            return 0;
        }
        int low = 0;
        int high = maxSets;
        while (low < high) {
            int mid = low + (int) (((long) high - low + 1) / 2);
            List<PatternIngredientAssignment> assignments = buildPatternAssignments(pattern, mid);
            DispatchPlan plan = assignments == null ? null
                    : buildDispatchPlan(ownerReference, PatternTargetInformation.NO_PATTERN_SLOT, pattern, assignments);
            if (plan != null && plan.canDispatch()) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    DispatchPlan orderingPlan(PatternCraftingReference owner, int slot, ItemStack pattern) {
        List<PatternIngredientAssignment> assignments = buildPatternAssignments(pattern, 1);
        return assignments == null ? null : buildDispatchPlan(owner, slot, pattern, assignments);
    }

    private List<PatternIngredientAssignment> buildPatternAssignments(ItemStack pattern, int sets) {
        if (sets <= 0) {
            return null;
        }
        PatternRecipeSnapshot configuredPattern = module.getPatternRecipe(pattern);
        if (configuredPattern == null) {
            return null;
        }
        List<PatternIngredientAssignment> assignments = new ArrayList<>();
        for (int slot = 0; slot < configuredPattern.getIngredientSlotCount(); slot++) {
            IPatternStack stack = configuredPattern.getInput(slot);
            if (stack == null || stack.getAmount() <= 0) {
                continue;
            }
            long amount = (long) stack.getAmount() * sets;
            if (amount > Integer.MAX_VALUE) {
                return null;
            }
            assignments
                    .add(new PatternIngredientAssignment(slot, PatternStackHelper.copyWithAmount(stack, (int) amount)));
        }
        return assignments.isEmpty() ? null : assignments;
    }

    private DispatchPlan buildDispatchPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern,
            List<PatternIngredientAssignment> assignments) {
        if (pattern == null || assignments == null || assignments.isEmpty()) {
            return null;
        }
        DispatchPlan plan = new DispatchPlan(ownerReference, patternSlot, pattern, assignments);
        PatternRecipeSnapshot configuredPattern = module.getPatternRecipe(pattern);
        if (configuredPattern == null) {
            return null;
        }
        boolean useSatellites = module.hasAdvancedSatelliteUpgrade()
                && !adjacentInventory.isConnectedToPatternCraftingTable();
        for (PatternIngredientAssignment assignment : assignments) {
            IPatternStack configuredStack = configuredPattern.getInput(assignment.inputSlot());
            ItemIdentifierStack item = PatternStackHelper.asSolidStack(assignment.stack());
            if (item != null) {
                IRequestItems target = PatternStackHelper.isSolid(configuredStack)
                        ? module.getSatelliteTargetForInputSlot(configuredPattern.getPattern(), assignment.inputSlot())
                        : null;
                if (target instanceof PipeItemsPatternSatelliteLogistics satellite) {
                    plan.addItemSatellite(satellite, item.clone(), assignment.inputSlot());
                } else {
                    if (useSatellites && (configuredPattern.getItemSatelliteId(assignment.inputSlot()) > 0
                            || !configuredPattern.getItemSatelliteUuid(assignment.inputSlot()).isEmpty())) {
                        return null;
                    }
                    plan.addLocal(assignment);
                }
                continue;
            }
            FluidIdentifier fluid = PatternStackHelper.asFluid(assignment.stack());
            if (fluid != null) {
                IRequestFluid target = PatternStackHelper.isFluid(configuredStack) ? module
                        .getFluidSatelliteTargetForInputSlot(configuredPattern.getPattern(), assignment.inputSlot())
                        : null;
                if (target instanceof PipeFluidPatternSatelliteLogistics satellite) {
                    if (!plan.addFluidSatellite(satellite, fluid, assignment.stack().getAmount())) {
                        return null;
                    }
                } else {
                    if (useSatellites && (configuredPattern.getFluidSatelliteId(assignment.inputSlot()) > 0
                            || !configuredPattern.getFluidSatelliteUuid(assignment.inputSlot()).isEmpty())) {
                        return null;
                    }
                    plan.addLocal(assignment);
                }
            }
        }
        return plan;
    }

    DispatchPlan readFromNBT(NBTTagCompound tag) {
        PatternCraftingReference owner = PatternCraftingReference.readFromNBT(tag, "owner");
        PatternCraftingReference batch = PatternCraftingReference.readFromNBT(tag, "batch");
        ItemStack pattern = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("pattern"));
        if (owner == null || batch == null || pattern == null) {
            throw new PatternCraftingPersistence.RestoreNotReadyException();
        }
        DispatchPlan plan = new DispatchPlan(
                owner,
                batch,
                tag.getInteger("patternSlot"),
                pattern,
                readAssignments(tag.getTagList("assignments", 10)),
                ForgeDirection.getOrientation(tag.getInteger("localDirection")));
        plan.started = tag.getBoolean("started");
        plan.committing = tag.hasKey("committing") ? tag.getBoolean("committing") : plan.started;
        plan.usesLocalInventory = tag.getBoolean("usesLocalInventory");
        plan.localAssignments.addAll(readAssignments(tag.getTagList("localRemaining", 10)));
        NBTTagList items = tag.getTagList("items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound entry = items.getCompoundTagAt(i);
            ItemIdentifierStack stack = PatternItemStack.readItem(entry);
            if (stack == null || stack.getStackSize() <= 0)
                throw new PatternCraftingPersistence.RestoreNotReadyException();
            ItemSatelliteAssignment assignment = new ItemSatelliteAssignment(
                    entry.getString("satellite"),
                    stack,
                    entry.getInteger("inputSlot"));
            assignment.remaining = Math.max(0, Math.min(stack.getStackSize(), entry.getInteger("remaining")));
            assignment.routed = entry.getBoolean("routed");
            assignment.deliveryReference = PatternCraftingReference.readFromNBT(entry, "delivery");
            plan.itemSatelliteAssignments.add(assignment);
        }
        NBTTagList fluids = tag.getTagList("fluids", 10);
        for (int i = 0; i < fluids.tagCount(); i++) {
            NBTTagCompound entry = fluids.getCompoundTagAt(i);
            IPatternStack stack = IPatternStack.readFromNBT(entry);
            if (!(stack instanceof PatternFluidStack fluid) || fluid.getAmount() <= 0) {
                throw new PatternCraftingPersistence.RestoreNotReadyException();
            }
            FluidSatelliteAssignment assignment = new FluidSatelliteAssignment(
                    entry.getString("satellite"),
                    fluid.getFluid(),
                    fluid.getAmount());
            assignment.remaining = Math.max(0, Math.min(fluid.getAmount(), entry.getInteger("remaining")));
            plan.fluidSatelliteAssignments.add(assignment);
        }
        return plan;
    }

    private static NBTTagList writeAssignments(List<PatternIngredientAssignment> assignments) {
        NBTTagList list = new NBTTagList();
        for (PatternIngredientAssignment assignment : assignments) {
            NBTTagCompound tag = new NBTTagCompound();
            assignment.stack().writeToNBT(tag);
            tag.setInteger("inputSlot", assignment.inputSlot());
            list.appendTag(tag);
        }
        return list;
    }

    private static List<PatternIngredientAssignment> readAssignments(NBTTagList list) {
        List<PatternIngredientAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            IPatternStack stack = IPatternStack.readFromNBT(tag);
            if (stack == null || stack.getAmount() <= 0)
                throw new PatternCraftingPersistence.RestoreNotReadyException();
            assignments.add(new PatternIngredientAssignment(tag.getInteger("inputSlot"), stack));
        }
        return assignments;
    }

    private List<PipeItemsPatternSatelliteLogistics> uniqueItemSatellites(List<ItemSatelliteAssignment> assignments) {
        List<PipeItemsPatternSatelliteLogistics> result = new ArrayList<>();
        for (ItemSatelliteAssignment assignment : assignments) {
            if (!result.contains(assignment.satellite)) {
                result.add(assignment.satellite);
            }
        }
        return result;
    }

    private List<PipeFluidPatternSatelliteLogistics> uniqueFluidSatellites(List<FluidSatelliteAssignment> assignments) {
        List<PipeFluidPatternSatelliteLogistics> result = new ArrayList<>();
        for (FluidSatelliteAssignment assignment : assignments) {
            if (!result.contains(assignment.satellite)) {
                result.add(assignment.satellite);
            }
        }
        return result;
    }

    private static final class ItemSatelliteAssignment {

        private PipeItemsPatternSatelliteLogistics satellite;
        private final String satelliteUuid;
        private final ItemIdentifierStack stack;
        private final int inputSlot;
        private boolean routed;
        private PatternCraftingReference deliveryReference;
        /** What still has to go to the satellite; the rest already left the buffer. */
        private int remaining;

        private ItemSatelliteAssignment(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack,
                int inputSlot) {
            this(satellite.getSatelliteUuid(), stack, inputSlot);
            this.satellite = satellite;
        }

        private ItemSatelliteAssignment(String satelliteUuid, ItemIdentifierStack stack, int inputSlot) {
            this.satelliteUuid = satelliteUuid;
            this.stack = stack;
            this.inputSlot = inputSlot;
            this.remaining = stack.getStackSize();
        }
    }

    private static final class FluidSatelliteAssignment {

        private PipeFluidPatternSatelliteLogistics satellite;
        private final String satelliteUuid;
        private final FluidIdentifier fluid;
        private int amount;
        /** What still has to go to the satellite; the rest already left the buffer. */
        private int remaining;

        private FluidSatelliteAssignment(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid,
                int amount) {
            this(satellite.getSatelliteUuid(), fluid, amount);
            this.satellite = satellite;
        }

        private FluidSatelliteAssignment(String satelliteUuid, FluidIdentifier fluid, int amount) {
            this.satelliteUuid = satelliteUuid;
            this.fluid = fluid;
            this.amount = amount;
            this.remaining = amount;
        }
    }

    enum DispatchResult {
        /** Nothing went in; the buffer is unchanged. */
        NONE,
        /** Part of the set went in and left the buffer; the rest must follow before anything else. */
        PARTIAL,
        /** The whole set went in. */
        COMPLETE
    }

    /**
     * One set (or several) of buffered ingredients on its way into the target and its satellites.
     * <p>
     * Every amount that goes in leaves the buffer right away, per assignment, so a short insert can never leave items
     * both in the target and in the buffer (D1). What didn't fit stays buffered and is retried by the next
     * {@link #dispatch(PatternStackBufferHandler)}.
     */
    final class DispatchPlan {

        private final int patternSlot;
        private final PatternCraftingReference ownerReference;
        private final PatternCraftingReference batchReference;
        private final ItemStack pattern;
        private final List<PatternIngredientAssignment> assignments;
        private final List<PatternIngredientAssignment> localAssignments = new ArrayList<>();
        private final List<ItemSatelliteAssignment> itemSatelliteAssignments = new ArrayList<>();
        private final List<FluidSatelliteAssignment> fluidSatelliteAssignments = new ArrayList<>();
        private final ForgeDirection localDirection;
        private boolean usesLocalInventory;
        private boolean started;
        private boolean committing;

        private DispatchPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern,
                List<PatternIngredientAssignment> assignments) {
            this(
                    ownerReference,
                    ownerReference == null ? null : ownerReference.createChild(),
                    patternSlot,
                    pattern,
                    assignments,
                    adjacentInventory.getConnected() == null ? ForgeDirection.UNKNOWN
                            : adjacentInventory.getConnected().orientation);
        }

        private DispatchPlan(PatternCraftingReference ownerReference, PatternCraftingReference batchReference,
                int patternSlot, ItemStack pattern, List<PatternIngredientAssignment> assignments,
                ForgeDirection localDirection) {
            this.ownerReference = ownerReference;
            this.batchReference = batchReference;
            this.patternSlot = patternSlot;
            this.pattern = pattern.copy();
            this.assignments = new ArrayList<>(assignments);
            this.localDirection = localDirection;
        }

        List<PatternIngredientAssignment> assignments() {
            return assignments;
        }

        int patternSlot() {
            return patternSlot;
        }

        PatternCraftingReference ownerReference() {
            return ownerReference;
        }

        PatternCraftingReference batchReference() {
            return batchReference;
        }

        ItemStack pattern() {
            return pattern;
        }

        logisticspipes.utils.AdjacentTile localTarget() {
            if (pipe.container == null || localDirection == ForgeDirection.UNKNOWN) return null;
            var tile = pipe.container.getTile(localDirection);
            return tile == null || tile.isInvalid() ? null
                    : new logisticspipes.utils.AdjacentTile(tile, localDirection);
        }

        private List<Object> targetInventories() {
            List<Object> targets = new ArrayList<>();
            if (!resolveTargets()) return targets;
            if (usesLocalInventory && localTarget() != null) targets.add(localTarget().tile);
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                var target = assignment.satellite.getPatternTargetInventory();
                if (target != null) targets.add(target.tile);
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments)
                targets.addAll(assignment.satellite.patternTargetTanks());
            PatternRecipeSnapshot recipe = module.getPatternRecipe(pattern);
            for (int slot = 0; slot < recipe.getResultSlotCount(); slot++) {
                IPatternStack output = recipe.getOutput(slot);
                if (output == null) continue;
                boolean fluid = PatternStackHelper.isFluid(output);
                int id = fluid ? recipe.getFluidByproductSatelliteId(slot) : recipe.getByproductSatelliteId(slot);
                String uuid = fluid ? recipe.getFluidByproductSatelliteUuid(slot)
                        : recipe.getByproductSatelliteUuid(slot);
                if (id <= 0 && uuid.isEmpty()) {
                    if (localTarget() != null) targets.add(localTarget().tile);
                } else if (fluid) {
                    PipeFluidPatternSatelliteLogistics satellite = uuid.isEmpty()
                            ? PipeFluidPatternSatelliteLogistics.findById(id, pipe.getRouter())
                            : PipeFluidPatternSatelliteLogistics.findByUuid(uuid);
                    if (satellite != null) targets.addAll(satellite.patternTargetTanks());
                } else {
                    PipeItemsPatternSatelliteLogistics satellite = uuid.isEmpty()
                            ? PipeItemsPatternSatelliteLogistics.findById(id, pipe.getRouter())
                            : PipeItemsPatternSatelliteLogistics.findByUuid(uuid);
                    if (satellite != null && satellite.getPatternTargetInventory() != null)
                        targets.add(satellite.getPatternTargetInventory().tile);
                }
            }
            return targets;
        }

        boolean sameRecipe(DispatchPlan other) {
            if (pattern.getItem() != other.pattern.getItem() || pattern.getItemDamage() != other.pattern.getItemDamage()
                    || assignments.size() != other.assignments.size())
                return false;
            PatternRecipeSnapshot left = module.getPatternRecipe(pattern);
            PatternRecipeSnapshot right = module.getPatternRecipe(other.pattern);
            if (left.getIngredientSlotCount() != right.getIngredientSlotCount()
                    || left.isIgnoreNbtEnabled() != right.isIgnoreNbtEnabled()
                    || left.isOreDictSubstitutionEnabled() != right.isOreDictSubstitutionEnabled())
                return false;
            for (int i = 0; i < left.getIngredientSlotCount(); i++) {
                IPatternStack a = left.getInput(i);
                IPatternStack b = right.getInput(i);
                if (a == null || b == null) {
                    if (a != b) return false;
                } else if (!a.canMerge(b) || a.getAmount() != b.getAmount()) return false;
            }
            if (left.getResultSlotCount() != right.getResultSlotCount()) return false;
            for (int i = 0; i < left.getResultSlotCount(); i++) {
                IPatternStack a = left.getOutput(i);
                IPatternStack b = right.getOutput(i);
                if (a == null || b == null) {
                    if (a != b) return false;
                } else if (!a.canMerge(b) || a.getAmount() != b.getAmount()) return false;
            }
            return true;
        }

        boolean sharesTargets(DispatchPlan other) {
            List<Object> targets = targetInventories();
            for (Object target : other.targetInventories()) if (targets.contains(target)) return true;
            return false;
        }

        private void addLocal(PatternIngredientAssignment assignment) {
            usesLocalInventory = true;
            localAssignments.add(assignment);
        }

        int sets() {
            return insertedSetsFromPlan(pattern, assignments);
        }

        /** Reconnects saved target identities as their chunks become available. */
        boolean resolveTargets() {
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                assignment.satellite = PipeItemsPatternSatelliteLogistics.findByUuid(assignment.satelliteUuid);
                if (assignment.satellite == null || !assignment.satellite.canExtractByproductsFor(pipe.getRouter())) {
                    return false;
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                assignment.satellite = PipeFluidPatternSatelliteLogistics.findByUuid(assignment.satelliteUuid);
                if (assignment.satellite == null || !assignment.satellite.canExtractByproductsFor(pipe.getRouter())) {
                    return false;
                }
            }
            return true;
        }

        NBTTagCompound writeToNBT() {
            NBTTagCompound tag = new NBTTagCompound();
            ownerReference.writeToNBT(tag, "owner");
            batchReference.writeToNBT(tag, "batch");
            tag.setInteger("patternSlot", patternSlot);
            NBTTagCompound recipe = new NBTTagCompound();
            pattern.writeToNBT(recipe);
            tag.setTag("pattern", recipe);
            tag.setBoolean("started", started);
            tag.setBoolean("committing", committing);
            tag.setBoolean("usesLocalInventory", usesLocalInventory);
            tag.setInteger("localDirection", localDirection.ordinal());
            tag.setTag("assignments", writeAssignments(assignments));
            tag.setTag("localRemaining", writeAssignments(localAssignments));
            NBTTagList items = new NBTTagList();
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                NBTTagCompound entry = new NBTTagCompound();
                PatternItemStack.writeItem(entry, assignment.stack);
                entry.setString("satellite", assignment.satelliteUuid);
                entry.setInteger("inputSlot", assignment.inputSlot);
                entry.setInteger("remaining", assignment.remaining);
                entry.setBoolean("routed", assignment.routed);
                if (assignment.deliveryReference != null) assignment.deliveryReference.writeToNBT(entry, "delivery");
                items.appendTag(entry);
            }
            tag.setTag("items", items);
            NBTTagList fluids = new NBTTagList();
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                NBTTagCompound entry = new NBTTagCompound();
                new PatternFluidStack(assignment.fluid, assignment.amount).writeToNBT(entry);
                entry.setString("satellite", assignment.satelliteUuid);
                entry.setInteger("remaining", assignment.remaining);
                fluids.appendTag(entry);
            }
            tag.setTag("fluids", fluids);
            return tag;
        }

        private void addItemSatellite(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack,
                int inputSlot) {
            itemSatelliteAssignments.add(new ItemSatelliteAssignment(satellite, stack, inputSlot));
        }

        /**
         * Amounts of the same fluid for the same satellite are merged, so the room check covers all of them together.
         */
        private boolean addFluidSatellite(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid,
                int amount) {
            for (FluidSatelliteAssignment existing : fluidSatelliteAssignments) {
                if (existing.satellite == satellite && existing.fluid.equals(fluid)) {
                    if ((long) existing.amount + amount > Integer.MAX_VALUE) {
                        return false;
                    }
                    existing.amount += amount;
                    existing.remaining += amount;
                    return true;
                }
            }
            fluidSatelliteAssignments.add(new FluidSatelliteAssignment(satellite, fluid, amount));
            return true;
        }

        boolean canDispatch() {
            if (!resolveTargets()) return false;
            if (!module.batchOutputs().canUseTargets(this)) return false;
            if (hasSatellites() && !pipe.hasAdvancedSatelliteUpgrade()) {
                return false;
            }
            if (!localAssignments.isEmpty()
                    && !adjacentInventory.canInsertPatternIngredients(pattern, localAssignments)) {
                return false;
            }
            boolean instantItems = module.hasInstantSatelliteUpgrade();
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                if (!assignment.satellite.canReserveFor(pipe, batchReference)

                        || (!instantItems && !canRouteToItemSatellite(assignment))) {
                    return false;
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                if (!assignment.satellite.canReserveFor(pipe, batchReference)) {
                    return false;
                }
            }
            for (PipeItemsPatternSatelliteLogistics satellite : uniqueItemSatellites(itemSatelliteAssignments)) {
                List<ItemIdentifierStack> stacks = new ArrayList<>();
                for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                    if (assignment.satellite == satellite) {
                        stacks.add(assignment.stack);
                    }
                }
                if (!satellite.canAcceptPatternInputs(stacks)) {
                    return false;
                }
            }
            for (PipeFluidPatternSatelliteLogistics satellite : uniqueFluidSatellites(fluidSatelliteAssignments)) {
                List<PatternFluidStack> fluids = new ArrayList<>();
                for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                    if (assignment.satellite == satellite) {
                        fluids.add(new PatternFluidStack(assignment.fluid, assignment.amount));
                    }
                }
                if (!satellite.canAcceptPatternInputs(fluids)) {
                    return false;
                }
            }
            return canFitSharedItemInputs() && canFitSharedFluidInputs();
        }

        private boolean canFitSharedFluidInputs() {
            List<List<logisticspipes.utils.tuples.Pair<net.minecraftforge.fluids.IFluidHandler, ForgeDirection>>> targets = new ArrayList<>();
            List<PatternFluidStack> fluids = new ArrayList<>();
            var connected = adjacentInventory.getConnected();
            if (connected != null && connected.tile instanceof net.minecraftforge.fluids.IFluidHandler handler) {
                for (PatternIngredientAssignment assignment : localAssignments) {
                    if (!(assignment.stack() instanceof PatternFluidStack fluid)) continue;
                    targets.add(
                            java.util.Collections.singletonList(
                                    new logisticspipes.utils.tuples.Pair<>(
                                            handler,
                                            adjacentInventory.getFluidInsertionOrientation(connected))));
                    fluids.add(fluid);
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                targets.add(assignment.satellite.patternInputTanks());
                fluids.add(new PatternFluidStack(assignment.fluid, assignment.amount));
            }
            java.util.Set<net.minecraftforge.fluids.IFluidHandler> seen = java.util.Collections
                    .newSetFromMap(new java.util.IdentityHashMap<>());
            for (var group : targets) {
                for (var target : group) if (!seen.add(target.getValue1()))
                    return AdjacentInventoryHandler.canFitFluidInputs(targets, fluids);
            }
            return true;
        }

        /** Two satellite connections to one inventory must not each simulate spending the same empty slots. */
        private boolean canFitSharedItemInputs() {
            java.util.Map<Object, net.minecraft.inventory.IInventory> inventories = new java.util.IdentityHashMap<>();
            java.util.Map<Object, List<ItemIdentifierStack>> inputs = new java.util.IdentityHashMap<>();
            var connected = adjacentInventory.getConnected();
            if (connected != null && !localAssignments.isEmpty()
                    && !adjacentInventory.isConnectedToPatternCraftingTable()) {
                for (PatternIngredientAssignment assignment : localAssignments) {
                    ItemIdentifierStack stack = PatternStackHelper.asSolidStack(assignment.stack());
                    if (stack == null) continue;
                    inventories.put(connected.tile, adjacentInventory.getInsertableInventory(connected));
                    inputs.computeIfAbsent(connected.tile, ignored -> new ArrayList<>()).add(stack);
                }
            }
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                var target = assignment.satellite.getPatternTargetInventory();
                if (target == null) return false;
                inventories.put(target.tile, assignment.satellite.getInsertableInventory(target));
                inputs.computeIfAbsent(target.tile, ignored -> new ArrayList<>()).add(assignment.stack);
            }
            for (Object target : inputs.keySet()) if (!AdjacentInventoryHandler
                    .canFitPatternSetsDisregardingSlots(inventories.get(target), inputs.get(target), 1))
                return false;
            return true;
        }

        /**
         * Stage the complete plan, check every target again, and commit without yielding the server thread. Unexpected
         * short inserts retain recovery progress; partial sets are never deliberately dispatched.
         */
        DispatchResult dispatch(PatternStackBufferHandler buffer) {
            if (!resolveTargets() || (!localAssignments.isEmpty() && (adjacentInventory.getConnected() == null
                    || adjacentInventory.getConnected().orientation != localDirection))) {
                return DispatchResult.NONE;
            }
            if (!started && !canDispatch()) {
                return DispatchResult.NONE;
            }
            if (!started) {
                List<PipeItemsPatternSatelliteLogistics> reservedItemSatellites = new ArrayList<>();
                List<PipeFluidPatternSatelliteLogistics> reservedFluidSatellites = new ArrayList<>();
                if (!reserveSatellites(reservedItemSatellites, reservedFluidSatellites)) {
                    releaseSatellites(reservedItemSatellites, reservedFluidSatellites);
                    return DispatchResult.NONE;
                }
            }
            boolean instantItems = module.hasInstantSatelliteUpgrade();
            if (!committing) {
                for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                    if (assignment.routed || instantItems) continue;
                    routeItemSatelliteAssignment(assignment);
                    assignment.routed = true;
                    buffer.remove(ownerReference, new PatternItemStack(assignment.stack), assignment.remaining);
                    started = true;
                }
                for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                    if (assignment.routed && assignment.satellite.stagedPatternInputAmount(assignment.deliveryReference)
                            < assignment.remaining)
                        return DispatchResult.PARTIAL;
                }
                // Routing may have taken many ticks. Recheck every target before inserting any ingredients.
                if (!canDispatch()) return started ? DispatchResult.PARTIAL : DispatchResult.NONE;
                committing = true;
            }
            insertLocal(buffer);
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                if (assignment.remaining <= 0) {
                    continue;
                }
                int inserted = assignment.satellite.insertPatternInput(assignment.fluid, assignment.remaining);
                if (inserted > 0) {
                    assignment.remaining -= inserted;
                    buffer.remove(ownerReference, new PatternFluidStack(assignment.fluid, inserted), inserted);
                    started = true;
                }
            }
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                if (assignment.remaining <= 0) {
                    continue;
                }
                int inserted;
                if (assignment.routed) {
                    inserted = assignment.satellite
                            .insertStagedPatternInput(assignment.deliveryReference, assignment.remaining);
                } else {
                    inserted = assignment.satellite.insertPatternInput(
                            new ItemIdentifierStack(assignment.stack.getItem(), assignment.remaining));
                }
                if (inserted > 0) {
                    assignment.remaining -= inserted;
                    if (!assignment.routed) buffer.remove(
                            ownerReference,
                            new PatternItemStack(new ItemIdentifierStack(assignment.stack.getItem(), inserted)),
                            inserted);
                    started = true;
                }
            }
            if (isComplete()) {
                return DispatchResult.COMPLETE;
            }
            if (!started) {
                releaseSatellites(
                        uniqueItemSatellites(itemSatelliteAssignments),
                        uniqueFluidSatellites(fluidSatelliteAssignments));
                return DispatchResult.NONE;
            }
            return DispatchResult.PARTIAL;
        }

        private void insertLocal(PatternStackBufferHandler buffer) {
            if (localAssignments.isEmpty()) {
                return;
            }
            int[] inserted = adjacentInventory.insertPatternIngredients(pattern, localAssignments);
            List<PatternIngredientAssignment> missing = new ArrayList<>();
            for (int i = 0; i < localAssignments.size(); i++) {
                PatternIngredientAssignment assignment = localAssignments.get(i);
                int amount = Math.max(0, Math.min(inserted[i], assignment.stack().getAmount()));
                if (amount > 0) {
                    buffer.remove(ownerReference, assignment.stack(), amount);
                    started = true;
                }
                if (amount < assignment.stack().getAmount()) {
                    IPatternStack rest = assignment.stack().copy();
                    rest.addAmount(-amount);
                    missing.add(new PatternIngredientAssignment(assignment.inputSlot(), rest));
                }
            }
            localAssignments.clear();
            localAssignments.addAll(missing);
        }

        private boolean isComplete() {
            if (!localAssignments.isEmpty()) {
                return false;
            }
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                if (assignment.remaining > 0) {
                    return false;
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                if (assignment.remaining > 0) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Gives up on the rest of the set: pulls what already reached the satellites back to storage and releases them.
         * The parts still missing are in the buffer, which the caller flushes; items already in the local target stay
         * there, like any dispatched set.
         */
        boolean abandon() {
            if (!resolveTargets()) return false;
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                int delivered = assignment.routed ? assignment.stack.getStackSize()
                        : assignment.stack.getStackSize() - assignment.remaining;
                if (delivered > 0) {
                    assignment.satellite.retrieveOrCancelToStorage(
                            new ItemIdentifierStack(assignment.stack.getItem(), delivered),
                            assignment.routed,
                            assignment.deliveryReference);
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                int delivered = assignment.amount - assignment.remaining;
                if (delivered > 0) {
                    assignment.satellite.retrieveFluidToStorage(assignment.fluid, delivered);
                }
            }
            releaseSatellites(
                    uniqueItemSatellites(itemSatelliteAssignments),
                    uniqueFluidSatellites(fluidSatelliteAssignments));
            return true;
        }

        boolean release() {
            if (!resolveTargets()) return false;
            releaseSatellites(
                    uniqueItemSatellites(itemSatelliteAssignments),
                    uniqueFluidSatellites(fluidSatelliteAssignments));
            return true;
        }

        private boolean hasSatellites() {
            return !itemSatelliteAssignments.isEmpty() || !fluidSatelliteAssignments.isEmpty();
        }

        private boolean canRouteToItemSatellite(ItemSatelliteAssignment assignment) {
            return batchReference != null && assignment.satellite.getRouter() != null
                    && pipe.getRouter() != null
                    && pipe.getRouter()
                            .hasRoute(assignment.satellite.getRouter().getSimpleID(), true, assignment.stack.getItem());
        }

        private void routeItemSatelliteAssignment(ItemSatelliteAssignment assignment) {
            PatternTargetInformation target = PatternTargetInformation
                    .delivery(patternSlot, assignment.inputSlot, batchReference);
            assignment.deliveryReference = target.deliveryReference();
            assignment.satellite.expectStagedPatternInput(assignment.stack, assignment.deliveryReference);
            int remaining = assignment.stack.getStackSize();
            int maxStackSize = Math.max(1, assignment.stack.getItem().getMaxStackSize());
            while (remaining > 0) {
                int sent = Math.min(remaining, maxStackSize);
                pipe.sendStack(
                        new ItemIdentifierStack(assignment.stack.getItem(), sent).makeNormalStack(),
                        assignment.satellite.getRouter().getSimpleID(),
                        CoreRoutedPipe.ItemSendMode.Normal,
                        target);
                remaining -= sent;
            }
        }

        private boolean reserveSatellites(List<PipeItemsPatternSatelliteLogistics> itemSatellites,
                List<PipeFluidPatternSatelliteLogistics> fluidSatellites) {
            for (PipeItemsPatternSatelliteLogistics satellite : uniqueItemSatellites(itemSatelliteAssignments)) {
                if (!satellite.reserveFor(pipe, batchReference)) {
                    return false;
                }
                itemSatellites.add(satellite);
            }
            for (PipeFluidPatternSatelliteLogistics satellite : uniqueFluidSatellites(fluidSatelliteAssignments)) {
                if (!satellite.reserveFor(pipe, batchReference)) {
                    return false;
                }
                fluidSatellites.add(satellite);
            }
            return true;
        }

        private void releaseSatellites(List<PipeItemsPatternSatelliteLogistics> itemSatellites,
                List<PipeFluidPatternSatelliteLogistics> fluidSatellites) {
            for (PipeItemsPatternSatelliteLogistics satellite : itemSatellites) {
                satellite.releaseReservation(pipe, batchReference);
            }
            for (PipeFluidPatternSatelliteLogistics satellite : fluidSatellites) {
                satellite.releaseReservation(pipe, batchReference);
            }
        }
    }

}
