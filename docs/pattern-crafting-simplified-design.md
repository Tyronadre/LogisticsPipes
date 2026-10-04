# Simplified pattern crafting design

This is the implemented scheduling model. The source map and diagrams are linked at the end.

## Request planning

1. Generate the initial dependency tree and reserve supplier materials.
2. For each pattern, calculate how many additional complete ingredient sets may be ordered.
3. Create a subtree for that number of sets, bounded by the remaining original request and supplier reservations.
4. Repeat recursively for dependency patterns until reaching reserved supplier materials.
5. Fulfill the selected subtree. Each item/fluid delivery carries its owning branch, pattern, and ingredient slot.
6. Repeat scheduling as sets move out of the ingredient buffer and results fulfill dependent orders.

Every pattern has an internal allowance of **64 complete ingredient sets**. This allowance is shared by all orders
using that pattern, rather than granted independently to every order. Concrete items and fluid quantities are
derived from the selected recipe; a set is not an item stack or inventory slot.

| Mode | Ordering allowance for the pattern |
| --- | --- |
| Blocking | 64 ingredient sets. |
| Smart Blocking | 64 sets plus the complete sets that currently fit in eligible machine targets, provided their active recipe matches this pattern. Idle targets can accept the pattern. |
| Non blocking | 64 sets plus the complete sets that currently fit in machine targets. |

Ordering subtracts buffered and outstanding requested/in-flight ingredients from the allowance. Incomplete
deliveries count against the sets already admitted; repeated scheduling cannot order the same missing ingredients
again. Capacity calculations cover the whole recipe across local inventories, fluid tanks, and satellites, including
shared targets. Different patterns cannot independently spend the same target space: reserve that space during
admission or otherwise account for already admitted target allowances.

Target capacity is checked when admitting additional sets. Arrivals always enter the owning pattern's buffer,
without another capacity rejection. If target room disappears before insertion, previously admitted deliveries
may overflow the 64-set internal allowance. This overflow does not authorize additional orders: buffered and
in-flight quantities still count toward admission. External target changes can therefore cause temporary overflow.

## Arrival and normalized insertion

On arrival, identify the owning branch and pattern, account for the delivery, and add its concrete item/fluid
quantity to that pattern's buffer. Then invoke the same normalized insertion logic used by periodic scheduling.

The insertion logic:

1. Select an order with complete concrete ingredient sets in its pattern buffer.
2. Resolve all local and satellite input/output targets for that recipe.
3. Check active inserted batches and mode compatibility at shared physical machine targets.
4. Check that every input target can accept the selected whole sets.
5. Reserve the participating targets while preparing insertion. Routed satellite ingredients wait in staging
   until all ingredients for the selected sets have arrived.
6. Recheck all targets, then commit complete sets across every target. Never intentionally insert a partial set.
7. Remove the committed ingredient quantities from buffers/staging, release preparation reservations, and
   record the inserted sets against the order and pattern.

Blocking and Smart Blocking use the same insertion compatibility rule: a different recipe cannot enter shared
targets while an active recipe still has inserted sets whose complete outputs have not been extracted. The modes
differ in how many ingredient sets they may order. Non blocking permits different complete recipes where the
machine supports them and the targets fit.

Active production is tracked per pattern and order, not by one last-pattern field on the pipe. Shared-target
checks also cover different pipes using the same physical inventories/tanks. Disjoint hatches of one multiblock
require a machine identity if they must share a lock; inventory identity alone cannot establish that relationship.

## Result sets and distribution

Each successful insertion records the number of sets and the recipe's expected output quantities and locations.
An output set includes the selected main output and every configured byproduct. The consumer's requested amount
does not change the physical recipe yield.

When a complete result set is available at its configured targets:

1. Extract all outputs of that set.
2. Account for the completed set against its producing order and pattern.
3. Distribute outputs to the consumers/parent branches according to their claims.
4. Send unclaimed surplus through default routing.
5. Wake dependent scheduling and release recipe blocking when no inserted sets of that active recipe remain.

If multiple active batches produce indistinguishable outputs in shared targets, allocate them deterministically
to matching outstanding batches. Do not extract more than the recorded production. Keep production records
until all inserted sets are accounted for, even if the original consumer order has already been fulfilled.

## Cancellation

Cancel the branch's orders, stop new requests, and release unused supplier and preparation reservations.
Return buffered/staged items and fluids through their normal default routing. Late in-flight deliveries belonging
to a cancelled branch also use default routing. No storage capacity reservation is required; normal routing may
drop material into the world when it has no destination, which is accepted behavior.

