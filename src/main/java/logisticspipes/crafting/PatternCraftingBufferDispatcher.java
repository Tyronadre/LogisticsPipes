package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.utils.AdjacentTile;

/** Moves complete buffered ingredient sets into the local crafting target and linked satellites. */
final class PatternCraftingBufferDispatcher {

    private final ModulePatternCrafting module;
    private final PatternStackBufferHandler ingredientBuffer;
    private final AdjacentInventoryHandler adjacentInventory;
    private final PatternCraftingBlockingHandler blockingHandler;
    private final PatternSatelliteDispatchHandler satelliteDispatchHandler;
    private final PatternCraftingIngredientPlanner ingredientPlanner;
    /**
     * A set that only partly went into the target (or its satellites). It is finished before anything else is pushed,
     * so the target never keeps half a set. Its exact progress is saved with its owning branch.
     */
    private PatternSatelliteDispatchHandler.DispatchPlan pendingDispatch;
    private final Map<PatternSatelliteDispatchHandler.DispatchPlan, Boolean> deferredCleanup = new LinkedHashMap<>();

    PatternCraftingBufferDispatcher(ModulePatternCrafting module, PatternStackBufferHandler ingredientBuffer,
            AdjacentInventoryHandler adjacentInventory, PatternCraftingBlockingHandler blockingHandler,
            PatternSatelliteDispatchHandler satelliteDispatchHandler,
            PatternCraftingIngredientPlanner ingredientPlanner) {
        this.module = module;
        this.ingredientBuffer = ingredientBuffer;
        this.adjacentInventory = adjacentInventory;
        this.blockingHandler = blockingHandler;
        this.satelliteDispatchHandler = satelliteDispatchHandler;
        this.ingredientPlanner = ingredientPlanner;
    }

    void refreshSatelliteBatches() {
        blockingHandler.hasSatelliteBatches();
    }

    void readFromNBT(NBTTagCompound tag) {
        pendingDispatch = tag.hasKey("patternPendingDispatch")
                ? satelliteDispatchHandler.readFromNBT(tag.getCompoundTag("patternPendingDispatch"))
                : null;
        deferredCleanup.clear();
        NBTTagList cleanups = tag.getTagList("patternDispatchCleanup", 10);
        for (int i = 0; i < cleanups.tagCount(); i++) {
            NBTTagCompound entry = cleanups.getCompoundTagAt(i);
            PatternSatelliteDispatchHandler.DispatchPlan plan = satelliteDispatchHandler.readFromNBT(entry);
            boolean returnToStorage = entry.getBoolean("returnToStorage");
            deferredCleanup.put(plan, returnToStorage);
            if (returnToStorage) PatternCraftingInstanceRegistry.recordCancellation(plan.ownerReference().instanceId());
        }
    }

    void writeToNBT(NBTTagCompound tag) {
        if (pendingDispatch != null) {
            tag.setTag("patternPendingDispatch", pendingDispatch.writeToNBT());
        } else {
            tag.removeTag("patternPendingDispatch");
        }
        NBTTagList cleanups = new NBTTagList();
        for (Map.Entry<PatternSatelliteDispatchHandler.DispatchPlan, Boolean> cleanup : deferredCleanup.entrySet()) {
            NBTTagCompound entry = cleanup.getKey().writeToNBT();
            entry.setBoolean("returnToStorage", cleanup.getValue());
            cleanups.appendTag(entry);
        }
        tag.setTag("patternDispatchCleanup", cleanups);
    }

    void deferCleanup(PatternSatelliteDispatchHandler.DispatchPlan plan, boolean returnToStorage) {
        deferredCleanup.put(plan, returnToStorage);
        module.markCraftingStateDirty();
    }

