package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;

/** Moves complete buffered ingredient sets into the local crafting target and linked satellites. */
final class PatternCraftingBufferDispatcher {

    private final ModulePatternCrafting module;
    private final PatternStackBufferHandler ingredientBuffer;
    private final PatternSatelliteDispatchHandler satelliteDispatchHandler;
    private final PatternCraftingIngredientPlanner ingredientPlanner;
    /**
     * A prepared batch awaiting satellite deliveries or recovering an unexpectedly short insert. Its exact progress is
     * saved with its owning branch; another batch cannot enter its reserved targets during preparation.
     */
    private final Map<PatternCraftingReference, PatternSatelliteDispatchHandler.DispatchPlan> pendingDispatches = new LinkedHashMap<>();
    private final Map<PatternSatelliteDispatchHandler.DispatchPlan, Boolean> deferredCleanup = new LinkedHashMap<>();

    PatternCraftingBufferDispatcher(ModulePatternCrafting module, PatternStackBufferHandler ingredientBuffer,
            AdjacentInventoryHandler adjacentInventory, PatternSatelliteDispatchHandler satelliteDispatchHandler,
            PatternCraftingIngredientPlanner ingredientPlanner) {
        this.module = module;
        this.ingredientBuffer = ingredientBuffer;
        this.satelliteDispatchHandler = satelliteDispatchHandler;
        this.ingredientPlanner = ingredientPlanner;
    }

    void readFromNBT(NBTTagCompound tag) {
        pendingDispatches.clear();
        NBTTagList pending = tag.getTagList("patternPendingDispatches", 10);
        for (int i = 0; i < pending.tagCount(); i++) {
            PatternSatelliteDispatchHandler.DispatchPlan plan = satelliteDispatchHandler
                    .readFromNBT(pending.getCompoundTagAt(i));
            pendingDispatches.put(plan.ownerReference(), plan);
        }
        if (tag.hasKey("patternPendingDispatch")) {
            PatternSatelliteDispatchHandler.DispatchPlan plan = satelliteDispatchHandler
                    .readFromNBT(tag.getCompoundTag("patternPendingDispatch"));
            pendingDispatches.put(plan.ownerReference(), plan);
        }
        deferredCleanup.clear();
        // Old saves held satellite locks until inputs were consumed. Release those obsolete locks without
        // retrieving ingredients already handed to a machine; legacy output orders finish through compatibility.
        NBTTagList legacy = tag.getTagList("patternSatelliteBatches", 10);
        for (int i = 0; i < legacy.tagCount(); i++)
            deferredCleanup.put(satelliteDispatchHandler.readFromNBT(legacy.getCompoundTagAt(i)), false);

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
        tag.removeTag("patternPendingDispatch");
        NBTTagList pending = new NBTTagList();
        for (PatternSatelliteDispatchHandler.DispatchPlan plan : pendingDispatches.values())
            pending.appendTag(plan.writeToNBT());
        tag.setTag("patternPendingDispatches", pending);
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
        for (PatternSatelliteDispatchHandler.DispatchPlan plan : new ArrayList<>(pendingDispatches.values()))
            resumePendingDispatch(plan);
        PipeItemsPatternCraftingLogistics.BlockingMode mode = module.getEffectiveBlockingMode();
        module.debug(
                "push tick mode=%s runningCraft=%d bufferedSlots=%d",
                mode,
                module.batchOutputs().firstActivePattern(),
                ingredientBuffer.size());
        for (int patternSlot : new ArrayList<>(ingredientBuffer.asMap().keySet())) {
            for (PatternCraftingReference owner : ingredientBuffer.owners(patternSlot)) {
                if (completeBufferedSets(owner, patternSlot) > 0) {
                    pushBufferedIngredientsFor(owner, patternSlot);
                }
            }
        }
    }

    void pushBufferedIngredientsFor(int patternSlot) {
        pushBufferedIngredientsFor(findCompleteBufferedOwner(patternSlot), patternSlot);
    }

