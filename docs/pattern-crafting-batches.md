# Pattern crafting execution

The current system uses the [simplified per-pattern model](pattern-crafting-simplified-design.md).
This replaces whole-plan workspace reservation and a separate last-pattern blocking controller.

Ingredient admission is 64 sets per pattern in Blocking mode. Smart and Non blocking may also use current target
capacity, accounting for buffered/requested sets and competing admitted target allowances. Arrivals accept owned
ingredients without another capacity gate. Dispatches commit at most 64 complete sets per invocation, with independent
satellite preparation state per branch. This batch size is an execution chunk, not the total request limit.

Outstanding producing batches retain their recipe, owner, output targets, and quantities until their results have been
extracted/distributed. Partial output collection keeps small hatches clear; distribution uses complete result sets.
Unused surplus and cancellations use default routing without waiting for storage space.

See [the current execution diagram](pattern-crafting-process.md) for actual class/method names and
[the source inventory](pattern-crafting-code-map.md) for the full GraphML diagrams.
