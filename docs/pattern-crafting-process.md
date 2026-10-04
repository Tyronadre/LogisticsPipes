# Current pattern crafting process

All runtime classes shown below are in `src/main/java/logisticspipes/crafting/`. Arrows describe execution or
asynchronous data flow; they do not imply that every method directly calls the next method.

## Planning, ordering, and insertion

```mermaid
flowchart TD
    Tree["Initial RequestTree / RequestTreeNode<br/>PatternCraftingBranch.reserveProviderPromises()"]
    Register["ModulePatternCrafting.fullFillStagedCrafting(...)<br/>PatternStagedCraftingCoordinator.fulfill(...) / registerOrder(...)"]
    Order["PatternCraftingOrder<br/>Stable branch reference and saved recipe snapshot"]
    Schedule["PatternStagedCraftingScheduler.requestIngredients(...)<br/>PatternCraftingCapacity.orderableSets(order)"]
    Limit{"New sets fit per-pattern allowance?<br/>64 + mode-eligible target room - pending sets"}
    Subtree["PatternCraftingOrder.requestIngredients(...) / requestFromBranches(...)<br/>PatternCraftingBranch.request(...) / copyForAmount(...)"]
    Child["Child crafting promise creates another PatternCraftingOrder<br/>Repeat bounded subtree expansion recursively"]
    Supplier["Fulfill reserved supplier promises<br/>PatternTargetInformation identifies owner, pattern and ingredient"]
    Arrive["ModulePatternCrafting.itemArrived(...)<br/>PatternCraftingArrivalHandler.itemArrived(...)<br/>Accept outstanding owned ingredients without a capacity gate"]
    Buffer["PatternStackBufferHandler<br/>Ingredient buffer owned by branch, grouped by pattern"]
    Push["PatternCraftingBufferDispatcher.pushBufferedIngredients(...)<br/>PatternSatelliteDispatchHandler.findInsertableBufferedPlan(...)"]
    Check{"DispatchPlan.canDispatch()<br/>Complete sets, shared targets, active recipes and input room"}
    Prepare["PatternCraftingBatchOutputs.prepare(plan)<br/>Record expected full outputs and preparation lease"]
    Stage["DispatchPlan.dispatch(buffer)<br/>Reserve satellites and stage routed item deliveries"]
    Wait["PipeItemsPatternSatelliteLogistics.itemArrived(...)<br/>stagedPatternInputAmount(...)<br/>Pending preparation resumes independently by branch"]
    Commit["DispatchPlan.dispatch(buffer)<br/>Recheck all targets; insertLocal(...) / insertPatternInput(...)<br/>insertStagedPatternInput(...)"]
    Finish["PatternCraftingBufferDispatcher.finishDispatch(plan)<br/>PatternCraftingBatchOutputs.committed(plan)<br/>PatternCraftingOrder.ingredientsDispatched(sets)<br/>DispatchPlan.release()"]
    Machine["Machine processes complete sets"]
    Tree --> Register --> Order --> Schedule --> Limit
    Limit -->|Wait for allowance| Schedule
    Limit -->|Admit next slice| Subtree
    Subtree -->|Crafting dependency| Child --> Register
    Subtree -->|Supplier material| Supplier --> Arrive --> Buffer --> Push --> Check
    Check -->|Wait for complete set, room or mode| Buffer
    Check -->|Ready| Prepare --> Stage
    Stage -->|Routed items pending| Wait --> Stage
    Stage -->|All staged; targets still fit| Commit --> Finish --> Machine
    Commit -->|Adapter inserted less than simulated: retain recovery progress| Wait
    Finish --> Schedule
```

Blocking and Smart Blocking both permit additional complete sets of the active recipe; they differ in ordering
allowance. Non blocking permits different recipes when targets fit. Prepared batches hold their shared targets
exclusively until commit. Shared item inventories and overlapping fluid tank handlers are simulated together to
avoid spending the same physical room twice.

## Extraction, distribution, and cancellation