    private void pushBufferedIngredientsFor(PatternCraftingReference ownerReference, int patternSlot) {
        if (ownerReference == null) {
            return;
        }
        if (pendingDispatches.containsKey(ownerReference)) return;
        ItemStack pattern = module.patternForOwner(ownerReference, patternSlot);
        if (pattern == null) {
            module.debugEvent("BUFFER", "push slot=%d dropped buffer: pattern missing", patternSlot);
            ingredientBuffer.removeAll(patternSlot);
            return;
        }
        int bufferedSets = completeBufferedSets(ownerReference, patternSlot);
        PatternSatelliteDispatchHandler.DispatchPlan plan = satelliteDispatchHandler
                .findInsertableBufferedPlan(ownerReference, patternSlot, pattern, bufferedSets);
        if (plan != null && !module.batchOutputs().prepare(plan)) plan = null;
        PatternSatelliteDispatchHandler.DispatchResult result = plan == null
                ? PatternSatelliteDispatchHandler.DispatchResult.NONE
                : plan.dispatch(ingredientBuffer);
        if (result == PatternSatelliteDispatchHandler.DispatchResult.NONE) {
            if (plan != null) module.batchOutputs().discardPrepared(plan);
            module.debugEventThrottled("BUFFER", "push slot=%d failed: bufferedSets=%d", patternSlot, bufferedSets);
            return;
        }
        if (result == PatternSatelliteDispatchHandler.DispatchResult.PARTIAL) {
            pendingDispatches.put(ownerReference, plan);
            module.debugEvent("BUFFER", "push slot=%d prepared; waiting for delivery or insertion", patternSlot);
            module.markCraftingStateDirty();
            return;
        }
        finishDispatch(plan);
    }

    /**
     * Retries the rest of a partly inserted set. Gates like blocking mode don't apply: the set is already partly in the
     * target and must be completed.
     */
    private void resumePendingDispatch(PatternSatelliteDispatchHandler.DispatchPlan plan) {
        if (plan.dispatch(ingredientBuffer) != PatternSatelliteDispatchHandler.DispatchResult.COMPLETE) {
            module.debugEventThrottled("BUFFER", "pending set slot=%d still waiting for room", plan.patternSlot());
            return;
        }
        pendingDispatches.remove(plan.ownerReference());
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
        PatternCraftingOrder order = PatternCraftingInstanceRegistry.find(ownerReference);
        module.batchOutputs().committed(plan);
        if (!plan.release()) deferCleanup(plan, false);
        if (order != null) {
            order.ingredientsDispatched(insertedSets);
        }
        module.debugEvent(
                "BUFFER",
                "push slot=%d buffer after insert remainingSets=%d runningCraft=%d adjacentBatch=%s",
                patternSlot,
                completeBufferedSets(patternSlot),
                module.batchOutputs().firstActivePattern(),
                module.batchOutputs().firstActivePattern() >= 0);
        module.requestIngredientsForStagedCrafts();
        module.markCraftingStateDirty();
    }

    /**
     * Gives up on the partly inserted set when it belongs to {@code instanceId} (any instance when null). What reached
     * the satellites goes back to storage; the rest is still buffered and is flushed by the caller.
     */
    boolean abandonPendingDispatch(java.util.UUID instanceId) {
        boolean changed = false;
        for (PatternSatelliteDispatchHandler.DispatchPlan plan : new ArrayList<>(pendingDispatches.values())) {
            if (instanceId != null && !instanceId.equals(plan.ownerReference().instanceId())) continue;
            module.batchOutputs().discardPrepared(plan);
            if (!plan.abandon()) deferCleanup(plan, true);
            pendingDispatches.remove(plan.ownerReference());
            changed = true;
        }
        if (changed) module.markCraftingStateDirty();
        return changed;
    }

    int completeBufferedSets(int patternSlot) {
        int sets = 0;
        for (PatternCraftingReference owner : ingredientBuffer.owners(patternSlot))
            sets += completeBufferedSets(owner, patternSlot);
        return sets;
    }

    private int completeBufferedSets(PatternCraftingReference owner, int patternSlot) {
        ItemStack pattern = module.patternForOwner(owner, patternSlot);
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

}
