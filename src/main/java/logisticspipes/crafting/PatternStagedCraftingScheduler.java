package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;

/** Expands only the next ingredient subtree that fits this pattern's ordering allowance. */
final class PatternStagedCraftingScheduler {

    private final ModulePatternCrafting module;
    private final List<PatternCraftingOrder> stagedCrafts;
    private final Set<Integer> requestingPatterns = new HashSet<>();
    private long lastRequestTick = Long.MIN_VALUE;

    PatternStagedCraftingScheduler(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe,
            List<PatternCraftingOrder> stagedCrafts) {
        this.module = module;
        this.stagedCrafts = stagedCrafts;
    }

    void requestIngredients(boolean capacityChanged) {
        long tick = module.currentWorldTick();
        if (!capacityChanged && lastRequestTick == tick) return;
        lastRequestTick = tick;
        Set<Integer> slots = new LinkedHashSet<>();
        for (PatternCraftingOrder order : new ArrayList<>(stagedCrafts)) slots.add(order.patternSlot);
        for (int slot : slots) requestIngredients(slot);
    }

    void requestIngredients(int patternSlot) {
        if (!requestingPatterns.add(patternSlot)) return;
        try {
            for (PatternCraftingOrder order : new ArrayList<>(stagedCrafts)) {
                if (order.patternSlot != patternSlot) continue;
                if (order.isFullyRequested()) {
                    order.releaseReservations();
                    stagedCrafts.remove(order);
                    continue;
                }
                ItemStack pattern = order.pattern();
                if (pattern == null || !module.isPatternCraftingSupported(pattern)) continue;
                int sets = Math.min(order.remainingSets, module.orderablePatternSets(order));
                sets = Math.min(sets, order.availableSetsFromBranches(pattern));
                if (sets <= 0) continue;
                int requested = order.requestIngredients(pattern, sets);
                module.debugEvent(
                        "SCHED",
                        "slot=%d requested=%d remaining=%d",
                        patternSlot,
                        requested,
                        order.remainingSets);
                if (order.isFullyRequested()) {
                    order.releaseReservations();
                    stagedCrafts.remove(order);
                }
                module.markCraftingStateDirty();
            }
        } finally {
            requestingPatterns.remove(patternSlot);
        }
    }
}
