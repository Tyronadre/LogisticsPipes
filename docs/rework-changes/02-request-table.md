# 02 – Request table rework

## Overview

**Upstream (`GTNH-origin/master`).** There is one request table, `PipeBlockRequestTable` (item "Logistics Request
Table"). Its GUI is `GuiRequestTable`: an items-only network list, a 27-slot internal buffer that stacks to 64, a
"to sort" slot, a disk slot, and a 3x3 crafting grid that crafts from the buffer. Fluids are requested only through
the separate fluid request pipe and `FluidGuiOrderer`, and that list shows only fluids already stored in the network.

**This branch.** The old table is still there and works as before. A second table was added next to it:
[`RequestTablePipe`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTablePipe.java), registered as
"New Request Table" with the display name "Logistics Request Table Mk2". It extends `PipeBlockRequestTable` and adds:

- one network list that shows items **and** fluids, with network, internal and craftable amounts for each entry,
  per-player sorting, visibility, search, terminal-size and request-message settings;
- internal item storage with permanent circuit-tier upgrades, and internal fluid storage with slot/capacity cards;
- a request popup with typed amounts and +/- buttons in place of the old request buttons;
- a ghost (fake) 3x3 crafting grid that crafts from internal storage and then from the player's inventory, plus a
  request icon that orders the missing ingredients for N crafts, plus a craftable output count;
- "Send all" buttons that push internal items or fluids back into the network;
- a Main-only socket for a Fluid Crafting Upgrade, enabling the table's fluid features;
- a Minecraft-style GUI whose height follows the screen size, with top tabs, floating controls and detached upgrade slots.

Smaller related changes: the fluid orderer (`FluidGuiOrderer`) now shows craftable fluids, can filter Both/Craft/Supply,
and has a "Content" button that shows what a fluid request would use. The old table's monitor popup and its watch
packets were extended for pattern crafting (see [01-pattern-crafting.md](01-pattern-crafting.md)).

All new code is in `logisticspipes.crafting.requesttable` (pipe, container, GUI, models) and
`logisticspipes.network.packets.crafting.requesttable` (packets). The new GUI is still a legacy `GuiContainer`, not a
ModularUI screen (see [05-modularui-gui.md](05-modularui-gui.md) for the MUI work and the shared `PacketGuards`).

---

# Features

## New request table pipe (Mk2)

**For the player.** This is a new placeable item, "Logistics Request Table Mk2". Right-clicking it opens the new GUI
(`GuiIDs.GUI_New_Request_Table_ID = 39`). It routes, renders and accepts security settings like the old table,
because it inherits all of that from `PipeBlockRequestTable`.

**How it works.**
- [`RequestTablePipe`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTablePipe.java) extends
  `PipeBlockRequestTable` and implements `IRequestFluid`, so the fluid request code can use it as a requester.
- `openGui` is overridden: it applies the storage upgrades and opens GUI 39 (server: `RequestTableContainer`, client:
  `RequestTableGui`, wired in `GuiHandler`). The parent's `openGui` would put a held disk into the disk slot. The
  override skips that, so the new table never takes a disk.
- `guiOpenedByPlayer`/`guiClosedByPlayer` keep their own `requestTableGuiWatchers` list. The parent's watcher list,
  used for crafting-monitor packets, is still updated through `super`.
- The custom `TransportLayer.handleItem` handles arriving items:
  - **Fluid container items** go into the fluid storage. Whatever doesn't fit becomes a new LP fluid container item.
    That item goes into item storage if there is room, and is dropped in the world otherwise.
  - **Normal items** go into item storage with `addCompressed(stack, true)`, which ignores the item's max stack size.
    Anything that doesn't fit is dropped in the world. The old table instead left the remainder on the routed item.
  - Arrivals mark the network list for a refresh to everyone who has the GUI open. Storage changes are batched into
    one refresh per server tick.
- `sendFailed(FluidIdentifier, Integer)` does nothing. Failures reach the player through the normal
  `MissingItems` popup/chat path.
- `onAllowedRemoval` first calls the parent, which drops `inv`, `toSortInv` and `diskInv`. It then drops every
  stored fluid as LP fluid container items.

**Registration (files owned by other areas).** `LogisticsPipes.java` creates the pipe
(`createPipe(RequestTablePipe.class, "New Request Table", side)`). `GuiIDs`/`GuiHandler` add GUI 39, `en_US.lang`
adds `item.RequestTablePipe(.name)=Logistics Request Table Mk2`, and `TravelingItemRenderer` renders the new item
like the old table.

**NBT / save compatibility.**
- The old table (`PipeBlockRequestTable`, "Request Table") is still registered, keeps its GUI (`GuiRequestTable`) and
  loads exactly as before. **Nothing migrates an old table into the new one.** They are separate items, so existing
  worlds keep their old tables.
- The new table writes the parent's keys (`inv`, `matrix`, `toSortInv`, `diskInv`, `blockRotation`, …) plus:
  - `requestTableItemUpgrades`: integer `slotTier` and `sizeTier` values (0 = Base, 1–10 = LV–UEV).
    The table's dropped item carries this compound and restores it on placement. Stored items still drop separately.
  - `newRequestTableFluidStoragefluids` (list of FluidStack + `index`), `newRequestTableFluidStoragesize` and
    `newRequestTableFluidStoragecapacity`. The key prefix and suffix are joined with no separator.
  - `newRequestTableFluidUpgradeitems` and `newRequestTableFluidUpgradeitemsCount`: the separate, single-item
    fluid-upgrade inventory. Existing stored fluids remain saved when the socket is empty; install the upgrade
    to access them. The upgrade drops with the table when the table is broken.
  - `newRequestTableDisplaySettings`: a list of `{player: UUID string, settings: {sortMode, sortDirection,
    filterMode, requestMessages, searchBoxMode, saveSearch, savedSearch, terminalStyle, showItems, showFluids}}`.
    Only non-default settings are stored, and bad UUIDs are skipped. Older saves keep request messages, items
    and fluids enabled, with Standard search, no remembered text and Small terminal size.
- `SimpleStackInventory` (changed in another area) now also writes `<prefix>itemsCount` and grows itself on load when
  the saved count is larger. This keeps slots added by upgrades through a reload: at load time `container` is still
  `null`, so `updateStorageUpgrades()` returns early. Old saves without `itemsCount` read as 0 and keep the default
  size.
- Each saved item entry also carries integer `lpStackSize`, preserving compressed counts through save/reload.
  Entries without this key still use vanilla `Count`.

## Combined item + fluid network list

**For the player.** The upper panel lists every requestable item and fluid. An entry is shown if it is stored in the
network, stored in this table, or craftable by the network. Fluids use their NEI display icon without a corner marker. The hover tooltip
shows `Network: N`, `Internal: N`, and `Total: N` while shift is held, in mB for fluids. The internal amount is also
drawn small and gold in the top-right corner of the icon. Item totals use NEI fluid-display text sizing and shadows
at the bottom-left: half size normally, three-quarter size with Unicode fonts. Counts use compact suffixes such as
`1k`, `1M`, and `1G`; the gold internal count uses the same size and format. Internal item-storage slots use the same
count overlay while retaining native slot interactions and durability bars. Fluid display items draw their own
amount, with no additional total-count overlay. Tooltips retain exact amounts. The mouse wheel and a draggable
scrollbar scroll the list. The search bar matches
space-separated words against the localized fluid name, the display name and the item's friendly name.

**How it works.**
- [`RequestTableRefreshPacket.buildEntries`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableRefreshPacket.java)
  builds the list on the server from:
  - `logisticsManager.getAvailableItems` (network items);
  - the table's `inv` (internal items);
  - `logisticsManager.getCraftableItems` (craftable). Craftable fluid containers are sorted into the fluid half;
  - `logisticsFluidManager.getAvailableFluid` (network fluids), plus the internal fluid storage. Fluid identifiers
    are turned into fluid-container `ItemIdentifier`s.
- Each entry is a
  [`RequestTableNetworkEntry`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkEntry.java)
  holding a stack, a fluid flag, the network amount, the internal amount and a craftable flag.
- [`RequestTableContentPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableContentPacket.java)
  (server → client) carries the list plus the player's display settings. The client applies it only if the open
  `RequestTableGui` is for the same coordinates (`isForTable`).
- On the client, [`RequestTableNetworkList`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkList.java)
  caches names and the sorted and filtered lists. Sorting reruns only when entries or sort settings change, filtering
  only when entries, the stored/craftable or item/fluid filters, or the search text change.
  [`RequestTableNetworkGrid`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkGrid.java)
  draws the list and finds which entry is under the mouse.
- When the list is sent:
  - **Client asks:** when the GUI opens, after a sub-GUI closes, after a request is submitted, after an ingredient
    request, and when switching back to the network view.
  - **Server pushes to all viewers:** after arrivals, player insertion/removal (including shift-click), crafting,
    network-entry clicks, Send all, or storage resize. Changes are batched once per server tick.

## Inventory synchronization

- `RequestTableContainer` sends a complete
  [`RequestTableInventoryPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableInventoryPacket.java)
  when the GUI opens, after clicks, and when its contents or cursor change. The snapshot includes the item/fluid
  slot counts and capacities, item and upgrade slots, the actual `FluidStack` contents, the viewer's craftable output
  count, and the cursor. Item counts use integers, including compressed stacks above 127. Vanilla slot and
  window-content updates are ignored by this container.
- The client accepts snapshots only for the currently open window and table coordinates. It applies the server's
  storage sizes and rebuilds its slots before applying contents. Slot clicks wait for the initial snapshot and for
  a fresh snapshot after upgrade interactions that can resize storage. Storage upgrades are applied only on the
  server; drawing the GUI cannot resize or drop client storage.
- Item and fluid inventory listeners mark the tile dirty and schedule a combined-list refresh. Container change
  detection also catches direct slot mutations and compares fluid type, amount and NBT. Each open viewer receives
  the current contents, including pipe deliveries and fluid filled/drained through cells.
- Fluid transfers from held containers run on the server. Both fluid-storage slots and combined-list entries use
  the same cursor-stack handling: left-click converts the whole stack and keeps the resulting full or empty stack
  on the cursor. If fluid storage cannot handle the whole stack, the converted cells stay on the cursor and the
  untouched cells return to the player inventory. If those leftovers cannot fit, nothing changes.
  Right-click converts one cell into the player inventory, leaving the remaining cells of the original kind on the
  cursor. If the converted cell cannot fit, fluid storage and the cursor are left untouched. Transfers are planned
  against an isolated copy of fluid storage before committing. Existing Forge registry cell and `IFluidContainerItem`
  transfer support is retained.
- Shift-clicking a filled cell stack in fluid storage empties its fluids into the table and keeps the returned
  containers in the player inventory. Other items do nothing in this view. Item storage uses normal item transfers,
  including for filled cells. The main/network view empties filled cells into fluid storage and transfers other items
  into item storage. A
  [`RequestTableShiftClickPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableShiftClickPacket.java)
  carries the view, window ID, and player inventory index together; the client waits for the server snapshot instead
  of predicting a transfer into the wrong storage. Shift-click transfers stop when fluid or inventory space runs out,
  preserving unconverted cells. A complete conversion reuses the source slot, including with a full player inventory.
- Clicking the Main grid with a held stack uses the same mixed routing, including empty grid cells: filled cells
  empty into fluid storage, compatible upgrades go into upgrade slots, and other items go into item storage.
  The held item need not match the clicked entry. Empty cells clicked on a fluid fill from internal fluid storage,
  retaining the whole-stack left-click and single-cell right-click cursor/inventory placement described above.
  The interaction packet carries an optional clicked entry and distinguishes held-stack transfers from withdrawals;
  all transfers use the server's actual cursor stack.
- Fluid-storage slots, combined-list icons/tooltips, and request-popup icons use the same NEI fluid display as
  pattern crafting. Requests and transport still use LP fluid identifiers. Display items are never applied to fluid
  storage; snapshots preserve the real fluids, amounts, tags, and empty slot indexes.
- Withdrawals to the cursor, player inventory, or an empty hotbar slot are limited to the item's normal stack size.
  Any compressed remainder stays in the table. Dragging cannot reduce an existing compressed stack.

**Multiplayer checks still to run.** Open the same table with two players; insert and remove items with left/right
click, shift-click, dragging, and hotbar keys. Deliver items from another pipe while both GUIs are open. Check the
storage slots, internal amounts in the combined list, cursor, and player inventories on both clients. Repeat with
item-slot upgrades and a 192-item compressed stack, with a full player inventory and full table, and after closing
and reopening the GUI. Also check crafting and fluid-container clicks, which share the container snapshot.
For fluids, repeat filling and draining with one cell and a 64-cell stack in both the fluid-storage and network
views. Check whole-stack left-click and single-cell right-click, including cursor and returned-container placement.
Shift-click filled cells, empty cells and ordinary items in all three views, including with a full player inventory
and storage that can accept only part of the stack. Test empty/full storage, fluid arrivals from pipes, and different fluid/NBT variants.
Check the NEI icons and tooltips in storage, the combined list, and the request popup.

## Request popup (request overlay)

**For the player.** Middle-click an entry, or Ctrl+left/right-click it, to open a small popup. It shows
"In table: N", the item icon, a number field, +1/+10/+100/+1000 and −1/−10/−100/−1000 buttons, OK and a close "x".
The starting amount is 64 for Ctrl+left-click and 1 otherwise, including middle-click. Enter or OK submits, Esc
closes. The popup takes all mouse and key input while it is open.

**How it works.**
- [`RequestTableRequestOverlay`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableRequestOverlay.java)
  is drawn by `RequestTableGui.drawScreen`; it is not a `SubGuiScreen`. The amount is limited to 0…999 999 999.
  When a new list arrives, the selected entry is refreshed and the typed amount is kept.
- Submitting sends
  [`RequestTableSubmitPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableSubmitPacket.java)
  with a fluid flag. The server calls `RequestHandler.requestFluid(player, stack, table, table)` for fluids and
  `RequestHandler.request(player, stack, table)` for items. Requested goods are delivered into the table's internal
  storage.
- When request messages are enabled, the answer (`MissingItems`) goes to `RequestTableGui.handleRequestAnswer` when `Configs.DISPLAY_POPUP` is on. That
  opens the standard `GuiRequestPopup` ("You are missing:" / "Request successful!"). When it is off, the answer goes
  to chat. The `ClientProxy` dispatch for this lives in another area.

## Internal item & fluid storage

**For the player.** Three tabs at the top select **Main**, **Items** and **Fluids**. The storage tabs show a
rendered Minecraft chest and BuildCraft tank, with brown/blue fill bars below their text labels, beside the icons.
The bars are three pixels high inside a five-pixel frame, with two pixels between the letters and bar;
the tabs are 22 px tall. They switch the upper panel
between the network list and a scrollable 9-column slot grid.
The content grid in every tab aligns with the player inventory: both are exactly nine slots wide, with the
scrollbar beside the content grid on its right. Slot borders meet the light-gray GUI background directly, without
an additional dark panel around the inventory.
Clicking the selected tab keeps the current view and scroll position. Enabled controls float to the left,
separated from the central panel by a gap: sort/filter buttons in **Main**, and a **Send all** arrow in storage
views. A speech-bubble **Request messages** toggle appears in every view, with a green check for On and a red cross
for Off. Disabled left-side controls are hidden. Every button has a tooltip.

What the player can do:
- **Item storage:** normal slots. Shift-click moves a stack from storage to the player inventory, and shift-click in
  the player inventory moves a stack into storage.
- **Fluid storage:** click a slot with a fluid container on the cursor:
  - an empty container is filled from the slot;
  - a full container of the same fluid is emptied into the slot;
  - a partly full container is filled.

  This works with `IFluidContainerItem`s, `FluidContainerRegistry` containers and LP fluid containers. Left-click
  handles the whole stack and leaves converted containers on the cursor; right-click handles one and puts the
  converted container in the player inventory. See **Inventory synchronization** for partial transfers.
- **From the network list (no Ctrl):**
  [`RequestTableNetworkInteractPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableNetworkInteractPacket.java)
  → `RequestTablePipe.handleNetworkEntryInteraction`. These clicks only touch the **internal** storage, never the
  network:
  - Holding an item: left-click inserts the whole cursor stack, right-click inserts one, regardless of the entry
    clicked. Compatible upgrades use upgrade slots, like shift-click. Empty grid cells also accept insertion.
  - Empty cursor: left-click takes up to a stack from storage, right-click takes about half (at most half a stack),
    shift+left moves up to a stack into the player inventory, shift+right takes 1.
  - Holding filled cells: deposit their fluid into internal storage, regardless of the clicked entry. Left-click
    converts the stack on the cursor; right-click converts one into the player inventory.
  - Fluid entries with empty cells on the cursor: fill from internal storage using the same cursor/inventory
    placement as the fluid-storage view. Empty cells clicked elsewhere insert as items, like shift-click.
  - Afterwards the server sends the inventory snapshot, including the new cursor stack.
- **Send all:** [`RequestTableSendStoragePacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableSendStoragePacket.java)
  (integer 0 = items, 1 = fluids).
  - `sendStoredItemsToNetwork` sends at most one max-size stack at a time, using `assignDestinationFor` (default
    routing/sinks). It stops on a slot once nothing accepts that item.
  - `sendStoredFluidsToNetwork` sends up to 5000 mB at a time to the target from `logisticsFluidManager.getBestReply`,
    as passive LP fluid containers.

**How it works.**
- **Items** use the inherited `inv` (`SimpleStackInventory`, 27 slots, limit 64), resized by the upgrades.
- **Fluids** use
  [`RequestTableFluidStorage`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableFluidStorage.java).
  It holds a fixed `FluidStack[]` and shows it as an `IInventory`: each slot looks like an LP fluid container item,
  for container snapshots. The `FluidStack`s are the real data. `fill` tops up slots that already hold the
  same fluid first, then uses empty slots. `fillSlot`/`drain` work on one slot. `resize` moves the contents into the
  new layout and drops overflow as fluid container items.
- **Container:** [`RequestTableContainer`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableContainer.java)
  creates slots for item storage, fluid storage (`UnmodifiableSlot`), crafting ghost slots (`DummySlot`),
  result (`HandelableSlot`), restricted upgrade slots, and player inventory. The client moves them each frame
  through `layout(...)`. Slots
  that are hidden or scrolled out of view are parked at (-5000, -5000). Clicks on fluid slots and on the result slot
  are handled in `slotClick`, shift-click moves in `transferStackInSlot`. Storage slots are rebuilt when the server's
  storage sizes change, retaining the crafting, upgrade and player slots.
- **Views and layout:** [`RequestTableView`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableView.java)
  (NETWORK / ITEM_STORAGE / FLUID_STORAGE) and
  [`RequestTableLayout`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableLayout.java). The GUI is
  286 px wide overall, with a 190 px central panel. Small size is up to 272 px tall (four content rows), while
  Tall uses the screen height minus 48 px. Both round down to complete rows in the applicable spacing mode,
  keeping exactly 5 px between the storage-grid border and crafting-grid border. Both leave at least 20 px
  above and below at the minimum Minecraft scaled screen height of 240 px, keeping NEI's controls visible.
  Below 266 px tall it uses compact spacing; below 226 px it reduces the crafting area's bottom margin. The
  network panel fills whatever space is left above the crafting grid and the player inventory.
- **Style and icons:** `RequestTableGuiStyle` draws Minecraft bevels, inset panels and scrollbars;
  `RequestTableIconButton` draws icons from `textures/gui/requesttable_icons.png`, native pixel shapes, or block item renderers.
  Native item icons use depth testing and depth writes so the chest lid and base occlude correctly; labels and
  fill bars render as overlays afterwards. Inactive tabs use a darker face and render before the main panel,
  which covers their lower edge. The selected tab renders afterwards and omits both bottom-border pixels,
  joining the panel.
  The left buttons have no shared panel behind them. All three views support grabbing the scrollbar thumb
  without a jump, clicking its track to reposition it, and dragging past either end with clamped scrolling.
  Scrolling storage repositions client slots immediately; scrollbar clicks and releases bypass inventory clicks.
  Visibility controls (stored/craftable, items, fluids) occupy the outer left column; the other controls use the
  column beside the main panel. Columns are separated by 2 px, buttons vertically by 3 px, and the main panel
  by 7 px. The tank is resolved by registry name `BuildCraft|Factory:tankBlock`; a water bucket is the fallback if it is
  unavailable. The request popup uses the same Minecraft background and slot textures.

## Storage upgrades

The existing **Fluid Crafting Upgrade** enables all fluid features through a separate, single-item socket below
the visibility controls on **Main**. This socket is hidden on the item-storage, fluid-storage and monitor pages.
It is synchronized in the normal inventory snapshot and saved separately from the nine existing upgrade slots.
Main shift-click and held-stack insertion can install it; shift-click returns it to the player when the tanks are
empty. The slot rejects removal while any tank contains fluid, including normal clicks, shift-click, hotbar swaps,
dropping and double-click collection. A tooltip explains the lock, and a small amber latch marks it.

Without this upgrade, the fluid tab and fluid-visibility control are hidden, fluid network entries are omitted,
and the server rejects fluid requests, Send all and cell fill/drain interactions. Main treats filled cells as
ordinary items until fluid support is enabled; item-storage cell handling stays ordinary throughout. Routed LP
fluid containers received without the upgrade stay packaged in item storage, using the normal overflow drop
behavior. Storage-upgrade capacities remain configured while disabled, preserving saved tank contents.

### Permanent item upgrades

The branching-circuit sidebar button opens a separate upgrade screen (GUI 40). Select either **Amount of slots**
or **Slot size**, then a tier. The screen shows its resulting capacity, required circuit, and four material slots.
**Apply upgrade** consumes the required materials and permanently installs the selected tier. Unused materials
return to the player on closing or going Back, with overflow dropped if the player's inventory is full.
Back sits at the top-right of the compact header. The selected node and capacity change share a single row,
aligned left and right; the four material slots line up with the player's first four inventory columns.

The two branches progress independently. Only the next tier of a branch can be applied; later tiers remain locked
until its preceding tier is installed. Installed nodes are green, available nodes are gray, and locked nodes are
darker. The selected node has an amber outline. Mouse-wheel and scrollbar dragging expose the remaining tiers.

| Circuit tier | Additional slots | Additional items per slot | Accepted circuit ore name |
|--------------|-----------------:|--------------------------:|---------------------------|
| Base         |               27 |                        64 | —                         |
| LV           |                9 |                        64 | circuitBasic              |
| MV           |               18 |                       128 | circuitGood               |
| HV           |               27 |                       256 | circuitAdvanced           |
| EV           |               36 |                       512 | circuitData               |
| IV           |               45 |                      1024 | circuitElite              |
| LuV          |               54 |                      2048 | circuitMaster             |
| ZPM          |               63 |                      4096 | circuitUltimate           |
| UV           |               72 |                      8192 | circuitSuperconductor     |
| UHV          |               81 |                     16384 | circuitInfinite           |
| UEV          |               90 |                     32768 | circuitBio                |

Bonuses are cumulative: all tiers give 522 slots and 65,536 items per slot. Either branch costs one circuit of its
tier by default, accepting any matching ore-dictionary circuit variant. `config/LogisticsPipes.cfg` exposes
`requesttable.itemupgrades.baseSlots`, `baseSlotSize`, and tier subcategories `lv` through `uev`, each with
`additionalSlots`, `additionalSlotSize`, `circuitOreName`, and `circuitCount` (1–256). Configured requirements and
bonuses are sent from the server to the upgrade screen. Old item storage cards were removed without migration.

`RequestTableUpgradeContainer` owns each viewer's material slots. The confirmation packet checks the current
window, selection, pipe access, prerequisite, and available materials on the server before installing a tier.
Progress lives on `RequestTablePipe`, so simultaneous viewers cannot purchase the same tier twice. World NBT and
normal table drops preserve both branch levels; placement restores them before the pipe's placement callback.

### Monitoring and fluid cards

The right side of the table has a 28 px wide monitor module and a detached 28×152 px storage-upgrade bar,
separated from the central panel by a 6 px gap. The top-right monitor button sits above its dedicated socket,
with a circuit trace linking the two. The screen, power lamp and trace are gray without an upgrade; inserting a
**Crafting Monitoring Upgrade** lights them green and enables the button, including a subtle scanning pixel.
The button opens a monitor view shell; live crafting-job data is not connected yet. Removing the upgrade returns
the view to Main after the server confirms the inventory change. Crafting controls and storage transfers are
inactive in this shell, while the player inventory and upgrade slots remain accessible.

The first slot accepts **Crafting Monitoring Upgrade** only; the separate bar's eight slots accept the two
fluid storage upgrades (slot count and capacity). Empty-slot tooltips identify these restrictions. These are the
table's actual upgrade slots, limited to 16 items per slot. In the network and
item-storage views, shift-clicking a compatible upgrade installs it here; shift-clicking an installed upgrade
returns it to the player. The fluid-storage view keeps its filled-cell-only shift-click behavior. Capacity per
item slot and fluid tank is shown in the storage tab tooltips. The same slot rules apply to GUI placement,
shift-click, and right-clicking the table with a held upgrade; existing installed stacks remain removable.

| Upgrade (damage)                          | Effect per upgrade item                    |
|-------------------------------------------|--------------------------------------------|
| Request Table Fluid Slot Upgrade (47)     | +9 fluid slots (base 9)                    |
| Request Table Fluid Capacity Upgrade (48) | +64 000 mB per fluid slot (base 64 000 mB) |

Per-tank fluid capacity is the base capacity multiplied by `1 + upgrade count`:
64 000, 128 000, 192 000, … mB. Fluid capacity is independent of the transport tank configuration.

**How it works.**

- `RequestTablePipe.isUpgradeAllowed(slot, stack)` defines the monitor/storage slot split.
  `UpgradeManager.isUpgradeAllowed` applies it together with the usual pipe compatibility check, and is shared
  by the table container, the generic upgrade container and held-upgrade insertion.
- [`RequestTableStorageUpgrade`](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableStorageUpgrade.java)
  is the base class: `isAllowedForPipe` returns true only for `RequestTablePipe`, `isAllowedForModule` returns false,
  and `getAllowedPipes()` returns `"newRequestTable"`. The two subclasses are empty marker classes:
  [`RequestTableFluidInventoryUpgrade`](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableFluidInventoryUpgrade.java),
  [`RequestTableFluidCapacityUpgrade`](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableFluidCapacityUpgrade.java).
  `ItemUpgrade` registers them as damage values 47–48 and reuses existing icons 31 and 15. That file belongs to
  another area.
- `RequestTablePipe.updateStorageUpgrades()` calculates item sizes from permanent branch tiers and fluid sizes
  from matching upgrade cards in the eight-slot bar (16 per slot), then resizes `inv` and fluid storage. This
  method is called from many places instead: open GUI, container constructor and server change detection, every arrival, network click, Send all,
  NBT read/write, building the list, and the fill-level/amount getters. Those getters are also called while the
  client draws the GUI, where upgrade updates now return without changing storage.
- When storage shrinks, item contents are compacted back in and the overflow is **dropped in the world**. Fluid
  overflow is dropped as fluid container items.
- Upgrade sizes use `SimpleStackInventory.setSizeInventory`/`setInventoryStackLimit`. Those methods were added in
  another area.

## Display settings (per player and table)

**For the player.** Floating icon buttons on the left control:
- sort mode: **Name** / **Amount** (total amount, ties broken by name);
- direction: **Asc** / **Desc**;
- filter: **Both** / **Stored** (network + internal > 0) / **Craft** (craftable);
- **Show items** and **Show fluids**, independently, in the Main list;
- search mode: **Standard**, **Auto**, **NEI synced auto**, **NEI synced standard**. Auto focuses the terminal
  search when opening the GUI. Standard opens unfocused. Synced modes copy terminal search edits to NEI;
  editing NEI's search never replaces the terminal text;
- **Save search text: Yes / No**. Yes restores the text when reopening this table, including after reload;
- terminal size: **Small / Tall**. Small shows up to four content rows. Tall uses the available screen height
  while leaving space for NEI at the top and bottom.

Each button has a tooltip. The choice is remembered **per player, per table**, and survives reloads.
Defaults are Standard search, no saved search text, Small size, and both items and fluids visible. Sort, filter,
visibility and search buttons are shown only in Main; terminal size and request messages remain available in
storage views alongside Send all. Inactive buttons are hidden, and the remaining 20 px buttons are packed
vertically in their columns. Both columns fit even the smallest layout. Off visibility toggles stay clickable
so they can be reenabled.
The speech-bubble button toggles request messages in all views. Messages start enabled. Turning them off suppresses
request success, missing-resource and insufficient-energy notifications for item, fluid and ingredient requests,
including both result popups and chat output. Requests still run and crafting-monitor orders are still recorded.

**How it works.**
- [`RequestTableDisplaySettings`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableDisplaySettings.java)
  is an immutable value (Lombok `@Value` / `@With`). Enum ordinals out of range fall back to defaults, and missing
  NBT keys preserve the defaults above. Remembered text is limited to 256 characters, matching NEI's field.
- [`RequestTableDisplaySettingsStore`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableDisplaySettingsStore.java)
  is a UUID → settings map saved in the table's NBT.
- The client changes its settings right away and sends
  [`RequestTableDisplaySettingsPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableDisplaySettingsPacket.java).
  The server saves them, marks the tile dirty and sends back the current network contents with the saved state.
  Both settings packets use the same NBT representation as persistence. Every `RequestTableContentPacket` carries
  that player's settings back. Initial settings restore search and focus once, preserving edits made before the
  first packet and after subsequent refreshes. Search edits filter locally and update NEI immediately in synced
  modes; remembered text is saved on close or another preference change, without a packet per character.
  BuildCraft compatibility mixins preserve facade block metadata as an integer when it cannot fit in a signed byte,
  so GregTech frame material IDs survive facade creation and do not break NEI name filtering. Ordinary facade NBT
  and legacy byte reads are preserved. Already malformed facade variants use a registry-name/metadata fallback
  when their underlying item throws during name lookup; valid localized names and hollow suffixes stay intact.
  Restored text places the caret at the end. The terminal search uses the same inset border and gray fill as
  inventory slots, with a darker gray fill while focused; long restored text scrolls to its end. Both edges of
  the search bar align with the nine-column content grid, excluding its scrollbar. The bar is 12 px tall,
  with two pixels above and below its text and no placeholder. Its vertical gaps to the
  tabs and grid match: 4 px normally, 2 px on compact screens, and 1 px on the smallest screens.
  Changing terminal size rebuilds the client layout in place, retaining search text and crafting-request amount.
  Toggling messages keeps the list's scroll
  position. Item/fluid submission and ingredient requests pass the saved preference to `RequestHandler`, which
  gates notification delivery separately from requests and monitor callbacks. Other request handlers keep their
  existing default behavior.

## Crafting grid (ghost recipe, craft from storage, request ingredients)

**For the player.**
- The 3x3 grid holds ghost items only (like the old table's matrix), and the result shows the matching recipe output.
  The NEI "?" recipe transfer works on this GUI (the overlay handler is registered in another area).
- The grid aligns with the player inventory. Output, set count, the 18×18 px request button, and the craftable
  count sit beside the grid, without separate title or count rows. The request button's right edge aligns with
  the rightmost content-grid slot. The crafting area reserves 68 px above the
  player inventory (60 px on compact screens, 56 px on the smallest), down from 102 px in the regular layout.
- The 10×10 px **x** button next to the grid clears it and aligns with the grid's top border. A separate
  128×128 px white X with a black outline is scaled to 8×8 px inside the button.
  Its texture uses linear filtering and clamped edges, without red pixels.
- Clicking the result crafts once onto the cursor, if the cursor is empty or holds the same item with room left.
  Shift-clicking crafts up to 64 items straight into the player inventory.
- Ingredients come from the table's **internal storage first, then the player's main inventory**.
- Crafting remainders (empty buckets, damaged tools, …) go to internal storage, then the player inventory, and are
  dropped otherwise.
- The **Sets** number field (1–9999) and the green request icon (or Enter in the field) order N crafts' worth of grid ingredients
  from the network. Amounts already in internal storage are subtracted; the player inventory is not counted.
- **Craftable: N** shows the output-item count supported by table storage plus the viewer's main inventory,
  including the hotbar. It uses internal ingredients first, like actual crafting. Its tooltip identifies the unit
  as output items. The cursor and armor slots are excluded. The count does not simulate crafting
  callbacks or reusing returned containers/tools.

**How it works.**

- `RequestTablePipe.getCraftableAmount(player)` and `RequestTableCraftingCounter` check the selected recipe
  against copies of internal and player ingredients, with the same ore-dictionary and strict matching passes as
  crafting. Batches exhaust at least one ingredient stack each, so large compressed stacks do not require one
  iteration per craft. Each viewer's server container recalculates when item storage, the player's main inventory,
  the ghost grid or its result changes and includes the count in its snapshot. Fluid-only changes do not trigger
  recalculation, and players opening the same table can have different counts.
- `RequestTablePipe.getResultForClick(player, held, amount)` / `craftIntoPlayerInventory(player, limit)` build a
  `CraftingPreview` for each craft:
  - `getCurrentRecipe()` runs `cacheRecipe()` and then scans `CraftingUtil.getRecipeList()` for a recipe that matches
    the grid and produces the cached result;
  - `findIngredients` picks the slots to use, first with ore-dictionary matching and, if that fails, with strict
    `equalsForCrafting` matching;
  - the recipe is checked again against the actual ingredients.
- The ingredients are then removed, `SlotCrafting.onPickupFromSlot` fires (achievements and crafting events), and
  the remainders are collected.
- `RequestTableContainer.slotClick` sends result-slot clicks here: mode 1 → `craftIntoPlayerInventory(64)`, mode 0 →
  one craft to the cursor.
- [`RequestTableClearCraftingPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableClearCraftingPacket.java)
  clears the matrix. The client also clears its own copy right away.
- [`RequestTableRequestIngredientsPacket`](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableRequestIngredientsPacket.java)
  (integer = number of crafts) → `requestCraftingIngredients` → `RequestHandler.requestList(player, list, table)`.

## GUI

[`RequestTableGui`](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableGui.java) (client only,
extends `LogisticsBaseGuiScreen`) puts the features above together:
- header: title, search bar (network view only), Send all (storage views only), item/fluid storage buttons;
- the main panel (network grid, storage slots or crafting-monitor shell);
- the display buttons on the left;
- the crafting area with the clear button, the amount field and Req;
- the player inventory;
- the top-right monitor button and its attached upgrade socket.

Many things are drawn with plain `Gui.drawRect` calls: the storage icons, the crafting arrow, the clear button and
the overlay buttons. All strings in this GUI are hard-coded English.

## Fluid orderer changes (fluid request pipe GUI)

**For the player.** The fluid request pipe GUI (`FluidGuiOrderer`) now has:
- three small buttons: **Refresh**, **Content**, and a **Both / Craft / Supply** cycle button;
- craftable fluids in its list (amount 0) as well as stored ones;
- "Content", which shows what requesting the selected fluid amount would use and what is missing, with the same
  component popup the item orderer uses.

**How it works.**
- [`FluidGuiOrderer`](../../src/main/java/logisticspipes/gui/orderer/FluidGuiOrderer.java): the buttons are now
  `SmallGuiButton`s. Button id 13 ("Content") sends the new
  [`RequestFluidComponentPacket`](../../src/main/java/logisticspipes/network/packets/orderer/RequestFluidComponentPacket.java).
  Button id 9 cycles a local `DisplayOptions` (Both → Craft → Supply) and refreshes.
- [`RequestFluidOrdererRefreshPacket`](../../src/main/java/logisticspipes/network/packets/orderer/RequestFluidOrdererRefreshPacket.java)
  now extends `Integer2CoordinatesPacket`. `integer2` is the option (0 Both, 1 SupplyOnly, 2 CraftOnly), passed on to
  the new `RequestHandler.refreshFluid(player, pipe, DisplayOptions)`. For Craft/Both that method adds craftable
  fluid containers that aren't already stored.
- `RequestFluidComponentPacket` → the new `RequestHandler.simulateFluid` builds a `RequestTree` for a
  `FluidResource` without starting it and returns a `ComponentList` (used/missing). `RequestHandler` itself is
  documented with [01-pattern-crafting.md](01-pattern-crafting.md). `requestFluid` now also returns early if the
  stack is not a fluid.

---

# Changes to upstream classes

| Class | Change | Why |
|---|---|---|
| [`PipeBlockRequestTable`](../../src/main/java/logisticspipes/pipes/PipeBlockRequestTable.java) | Adds `watchedPatternCraftingRequests` (client side), `handleClientSidePatternCraftingInfo`, and the helpers `sendWatchedRequestToGuiWatchers`/`sendWatchedRequestToPlayer`. These send `OrdererWatchPacket` **and** a new `PatternCraftingWatchPacket` (roots from `PatternCraftingMonitorRegistry.build`). Expired/removed watches also clear the pattern map. | Crafting-monitor support for pattern crafting (see [01-pattern-crafting.md](01-pattern-crafting.md)). `RequestTablePipe` inherits it, but the new GUI does not show monitor data (see gaps). |
| [`GuiRequestTable`](../../src/main/java/logisticspipes/gui/orderer/GuiRequestTable.java) (old table GUI) | The monitor button (id 100) opens `PatternRequestMonitorPopup` if the watched order has pattern crafting data, otherwise the old `RequestMonitorPopup`. | Pattern crafting tree view in the old table. |
| [`FluidGuiOrderer`](../../src/main/java/logisticspipes/gui/orderer/FluidGuiOrderer.java) | Smaller buttons; new Content and Both/Craft/Supply buttons; refresh sends the display option. | Fluid orderer changes above. |
| [`RequestFluidOrdererRefreshPacket`](../../src/main/java/logisticspipes/network/packets/orderer/RequestFluidOrdererRefreshPacket.java) | Base class `IntegerCoordinatesPacket` → `Integer2CoordinatesPacket`; maps `integer2` to `RequestHandler.DisplayOptions`. | Carries the fluid display filter. **The wire format changed**, so client and server must run the same build (normal for LP packets). |

Upstream classes changed for this feature but documented in other areas: `SimpleStackInventory` (resizable,
`itemsCount`), `ItemUpgrade` (damages 47–48), `GuiIDs`/`GuiHandler` (GUIs 39 and 40), `PacketGuards` (`getOpenRequestTable`),
`PacketHandler` (fails clearly on unregistered packets), `RequestHandler` (`refreshFluid` with options,
`simulateFluid`), `ClientProxy` (request-answer and recipe-import routing to `RequestTableGui`),
`LogisticsCraftingOverlayHandler`/`NEILogisticsPipesConfig` (NEI overlay on `RequestTableGui`), `LogisticsPipes`
(registration), `TravelingItemRenderer`, and `en_US.lang`.

---

# Known gaps / discrepancies

Seen in the code. Where `docs/pattern-crafting.md` §8 or `docs/testing-checklist.md` already lists an issue, its ID is
given.

1. **Large-stack persistence (D4) fixed in code.** `SimpleStackInventory` saves integer `lpStackSize` alongside
   vanilla `Count`; request-table snapshots also use integer counts. In-game save/reload verification is pending.
2. **Unstackable items stack (D11).** Arrivals, shift-click into storage and network-entry inserts use
   `addCompressed(stack, true)`, which ignores the item's max stack size. 64 swords can sit in one slot.
   `moveInternalItemToPlayerInventory` puts back what the player inventory didn't take with `addCompressed`, and
   anything that doesn't fit back is lost. A room check runs first, so this is unlikely.
3. **Performance (F3, F4).** Each craft scans the full recipe list again, and the preview runs twice (ore-dict and
   strict pass). A 64-item shift-craft does this for every craft. Each storage-change refresh rebuilds the whole
   network list (`getAvailableItems` + `getCraftableItems` + `getAvailableFluid`) and sends
   it to every viewer. Storage changes are now batched once per server tick; each craft still scans recipes.
4. **`RequestTableDisplaySettingsPacket` reads client coordinates.** It uses `getPipe(player.worldObj)`, not
   `PacketGuards.getOpenRequestTable`. That doesn't match the S4 note ("all request table C→S packets resolve the
   table from `player.openContainer`"). The only effect is that a player can set their own display settings on any
   loaded table in their world.
5. **Storage sync retest (G2).** Slot counts and contents now come from server snapshots, and client upgrade
   calculations cannot resize or drop storage. Dedicated-server testing with upgraded storage is still required.
6. **Fluid cards apply late.** Card changes take effect the next time `updateStorageUpgrades()` runs.
   Removing fluid cards drops overflowing fluids in the world. Permanent item tiers apply immediately and cannot
   be removed; lowering their configured capacities can still compact storage and drop overflow.
7. **Old-table features missing in the new GUI.**
   - No disk slot; `openGui` no longer takes a held disk, and `diskInv` is still saved and dropped.
   - No "to sort" slot; the inherited `toSortInv` logic still runs every tick but nothing can fill it.
   - The crafting-monitor button and view shell are present, but live jobs are not connected. The inherited watch
     logic keeps sending `OrdererWatchPacket`/`PatternCraftingWatchPacket` to viewers, but `RequestTableGui` does
     not yet show that data.
8. **No migration and no recipes.** Old request tables are never turned into Mk2. This repo has no crafting recipe
   for `logisticsNewRequestTable` or for fluid upgrade damages 47–48: `RecipeManager` and `SolderingStationRecipes` don't
   mention them. The items are only in the creative tab and NEI.
9. **Missing lang key.** `RequestTableStorageUpgrade.getAllowedPipes()` returns `"newRequestTable"`, but
   `en_US.lang` has no `item.upgrade.info.newRequestTable`, so the shift tooltip shows the raw key.
10. **Fallback amount 1.** Middle-click opens the request popup with amount 1, and only Ctrl+left-click starts at 64.
    `RequestTableRequestOverlay.open(entry, font, w, h)`, which would start at the entry's stack size, is never
    called.
11. **Ore-dict first.** `getCraftingPreview` tries ore-dictionary matching before strict matching. When both kinds of
    candidate are present, an ore-dict-equivalent item can be used even though an exact match exists.
12. **Unused fields.** `RequestTableRefreshPacket` still sends the dimension in its integer, and
    `RequestTableSubmitPacket` and `RequestTableNetworkInteractPacket` still send dimension and coordinates. The
    server ignores them by design (S4), so they are just dead payload.
13. **Hard-coded English.** GUI labels, button tooltips, "Craftable:" and "In table:" are not localized.

---

# Files

### `logisticspipes.crafting.requesttable`

| Class/file                                                                                                                         | Status | +/-      | Summary                                                                                                                                  |
|------------------------------------------------------------------------------------------------------------------------------------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------|
| [RequestTableContainer](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableContainer.java)                       | A      | +411/-0  | Server/client container: all slots created once and moved by the layout; fluid-slot clicks, result-slot crafting, shift-click transfers. |
| [RequestTableCraftingCounter](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableCraftingCounter.java)           | A      | —        | Counts recipe output from copied internal and player ingredients in batches, without consuming items.                                    |
| [RequestTableDisplaySettings](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableDisplaySettings.java)           | A      | +90/-0   | Immutable sort mode / direction / filter value with NBT and ordinal helpers.                                                             |
| [RequestTableDisplaySettingsStore](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableDisplaySettingsStore.java) | A      | +69/-0   | Per-player-UUID display settings saved in the table NBT; defaults are not stored.                                                        |
| [RequestTableFluidStorage](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableFluidStorage.java)                 | A      | +372/-0  | Resizable slotted FluidStack storage shown as an IInventory of LP fluid containers; fill/drain/resize/NBT/drop.                          |
| [RequestTableGui](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableGui.java)                                   | A      | +718/-0  | Client GUI: network grid, storage views, display buttons, crafting area, request popup, packet sends.                                    |
| [RequestTableGuiStyle](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableGuiStyle.java)                         | A      | —        | Shared Minecraft bevels, inset panels and scrollbars.                                                                                    |
| [RequestTableIconButton](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableIconButton.java)                     | A      | —        | Icon buttons and top tabs with native block rendering, selection highlights and storage fill bars.                                       |
| [requesttable_icons.png](../../src/main/resources/assets/logisticspipes/textures/gui/requesttable_icons.png)                       | A      | —        | Pixel icon atlas for navigation, sort/filter, Send all, request and clear actions.                                                       |
| [RequestTableLayout](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableLayout.java)                             | A      | +146/-0  | Pixel layout of the adaptive-height GUI (panel, buttons, crafting, player inventory).                                                    |
| [RequestTableNetworkEntry](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkEntry.java)                 | A      | +116/-0  | One list entry: stack, fluid flag, network/internal amounts, craftable flag.                                                             |
| [RequestTableNetworkGrid](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkGrid.java)                   | A      | +173/-0  | Draws the scrollable item/fluid icon grid, tooltips, internal-amount labels and hit-testing.                                             |
| [RequestTableNetworkList](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableNetworkList.java)                   | A      | +145/-0  | Client-side cached sort/filter/search over the received entries.                                                                         |
| [RequestTablePipe](../../src/main/java/logisticspipes/crafting/requesttable/RequestTablePipe.java)                                 | A      | +1168/-0 | Mk2 table pipe: storage upgrades, item/fluid arrivals, storage interactions, crafting, Send all, NBT.                                    |
| [RequestTableRequestOverlay](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableRequestOverlay.java)             | A      | +240/-0  | Modal request popup with an amount field, ±1/10/100/1000 buttons and OK/close.                                                           |
| [RequestTableView](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableView.java)                                 | A      | +22/-0   | Enum of the upper panel modes: NETWORK, ITEM_STORAGE, FLUID_STORAGE.                                                                     |

### Permanent item upgrade classes

| Class/file                                                                                                                                     | Summary                                                                                                  |
|------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| [RequestTableItemUpgradeBranch](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableItemUpgradeBranch.java)                   | Independent slot-count and slot-size branches.                                                           |
| [RequestTableItemUpgradeConfig](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableItemUpgradeConfig.java)                   | Editable bases, cumulative bonuses, circuit ore names and costs; server-to-client config serialization.  |
| [RequestTableUpgradeContainer](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableUpgradeContainer.java)                     | Per-viewer material inventory, circuit filtering, server validation/consumption, unused-material return. |
| [RequestTableUpgradeGui](../../src/main/java/logisticspipes/crafting/requesttable/RequestTableUpgradeGui.java)                                 | Scrollable two-branch tree, requirements and material-entry slots.                                       |
| [RequestTableOpenUpgradesPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableOpenUpgradesPacket.java) | Opens the upgrade screen or returns to Main through the player's current container.                      |
| [RequestTableItemUpgradePacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableItemUpgradePacket.java)   | Selects a node or confirms an upgrade in the current window.                                             |
| [RequestTableUpgradeStatePacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableUpgradeStatePacket.java) | Authoritative branch progress, selection and configured requirements.                                    |

### `logisticspipes.crafting.requesttable.upgrades`

| Class/file | Status | +/- | Summary |
|---|---|---|---|
| [RequestTableFluidCapacityUpgrade](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableFluidCapacityUpgrade.java) | A | +7/-0 | Marker upgrade: +1× base capacity per fluid slot (damage 48). |
| [RequestTableFluidInventoryUpgrade](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableFluidInventoryUpgrade.java) | A | +7/-0 | Marker upgrade: +9 fluid slots (damage 47). |
| [RequestTableStorageUpgrade](../../src/main/java/logisticspipes/crafting/requesttable/upgrades/RequestTableStorageUpgrade.java) | A | +37/-0 | Base `IPipeUpgrade`: allowed only on `RequestTablePipe`, never on modules. |

### `logisticspipes.gui.orderer`

| Class/file | Status | +/- | Summary |
|---|---|---|---|
| [FluidGuiOrderer](../../src/main/java/logisticspipes/gui/orderer/FluidGuiOrderer.java) | M | +49/-3 | Small buttons, Content (simulate) button, Both/Craft/Supply filter sent with refresh. |
| [GuiRequestTable](../../src/main/java/logisticspipes/gui/orderer/GuiRequestTable.java) | M | +6/-1 | Old table GUI opens the pattern-crafting monitor popup for pattern orders. |

### `logisticspipes.network.packets.crafting.requesttable`

| Class/file                                                                                                                                                 | Status | +/-     | Summary                                                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|---------|--------------------------------------------------------------------------------------------------------------------|
| [RequestTableClearCraftingPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableClearCraftingPacket.java)           | A      | +32/-0  | C→S: clear the ghost crafting grid of the open table.                                                              |
| [RequestTableContentPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableContentPacket.java)                       | A      | +93/-0  | S→C: network entries plus the player's display settings for the open GUI.                                          |
| [RequestTableDisplaySettingsPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableDisplaySettingsPacket.java)       | A      | +58/-0  | C→S: save the player's sort/filter and request-message settings, then acknowledge the state with network contents. |
| [RequestTableNetworkInteractPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableNetworkInteractPacket.java)       | A      | +80/-0  | C→S: click on a network entry, moving items/fluids between cursor and internal storage.                            |
| [RequestTableRefreshPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableRefreshPacket.java)                       | A      | +145/-0 | C→S refresh; `buildEntries` merges network, internal and craftable items and fluids.                               |
| [RequestTableRequestIngredientsPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableRequestIngredientsPacket.java) | A      | +32/-0  | C→S: request the missing grid ingredients × N crafts.                                                              |
| [RequestTableSendStoragePacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableSendStoragePacket.java)               | A      | +36/-0  | C→S: Send all internal items (0) or fluids (1) to the network.                                                     |
| [RequestTableSetCursorPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableSetCursorPacket.java)                   | A      | +31/-0  | S→C cursor stack sync; ignored on the server.                                                                      |
| [RequestTableSubmitPacket](../../src/main/java/logisticspipes/network/packets/crafting/requesttable/RequestTableSubmitPacket.java)                         | A      | +63/-0  | C→S: request an item or fluid amount into the open table.                                                          |

### `logisticspipes.network.packets.orderer`

| Class/file | Status | +/- | Summary |
|---|---|---|---|
| [RequestFluidComponentPacket](../../src/main/java/logisticspipes/network/packets/orderer/RequestFluidComponentPacket.java) | A | +33/-0 | C→S: simulate a fluid request and reply with a used/missing component list. |
| [RequestFluidOrdererRefreshPacket](../../src/main/java/logisticspipes/network/packets/orderer/RequestFluidOrdererRefreshPacket.java) | M | +16/-3 | Now `Integer2CoordinatesPacket`; `integer2` picks the Both/Supply/Craft fluid list. |

### `logisticspipes.pipes`

| Class/file | Status | +/- | Summary |
|---|---|---|---|
| [PipeBlockRequestTable](../../src/main/java/logisticspipes/pipes/PipeBlockRequestTable.java) | M | +45/-13 | Watch helpers also send pattern-crafting monitor trees; client map of pattern watches. |
