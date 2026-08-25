package logisticspipes.crafting;

import logisticspipes.crafting.pattern.PatternRecipeSnapshot;
import logisticspipes.crafting.patternStack.IPatternStack;
import logisticspipes.crafting.patternStack.PatternStackHelper;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

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
    DispatchPlan findInsertableBufferedPlan(PatternCraftingReference ownerReference, int patternSlot,
                                            ItemStack pattern, int maxSets) {
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
            int mid = low + (high - low + 1) / 2;
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
                    plan.addFluidSatellite(satellite, fluid, assignment.stack().getAmount());
                } else {
                    plan.addLocal(assignment);
                }
            }
        }
        return plan;
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

        private final PipeItemsPatternSatelliteLogistics satellite;
        private final ItemIdentifierStack stack;
        private final int inputSlot;
        private boolean routed;
        private PatternCraftingReference deliveryReference;

        private ItemSatelliteAssignment(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack,
                                        int inputSlot) {
            this.satellite = satellite;
            this.stack = stack;
            this.inputSlot = inputSlot;
        }
    }

    private static final class FluidSatelliteAssignment {

        private final PipeFluidPatternSatelliteLogistics satellite;
        private final FluidIdentifier fluid;
        private final int amount;

        private FluidSatelliteAssignment(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid,
                                         int amount) {
            this.satellite = satellite;
            this.fluid = fluid;
            this.amount = amount;
        }
    }

    final class DispatchPlan {

        private final int patternSlot;
        private final PatternCraftingReference batchReference;
        private final ItemStack pattern;
        private final List<PatternIngredientAssignment> assignments;
        private final List<PatternIngredientAssignment> localAssignments = new ArrayList<>();
        private final List<ItemSatelliteAssignment> itemSatelliteAssignments = new ArrayList<>();
        private final List<FluidSatelliteAssignment> fluidSatelliteAssignments = new ArrayList<>();

        private DispatchPlan(PatternCraftingReference ownerReference, int patternSlot, ItemStack pattern,
                             List<PatternIngredientAssignment> assignments) {
            this.batchReference = ownerReference == null ? null : ownerReference.createChild();
            this.patternSlot = patternSlot;
            this.pattern = pattern;
            this.assignments = new ArrayList<>(assignments);
        }

        List<PatternIngredientAssignment> assignments() {
            return assignments;
        }

        private void addLocal(PatternIngredientAssignment assignment) {
            localAssignments.add(assignment);
        }

        private void addItemSatellite(PipeItemsPatternSatelliteLogistics satellite, ItemIdentifierStack stack,
                                      int inputSlot) {
            itemSatelliteAssignments.add(new ItemSatelliteAssignment(satellite, stack, inputSlot));
        }

        private void addFluidSatellite(PipeFluidPatternSatelliteLogistics satellite, FluidIdentifier fluid,
                                       int amount) {
            fluidSatelliteAssignments.add(new FluidSatelliteAssignment(satellite, fluid, amount));
        }

        boolean canDispatch() {
            if (hasSatellites() && !pipe.hasAdvancedSatelliteUpgrade()) {
                return false;
            }
            if (!localAssignments.isEmpty()
                && !adjacentInventory.canInsertPatternIngredients(pattern, localAssignments)) {
                return false;
            }
            boolean instantItems = module.hasInstantSatelliteUpgrade();
            boolean reserveSatellites = usesSatelliteReservations();
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                if ((reserveSatellites && !assignment.satellite.canReserveFor(pipe, batchReference))
                    || (reserveSatellites && !assignment.satellite.isPatternTargetEmpty())
                    || !assignment.satellite.canAcceptPatternInput(assignment.stack)
                    || (!instantItems && !canRouteToItemSatellite(assignment))) {
                    return false;
                }
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                if ((reserveSatellites && !assignment.satellite.canReserveFor(pipe, batchReference))
                    || (reserveSatellites && !assignment.satellite.isPatternTargetEmpty())
                    || !assignment.satellite.canAcceptPatternInput(assignment.fluid, assignment.amount)) {
                    return false;
                }
            }
            return true;
        }

        boolean dispatch() {
            if (!canDispatch()) {
                return false;
            }
            List<PipeItemsPatternSatelliteLogistics> reservedItemSatellites = new ArrayList<>();
            List<PipeFluidPatternSatelliteLogistics> reservedFluidSatellites = new ArrayList<>();
            boolean reserveSatellites = usesSatelliteReservations();
            if (reserveSatellites && !reserveSatellites(reservedItemSatellites, reservedFluidSatellites)) {
                releaseSatellites(reservedItemSatellites, reservedFluidSatellites);
                return false;
            }
            if (!localAssignments.isEmpty() && !adjacentInventory.insertPatternIngredients(pattern, localAssignments)) {
                releaseSatellites(reservedItemSatellites, reservedFluidSatellites);
                return false;
            }
            for (FluidSatelliteAssignment assignment : fluidSatelliteAssignments) {
                int inserted = assignment.satellite
                    .insertPatternInput(assignment.fluid, assignment.amount, reserveSatellites);
                if (inserted != assignment.amount) {
                    releaseSatellites(reservedItemSatellites, reservedFluidSatellites);
                    return false;
                }
            }
            boolean instantItems = module.hasInstantSatelliteUpgrade();
            for (ItemSatelliteAssignment assignment : itemSatelliteAssignments) {
                if (instantItems) {
                    int inserted = assignment.satellite.insertPatternInput(assignment.stack, reserveSatellites);
                    if (inserted != assignment.stack.getStackSize()) {
                        releaseSatellites(reservedItemSatellites, reservedFluidSatellites);
                        return false;
                    }
                } else {
                    routeItemSatelliteAssignment(assignment, reserveSatellites);
                    assignment.routed = true;
                }
            }
            return true;
        }

        PatternCraftingBlockingHandler.SatelliteBatch satelliteBatch() {
            if (!hasSatellites()) {
                return null;
            }
            return new SatelliteDispatchBatch(
                batchReference,
                patternSlot,
                new ArrayList<>(itemSatelliteAssignments),
                new ArrayList<>(fluidSatelliteAssignments));
        }

        private boolean hasSatellites() {
            return !itemSatelliteAssignments.isEmpty() || !fluidSatelliteAssignments.isEmpty();
        }

        private boolean usesSatelliteReservations() {
            return module.getEffectiveBlockingMode() != PipeItemsPatternCraftingLogistics.BlockingMode.OFF;
        }

        private boolean canRouteToItemSatellite(ItemSatelliteAssignment assignment) {
            return batchReference != null && assignment.satellite.getRouter() != null && pipe.getRouter() != null
                && pipe.getRouter()
                .hasRoute(assignment.satellite.getRouter().getSimpleID(), true, assignment.stack.getItem());
        }

        private void routeItemSatelliteAssignment(ItemSatelliteAssignment assignment, boolean reserveSatellites) {
            if (reserveSatellites) {
                assignment.satellite.expectPatternInput(assignment.stack);
            }
            PatternTargetInformation target = PatternTargetInformation.delivery(
                patternSlot,
                assignment.inputSlot,
                batchReference);
            assignment.deliveryReference = target.deliveryReference();
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

    private final class SatelliteDispatchBatch implements PatternCraftingBlockingHandler.SatelliteBatch {

        private final int patternSlot;
        private final PatternCraftingReference reference;
        private final List<ItemSatelliteAssignment> itemAssignments;
        private final List<FluidSatelliteAssignment> fluidAssignments;

        private SatelliteDispatchBatch(PatternCraftingReference batchReference, int patternSlot,
                                       List<ItemSatelliteAssignment> itemAssignments,
                                       List<FluidSatelliteAssignment> fluidAssignments) {
            this.reference = batchReference;
            this.patternSlot = patternSlot;
            this.itemAssignments = itemAssignments;
            this.fluidAssignments = fluidAssignments;
        }

        @Override
        public PatternCraftingReference ownerReference() {
            return reference;
        }

        @Override
        public int patternSlot() {
            return patternSlot;
        }

        @Override
        public boolean isConsumed() {
            for (PipeItemsPatternSatelliteLogistics satellite : uniqueItemSatellites(itemAssignments)) {
                if (!satellite.isReservationConsumed(pipe, reference)) {
                    return false;
                }
            }
            for (PipeFluidPatternSatelliteLogistics satellite : uniqueFluidSatellites(fluidAssignments)) {
                if (!satellite.isReservationConsumed(pipe, reference)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int size() {
            return uniqueItemSatellites(itemAssignments).size() + uniqueFluidSatellites(fluidAssignments).size();
        }

        @Override
        public void retrieveAndRelease() {
            for (ItemSatelliteAssignment assignment : itemAssignments) {
                assignment.satellite.retrieveOrCancelToStorage(
                    assignment.stack.clone(),
                    assignment.routed,
                    assignment.deliveryReference);
            }
            for (FluidSatelliteAssignment assignment : fluidAssignments) {
                assignment.satellite.retrieveFluidToStorage(assignment.fluid, assignment.amount);
            }
            release();
        }

        @Override
        public void release() {
            for (PipeItemsPatternSatelliteLogistics satellite : uniqueItemSatellites(itemAssignments)) {
                satellite.releaseReservation(pipe, reference);
            }
            for (PipeFluidPatternSatelliteLogistics satellite : uniqueFluidSatellites(fluidAssignments)) {
                satellite.releaseReservation(pipe, reference);
            }
        }
    }
}
