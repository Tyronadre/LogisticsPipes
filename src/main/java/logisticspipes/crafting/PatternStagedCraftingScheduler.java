package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.utils.CacheHolder.CacheTypes;

/**
 * Decides when staged pattern crafting orders may request their next ingredient sets.
 * <p>
 * The scheduler balances three constraints before it asks a {@link PatternCraftingOrder} to consume branch state:
 * remaining output work, branch ingredient availability, and local capacity in the pipe buffer or adjacent target.
 */
class PatternStagedCraftingScheduler {

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private final List<PatternCraftingOrder> stagedCrafts;
    private final Set<Integer> requestingPatterns = new HashSet<>();
    private long lastPeriodicRequestTick = Long.MIN_VALUE;

    PatternStagedCraftingScheduler(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            List<PatternCraftingOrder> stagedCrafts) {
        this.module = module;
        this.pipe = pipe;
        this.stagedCrafts = stagedCrafts;
    }

    /**
     * Requests ingredients for every staged order that still has room in the module or adjacent inventory.
     */
    void requestIngredients(boolean capacityChanged) {
        long tick = module.currentWorldTick();
        if (!capacityChanged && lastPeriodicRequestTick == tick) {
            return;
        }
        lastPeriodicRequestTick = tick;
        Set<Integer> patternSlots = new LinkedHashSet<>();
        for (PatternCraftingOrder order : new ArrayList<>(stagedCrafts)) {
            patternSlots.add(order.patternSlot);
        }
        for (int patternSlot : patternSlots) {
            requestIngredients(patternSlot);
        }
    }

    /**
     * Requests ingredients for one pattern slot.
     * <p>
     * The per-pattern guard allows different patterns in the same module to stage work independently while preventing
     * recursive requests for the same pattern from re-entering through branch fulfillment.
     */
    void requestIngredients(int patternSlot) {
        if (!requestingPatterns.add(patternSlot)) {
            module.debugEventThrottled("SCHED", "request ingredients slot=%d skipped: already requesting", patternSlot);
            return;
        }
        try {
            requestIngredientsGuarded(patternSlot);
        } finally {
            requestingPatterns.remove(patternSlot);
        }
    }

    private void requestIngredientsGuarded(int patternSlot) {
        for (PatternCraftingOrder order : new ArrayList<>(stagedCrafts)) {
            if (removeFinishedOrder(order)) {
                continue;
            }
            if (order.patternSlot != patternSlot) {
                continue;
            }

            ItemStack pattern = module.getPatternStack(order.patternSlot);
            if (removeOrderWithoutPattern(order, pattern) || removeFullyRequestedOrder(order)) {
                continue;
            }
            requestOrderIngredients(order, pattern);
        }
    }

    private boolean removeFinishedOrder(PatternCraftingOrder order) {
        if (!order.outputOrder.isFinished()) {
            return false;
        }
        if (!order.isFullyRequested()) {
            module.debugEventThrottled(
                    "SCHED",
                    60,
                    "request ingredients slot=%d kept satisfied output until its planned ingredients are requested remainingSets=%d",
                    order.patternSlot,
                    order.remainingSets);
            return false;
        }
        module.debugEvent(
                "SCHED",
                "request ingredients slot=%d removing staged order: the order output is already satisfied remainingSets=%d",
                order.patternSlot,
                order.remainingSets);
        order.releaseReservations();
        stagedCrafts.remove(order);
        module.markCraftingStateDirty();
        return true;
    }

    private boolean removeOrderWithoutPattern(PatternCraftingOrder order, ItemStack pattern) {
        if (pattern != null) {
            return false;
        }
        module.debugEvent(
                "SCHED",
                "request ingredients slot=%d removing staged order: pattern missing",
                order.patternSlot);
        order.releaseReservations();
        stagedCrafts.remove(order);
        module.markCraftingStateDirty();
        return true;
    }

    private boolean removeFullyRequestedOrder(PatternCraftingOrder order) {
        if (!order.isFullyRequested()) {
            return false;
        }
        module.debugEvent(
                "SCHED",
                "request ingredients slot=%d removing staged order: fully requested remainingSets=%d",
                order.patternSlot,
                order.remainingSets);
        order.releaseReservations();
        stagedCrafts.remove(order);
        module.markCraftingStateDirty();
        return true;
    }

    private void requestOrderIngredients(PatternCraftingOrder order, ItemStack pattern) {
        if (!module.workspace().admit(order)) return;
        int branchSets = order.availableSetsFromBranches(pattern);
        int orderableSets = orderableSetsForPattern(order, pattern, branchSets);
        int sets = Math.min(order.remainingSets, orderableSets);
        sets = Math.min(sets, branchSets);
        if (sets <= 0) {
            module.debugEventThrottled(
                    "SCHED",
                    100,
                    "request ingredients slot=%d paused: no selectable sets remainingSets=%d orderableSets=%d branchSets=%d",
                    order.patternSlot,
                    order.remainingSets,
                    orderableSets,
                    branchSets);
            return;
        }
        module.debugEvent(
                "SCHED",
                "request ingredients slot=%d remainingSets=%d orderableSets=%d branchSets=%d selectedSets=%d",
                order.patternSlot,
                order.remainingSets,
                orderableSets,
                branchSets,
                sets);

        int requestedSets = order.requestIngredients(pattern, sets);
        if (requestedSets <= 0) {
            module.debugEventThrottled(
                    "SCHED",
                    "request ingredients slot=%d requested no sets selectedSets=%d",
                    order.patternSlot,
                    sets);
            return;
        }

        module.debugEvent(
                "SCHED",
                "request ingredients slot=%d requestedSets=%d remainingSets=%d",
                order.patternSlot,
                requestedSets,
                order.remainingSets);
        pipe.getCacheHolder().trigger(CacheTypes.Inventory);
        module.markCraftingStateDirty();
        if (order.isFullyRequested()) {
            module.debugEvent(
                    "REQUEST",
                    "request ingredients slot=%d completed staged order after request",
                    order.patternSlot);
            order.releaseReservations();
            stagedCrafts.remove(order);
            module.markCraftingStateDirty();
        }
    }

    /**
     * Calculates how many complete pattern sets can be ordered now without overcommitting the module buffer or adjacent
     * inventory. Requested but not-yet-arrived ingredients are subtracted so repeated recalculation only orders newly
     * freed capacity.
     */
    private int orderableSetsForPattern(PatternCraftingOrder order, ItemStack pattern, int branchSets) {
        if (!module.isPatternCraftingSupported(pattern)) {
            module.debugEventThrottled("SCHED", "orderable sets slot=%d result=0 cannot receive", order.patternSlot);
            return 0;
        }
        // Admission reserved this order and its entire subtree together. Machine mode only gates insertion;
        // gating dependency creation on a busy machine can prevent the orders needed to drain its output.
        return Math.min(order.remainingSets, branchSets);
    }
}
