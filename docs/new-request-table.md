# Logistics Request Table Mk2

Adds a second request table alongside the existing Logistics Request Table. Existing tables retain their GUI
and contents. Mk2 uses the existing logistics item and fluid request engines.

## Terminal and storage

- A combined network list shows items, fluids, internal stock and craftable entries. The hammer marks craftable
  entries; the gold count marks stock in this table. Counts use compact NEI-style rendering.
- Main, item-storage and fluid-storage tabs share a nine-column layout, draggable scrollbar and player inventory.
- Per-player controls select sorting, item/fluid visibility, stored/craftable filtering, request messages,
  search focus, one-way search synchronization to NEI, remembered search text and small/tall terminal layouts.
- Requests open an amount popup initially set to zero, with stock counts and increment/decrement buttons.
- Internal storage and the cursor synchronize from the server after player interaction and routed arrivals.
- Crafting uses the table's storage and the player's inventory, previews the available output and can request
  missing ingredients. NEI recipe import fills the ghost crafting grid.
- Filled and empty fluid cells support whole-stack left clicks and one-cell right clicks, with converted cells
  returned to the inventory for right clicks. Fluid-tab shift clicks accept filled cells; ordinary items stay
  put. Main-tab shift clicks handle fluid cells specially and otherwise insert items. Item-tab shift clicks
  retain normal item behavior.

## Permanent upgrades

The upgrade button opens a pannable, zoomable motherboard with four independent LV-to-UEV storage branches,
a fluid-controller socket and a crafting-monitor socket. Materials enter a nine-slot escrow inventory;
the server validates and consumes the complete recipe before applying an upgrade. Unused materials return
when the upgrade screen closes. Applied upgrades cannot be removed and persist on the table item after breaking
and placing it again.

Item storage starts with 27 slots of 64 items. Fluid storage starts with 27 tanks of 64,000 mB, accessible after
installing the fluid controller. Slot counts, per-slot capacities, tier additions, circuit ore names and circuit
counts are configurable in `LogisticsPipes.cfg` under `requesttable.itemupgrades` and `requesttable.fluidupgrades`.

Storage-tier recipes consume a matching circuit, a chest and robot arm for items or an empty bucket and electric
pump for fluids, and an LP blank/gold/diamond chip for the small/middle/large chip families. Controller recipes
use the specified circuits, pumps or covers and LP chip. Upgrades show prerequisites, current totals and their
own contribution. Installed chips assemble with a short pixel animation.

The permanent crafting-monitor upgrade enables its top-right terminal button. The monitor uses a wider panel
without player inventory or crafting slots. Select a request from the scrollable dropdown and switch between
the resource grid and the draggable dependency tree. The grid combines identical items (including NBT) or
fluids and shows waiting, active and sent quantities; exact values and unfulfilled remainders appear in tooltips.
The tree retains individual order branches, supports mouse-wheel zoom and a center button, and shows machine
or transport progress only when the existing LP orders supply it. The grid scrollbar can be dragged.

Requests containing crafting are observed throughout the connected LP network, including automatic requests,
even before a monitor opens. Simulations and pure stock deliveries are excluded. Finished requests remain for
ten seconds. Without the permanent fluid controller, fluid details are redacted on the server and replaced by
locked placeholders. Monitoring never changes orders, enables tracking, cancels jobs or persists job history.
Sent quantities indicate dispatch, not confirmed insertion into an inventory; completion uses existing LP data.

The observer is isolated in `crafting.monitor`. Existing LP code only hands off a fulfilled root order tree,
registers the observer, and clears it at server shutdown. Monitor subscriptions validate the open table, reach,
security and upgrade. The overview updates at most once per second; selected-job changes at most every five
ticks. Initial trees are split into bounded chunks, assembled atomically, and subsequently updated by node ID
and revision. Closing or leaving the monitor stops its subscription.

## Integration scope

The implementation lives in `crafting.requesttable` and `network.packets.crafting.requesttable`. Shared changes
register the pipe and GUIs, load its configuration, connect NEI and request-result popups, respect permanent
upgrade rules and preserve upgrade NBT on placement and drops. `SimpleStackInventory` supports configurable
slot counts, capacities and integer stack counts so compressed storage survives saving and dropping.

Client packets use direction and open-container checks; upgrade transitions validate reach and pipe security.
The NEI fluid display uses GregTech's fluid display item when GregTech is present, with the normal LP display
as a fallback. It has no dependency on the separate pattern-crafting implementation.

## Verification

Compile using the upstream Gradle wrapper and Java 25 toolchain. No tests were added or run for this feature;
manual in-game verification is planned for storage/cursor sync, fluid cells, NEI integration, crafting from both
inventories, upgrade consumption and persistence, terminal layout and board rendering. For the monitor, check
multi-stage and concurrent jobs, automatic requests, late and simultaneous viewers, separated networks, fluid
controller gating, partial dispatch, progress and unfulfilled remainders, completion retention, large trees,
zoom/panning/scrollbars, small windows and restoration of inventory slots after returning to the main view.