Inputs already consumed by a machine cannot be returned. Retain records of its inserted sets so their eventual
outputs can be extracted and sent through default routing. Cancellation identity remains recognizable until
outstanding deliveries and inserted production are accounted for.

## Persistence

Use stable branch IDs, parent branch references, and orders referencing their owning branch. Do not duplicate
the dependency tree in multiple independent structures.

The serialized branch/order records must also retain the runtime quantities needed to resume:

- Remaining supplier claims and unfulfilled ingredient requests, including outstanding deliveries.
- Concrete buffered ingredients and any satellite staging/preparation state.
- Inserted sets and the expected outputs still awaiting extraction.
- Claims on produced results, delivered quantities, and cancellation state.
- The recipe snapshot and target references used by already admitted or inserted work.

IDs and parent links reconstruct ownership; they alone do not reconstruct these changing quantities. Runtime
state may live directly in the branch/order records rather than requiring another graph. Existing pipe buffers
and transport packets keep their own contents and delivery metadata, reconciled without issuing duplicate orders
when loading. Changing a configured pattern must not reinterpret its existing work as a different recipe.

## Required assumptions and remaining boundaries

- The initial tree must resolve recursion into a finite plan or reject an unsatisfiable dependency cycle. A
  recursive recipe with usable seed material can be valid. A recipe cannot depend on its own future output as
  the only source of its starting ingredients.
- Only dependency-ready complete sets enter a machine. An inserted set must finish without needing a different
  recipe to be inserted into the machine it blocks. Machine recipes that themselves require such interleaving
  are incompatible with this blocking rule.
- Complete-result-set extraction assumes the machine can retain a full set of all configured outputs until
  extraction. If a byproduct fills a small output tank before the main result is available, waiting for the whole
  result set can stall the machine. Such adapters must either guarantee full-set output room or collect partial
  outputs into owned result staging, marking the set complete only after all outputs are accounted for.
- Full-set insertion across multiple targets requires a reliable atomic commit boundary. Generic simulate-then-
  insert APIs cannot guarantee this if a handler changes capacity or processes a recipe during insertion.
  Supported adapters must guarantee the checked insertion or provide rollback/recovery for an unexpectedly
  short insertion. Partial insertion is never an intentional scheduling strategy.
- Lost transport must either reliably report failure for replacement accounting or explicitly fail the affected
  branch. Silent loss would leave an ingredient request waiting forever. Late originals and replacements must
  not both fulfill the same claim.
- Fair retrying is needed for waiting patterns. A stopped machine, unloaded target, or externally changed inventory
  can pause work; the scheduler cannot guarantee progress against those conditions.

With these assumptions and accounting rules, the core loop remains: **plan subtree → request owned ingredients →
buffer → insert full sets → extract/account result sets → distribute → repeat**.

## Implementation entry points

- `PatternStagedCraftingScheduler`: bounded subtree expansion.
- `PatternCraftingCapacity.orderableSets`: shared per-pattern allowance; pending sets include buffer/staging and requested ingredients.
- `PatternCraftingOrder`: owning branch, recipe snapshot, remaining ingredient sets, admission in progress, and inserted sets.
- `PatternCraftingBufferDispatcher` / `PatternSatelliteDispatchHandler.DispatchPlan`: one insertion path for local and satellite inputs; pending preparations are independent by branch.
- `PatternCraftingBatchOutputs`: outstanding production per pattern/order, collected result sets, transport loss, and future subtree claims.
- `PatternCraftingResultExtractor`: typed item/fluid distribution and default routing of extras.
- `PatternCraftingArrivalHandler` / `PatternCraftingCancelHandler`: owned arrivals and cancellation/default routing.
- `PatternStagedCraftingCoordinator` / `PatternCraftingBranch`: flat persisted branch graph and order references.

The whole-plan `PatternCraftingWorkspace` and the separate last-pattern `PatternCraftingBlockingHandler` have been removed.
There are no whole-job item/fluid workspace budgets or output-stock storage-capacity reservations. Supplier reservations
from the initial tree remain until their branch slices are fulfilled or cancelled. Physical output collection may be
incremental to keep small byproduct hatches clear; consumers receive quantities only from completed result sets.
An admitted target allowance is exclusive against another pattern borrowing the same physical targets. Mode checks
compare recipe inputs/outputs and substitution settings; the GUI slot or main-output selection does not define recipe equality.

Older saved producing batches and pending dispatches are read in their existing formats. Pre-batch saved orders retain
a small per-order extraction compatibility path; their obsolete satellite preparation locks are released after targets resolve.
The old numeric workspace budget is ignored. Already ordered deliveries are not discarded when their new allowance is smaller.

See [the runtime flow](pattern-crafting-process.md) and [the generated class/function map](pattern-crafting-code-map.md).