```mermaid
flowchart TD
    Tick["PatternCraftingResultExtractor.tick()<br/>Every sixth tick: PatternCraftingBatchOutputs.collect()"]
    Output["AdjacentInventoryHandler.extract(...) / extractFluid(...)<br/>or PatternByproductExtractionTargetCache.extractItem(...) / extractFluid(...)"]
    Transit["Satellite result transport<br/>PatternTargetInformation.batchOutput(...)"]
    Arrival["ModulePatternCrafting.itemArrived(...)<br/>PatternCraftingBatchOutputs.arrival(...)"]
    Stock["Owned output staging<br/>Batch.completedSets() / deliverable(output)<br/>Include main yield and every byproduct"]
    Take["PatternCraftingResultExtractor.distributeItems() / distributeFluids()<br/>PatternCraftingBatchOutputs.take(order, maximum)"]
    Route["PatternCraftingResultExtractor.route(...)<br/>Same-pipe result: direct ModulePatternCrafting.itemArrived(...)<br/>Other consumer: active routing with branch metadata"]
    Claims["PatternCraftingBatchOutputs.futureClaims(...)<br/>PatternCraftingBranch.unrequestedOutputClaims(...)<br/>Protect promises in not-yet-expanded subtrees"]
    Default["PatternCraftingBatchOutputs.returnUnclaimedOutputs() / sendToStorage(...)<br/>Default routing; material may drop when no sink accepts it"]
    Cancel["PatternCraftingInstanceRegistry.cancelInstance(...)<br/>PatternCraftingCancelHandler<br/>Return unused/staged inputs; redirect late arrivals"]
    CancelBatch["PatternCraftingBatchOutputs.cancelInstance(...)<br/>Retain inserted production, drain eventual outputs to default routing"]
    Release["Batch.drained()<br/>Active recipe no longer prevents different recipes at shared targets"]
    Clean["PatternCraftingBatchOutputs.cleanup()<br/>PatternStagedCraftingCoordinator.cleanupCompletedOutputOrders()"]
    Tick --> Output
    Output -->|Local| Stock
    Output -->|Satellite| Transit --> Arrival --> Stock
    Stock -->|Complete result sets| Take --> Route
    Stock --> Claims
    Claims -->|Unclaimed completed surplus| Default
    Output -->|All expected output extracted| Release
    Cancel --> CancelBatch --> Tick
    Cancel --> Default
    Stock --> Clean
    Route --> Clean
```

Physical output collection may be partial to avoid filling a small byproduct hatch. Consumers can take output only
from complete result sets. Main-output overproduction remains tracked even if the original requested amount is
smaller than the recipe yield. Cancellation routes collected output immediately and keeps polling inserted production.

## Tick and restart

`ModulePatternCrafting.tick()` restores staged orders, retries deferred satellite cleanup, waits for unresolved
restoration, validates fluid support, retries lost deliveries, extracts/distributes results, resumes/inserts buffered
sets, then schedules additional ingredient subtrees. Output draining precedes further insertion.

NBT persists the flat branch graph and order references through `PatternStagedCraftingCoordinator`, recipe snapshots
and quantities through `PatternCraftingOrder`, buffers/requests through `PatternStackBufferHandler` and
`PatternStackRequestHandler`, production through `PatternCraftingBatchOutputs`, and independent pending dispatches
through `PatternCraftingBufferDispatcher`. Satellites persist their delivery staging and preparation reservation.

`ModulePatternCrafting.itemLost(...)`, `PatternCraftingBatchOutputs.lost(...)`, and
`PatternLostIngredientHandler.retryLostItems()` reconcile lost output/ingredient delivery. Satellite staging retries
lost ingredient packets through `PipeItemsPatternSatelliteLogistics.throttledUpdateEntity()`.

The small legacy extraction path handles older orders saved before batch accounting. Generic machine APIs still
require reliable simulation/insertion; an unexpectedly short insertion preserves exact recovery progress.

See [the implemented design](pattern-crafting-simplified-design.md) and [the complete source map](pattern-crafting-code-map.md).