    void retryDeferredCleanup() {
        Iterator<Map.Entry<PatternSatelliteDispatchHandler.DispatchPlan, Boolean>> iterator = deferredCleanup.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Map.Entry<PatternSatelliteDispatchHandler.DispatchPlan, Boolean> cleanup = iterator.next();
            if (cleanup.getValue() ? cleanup.getKey().abandon() : cleanup.getKey().release()) {
                iterator.remove();
                module.markCraftingStateDirty();
            }
        }
    }

    void pushBufferedIngredients() {
        if (pendingDispatch != null) {
            resumePendingDispatch();
            return;
        }
        refreshSatelliteBatches();
        AdjacentTile connected = adjacentInventory.getConnected();
        if (connected == null) {
            module.debugEventThrottled(
                    "BUFFER",
                    "push skipped: no connected inventory bufferedSlots=%d",
                    ingredientBuffer.size());
            return;
        }
        PipeItemsPatternCraftingLogistics.BlockingMode mode = module.getEffectiveBlockingMode();
        module.debug(
                "push tick mode=%s runningCraft=%d bufferedSlots=%d",
                mode,
                blockingHandler.runningCraft(),
                ingredientBuffer.size());
        if (mode == PipeItemsPatternCraftingLogistics.BlockingMode.OFF) {
            for (int patternSlot : new ArrayList<>(ingredientBuffer.asMap().keySet())) {
                for (PatternCraftingReference owner : ingredientBuffer.owners(patternSlot)) {
                    if (completeBufferedSets(owner, patternSlot) > 0) {
                        pushBufferedIngredientsFor(owner, patternSlot);
                    }
                }
            }
            return;
        }
        blockingHandler.refreshRunningCraftState(connected);
        if (blockingHandler.runningCraft() >= 0) {
            pushBufferedIngredientsFor(blockingHandler.runningCraftReference(), blockingHandler.runningCraft());
        }
    }

    void pushBufferedIngredientsFor(int patternSlot) {
        pushBufferedIngredientsFor(findCompleteBufferedOwner(patternSlot), patternSlot);
    }

    private void pushBufferedIngredientsFor(PatternCraftingReference ownerReference, int patternSlot) {
        if (ownerReference == null) {
            return;
        }
        if (pendingDispatch != null) {
            // a partly inserted set goes first; pushBufferedIngredients() resumes it
            return;
        }
        ItemStack pattern = module.getPatternStack(patternSlot);
        if (pattern == null) {
            module.debugEvent("BUFFER", "push slot=%d dropped buffer: pattern missing", patternSlot);
            ingredientBuffer.removeAll(patternSlot);
            return;
        }
        if (blockingHandler.shouldSkipPushFor(patternSlot)) {
            module.debugEventThrottled(
                    "BUFFER",
                    "push slot=%d skipped: running craft locked by slot=%d",
                    patternSlot,
                    blockingHandler.runningCraft());
            return;
        }
        int bufferedSets = completeBufferedSets(ownerReference, patternSlot);
        PatternSatelliteDispatchHandler.DispatchPlan plan = satelliteDispatchHandler
                .findInsertableBufferedPlan(ownerReference, patternSlot, pattern, bufferedSets);
        PatternSatelliteDispatchHandler.DispatchResult result = plan == null
                ? PatternSatelliteDispatchHandler.DispatchResult.NONE
                : plan.dispatch(ingredientBuffer);
        if (result == PatternSatelliteDispatchHandler.DispatchResult.NONE) {
            module.debugEventThrottled("BUFFER", "push slot=%d failed: bufferedSets=%d", patternSlot, bufferedSets);
            return;
        }
        if (result == PatternSatelliteDispatchHandler.DispatchResult.PARTIAL) {
            pendingDispatch = plan;
            module.debugEvent("BUFFER", "push slot=%d partly inserted, finishing the set later", patternSlot);
            module.markCraftingStateDirty();
            return;
        }
        finishDispatch(plan);
    }

    /**
     * Retries the rest of a partly inserted set. Gates like blocking mode don't apply: the set is already partly in the
     * target and must be completed.
     */
    private void resumePendingDispatch() {
        PatternSatelliteDispatchHandler.DispatchPlan plan = pendingDispatch;
        if (module.getPatternStack(plan.patternSlot()) == null) {
            module.debugEvent("BUFFER", "pending set slot=%d dropped: pattern missing", plan.patternSlot());
            abandonPendingDispatch();
            return;
        }
        if (plan.dispatch(ingredientBuffer) != PatternSatelliteDispatchHandler.DispatchResult.COMPLETE) {
            module.debugEventThrottled("BUFFER", "pending set slot=%d still waiting for room", plan.patternSlot());
            return;
        }
        pendingDispatch = null;
        module.debugEvent("BUFFER", "pending set slot=%d finished", plan.patternSlot());
        finishDispatch(plan);
    }

    /**
     * Bookkeeping once a whole set went into the target: lock the running craft, track the satellite batch and register
     * the byproducts of the dispatched sets.
     */
    private void finishDispatch(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        int patternSlot = plan.patternSlot();
        PatternCraftingReference ownerReference = plan.ownerReference();
        int insertedSets = plan.sets();
        module.debugEvent("BUFFER", "push slot=%d inserted sets=%d", patternSlot, insertedSets);
        blockingHandler.markDispatched(patternSlot, ownerReference, plan.satelliteBatch(), plan.usesLocalInventory());
        PatternCraftingOrder order = PatternCraftingInstanceRegistry.find(ownerReference);
        if (order != null) {
            order.ingredientsDispatched(insertedSets);
        }
        module.debugEvent(
                "BUFFER",
                "push slot=%d buffer after insert remainingSets=%d runningCraft=%d adjacentBatch=%s",
                patternSlot,
                completeBufferedSets(patternSlot),
                blockingHandler.runningCraft(),
                blockingHandler.runningCraftInAdjacent());
        module.requestIngredientsForStagedCrafts();
        module.markCraftingStateDirty();
    }

    /**
     * Pattern slot of the partly inserted set, or -1 when there is none.
     */
    int pendingDispatchSlot() {
        return pendingDispatch == null ? -1 : pendingDispatch.patternSlot();
    }

    /**
     * Gives up on the partly inserted set when it belongs to {@code instanceId} (any instance when null). What reached
     * the satellites goes back to storage; the rest is still buffered and is flushed by the caller.
     */
    boolean abandonPendingDispatch(java.util.UUID instanceId) {
        if (pendingDispatch == null) {
            return false;
        }
        PatternCraftingReference owner = pendingDispatch.ownerReference();
        if (instanceId != null && (owner == null || !instanceId.equals(owner.instanceId()))) {
            return false;
        }
        return abandonPendingDispatch();
    }

    private boolean abandonPendingDispatch() {
        if (!pendingDispatch.abandon()) deferCleanup(pendingDispatch, true);
        pendingDispatch = null;
        module.markCraftingStateDirty();
        return true;
    }

    int completeBufferedSets(int patternSlot) {
        ItemStack pattern = module.getPatternStack(patternSlot);
        int sets = ingredientPlanner.completeBufferedSets(patternSlot, pattern);
        module.debug("complete buffered sets slot=%d sets=%d", patternSlot, sets);
        return sets;
    }

    private int completeBufferedSets(PatternCraftingReference owner, int patternSlot) {
        ItemStack pattern = module.getPatternStack(patternSlot);
        return ingredientPlanner.completeBufferedSets(owner, patternSlot, pattern);
    }

    PatternCraftingReference findCompleteBufferedOwner(int patternSlot) {
        for (PatternCraftingReference owner : ingredientBuffer.owners(patternSlot)) {
            if (completeBufferedSets(owner, patternSlot) > 0) {
                return owner;
            }
        }
        return null;
    }

    int findCompleteBufferedPattern() {
        for (int patternSlot : ingredientBuffer.keySet()) {
            if (module.getPatternStack(patternSlot) == null) {
                ingredientBuffer.removeAll(patternSlot);
                continue;
            }
            if (completeBufferedSets(patternSlot) > 0) {
                return patternSlot;
            }
        }
        return -1;
    }

    void refreshRunningCraftState() {
        blockingHandler.refreshRunningCraftState(adjacentInventory.getConnected());
    }
}
