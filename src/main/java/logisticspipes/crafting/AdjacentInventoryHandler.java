package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.crafting.patternStack.PatternStackHelper;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.AdjacentTile;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.InventoryHelper;
import logisticspipes.utils.SidedInventoryMinecraftAdapter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.transactor.ITransactor;
import logisticspipes.utils.tuples.Pair;

class AdjacentInventoryHandler {

    private final ModulePatternCrafting module;
    private final PipeItemsPatternCraftingLogistics pipe;
    private long contentCacheTick = Long.MIN_VALUE;
    private net.minecraft.tileentity.TileEntity contentCacheTile;
    private ForgeDirection contentCacheOrientation = ForgeDirection.UNKNOWN;
    private boolean emptyCached;
    private boolean cachedEmpty;

    AdjacentInventoryHandler(ModulePatternCrafting module, PipeItemsPatternCraftingLogistics pipe) {
        this.module = module;
        this.pipe = pipe;
    }

    void invalidate() {
        contentCacheTick = Long.MIN_VALUE;
        contentCacheTile = null;
        contentCacheOrientation = ForgeDirection.UNKNOWN;
        clearContentCacheValues();
    }

    AdjacentTile getConnected() {
        return pipe.getConnectedInventoryTile();
    }

    boolean isConnectedToPatternCraftingTable() {
        AdjacentTile connected = getConnected();
        return connected != null && connected.tile instanceof PatternLogisticsCraftingTableTileEntity;
    }

    public boolean hasConnectedTE() {
        AdjacentTile connected = getConnected();
        return connected != null && connected.tile != null;
    }

    List<AdjacentTile> locateFluidHandlers() {
        List<AdjacentTile> handlers = new ArrayList<>();
        AdjacentTile connected = getConnected();
        if (connected != null && connected.tile instanceof IFluidHandler) {
            handlers.add(connected);
        }
        return handlers;
    }

    boolean canInsertPatternIngredients(ItemStack pattern, List<PatternIngredientAssignment> assignments) {
        AdjacentTile connected = getConnected();
        if (connected == null || assignments == null || assignments.isEmpty()) {
            return false;
        }
        if (connected.tile instanceof PatternLogisticsCraftingTableTileEntity table) {
            if (!table.isIdle()) return false;
            for (PatternIngredientAssignment assignment : assignments) {
                ItemIdentifierStack item = PatternStackHelper.asSolidStack(assignment.stack());
                if (item == null) {
                    return false;
                }
                ItemStack stack = item.makeNormalStack();
                if (table.roomForPatternPipeSlot(assignment.inputSlot(), stack) < stack.stackSize) {
                    return false;
                }
            }
            return true;
        }
        List<ItemIdentifierStack> solidIngredients = new ArrayList<>();
        List<PatternFluidStack> fluidIngredients = new ArrayList<>();
        for (PatternIngredientAssignment assignment : assignments) {
            ItemIdentifierStack item = PatternStackHelper.asSolidStack(assignment.stack());
            if (item != null) {
                solidIngredients.add(item.clone());
                continue;
            }
            if (assignment.stack() instanceof PatternFluidStack fluid) {
                fluidIngredients.add(fluid.copy());
            }
        }
        if (!solidIngredients.isEmpty()) {
            if (!(connected.tile instanceof IInventory)) {
                return false;
            }
            if (!canFitPatternSetsDisregardingSlots(getInsertableInventory(connected), solidIngredients, 1)) {
                return false;
            }
        }
        if (!fluidIngredients.isEmpty()) {
            if (!(connected.tile instanceof IFluidHandler handler)) {
                return false;
            }
            if (!canFitFluids(handler, getFluidInsertionOrientation(connected), fluidIngredients, 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Inserts the assignments into the connected target and returns how much of each actually went in, index by index.
     * Callers must take exactly these amounts out of their buffer: a short insert can leave part of the set in the
     * target, and counting it as not inserted would duplicate it (D1).
     */
    int[] insertPatternIngredients(ItemStack pattern, List<PatternIngredientAssignment> assignments) {
        int[] inserted = new int[assignments.size()];
        AdjacentTile connected = getConnected();
        if (connected == null) {
            return inserted;
        }
        if (connected.tile instanceof PatternLogisticsCraftingTableTileEntity table) {
            int[] tableInserted = table.insertPatternPlanFromPatternPipe(assignments);
            for (int amount : tableInserted) {
                if (amount > 0) {
                    invalidateContentCache();
                    break;
                }
            }
            return tableInserted;
        }
        for (int i = 0; i < assignments.size(); i++) {
            PatternIngredientAssignment assignment = assignments.get(i);
            ItemIdentifierStack item = PatternStackHelper.asSolidStack(assignment.stack());
            if (item != null) {
                inserted[i] = insert(item.clone());
            } else if (assignment.stack() instanceof PatternFluidStack fluid) {
                inserted[i] = insertFluid(fluid.copy());
            }
        }
        return inserted;
    }

    /**
     * Whether {@code sets} times the given fluids fit into the handler at the same time (D3).
     * <p>
     * {@code fill(simulate)} only answers for one fluid against the current contents, so two fluids aimed at a single
     * tank would both pass. Each fluid (amounts of the same fluid merged) must pass the simulation, and when there are
     * several distinct fluids they must also fit the reported tanks together: a fluid goes into tanks that already hold
     * it, then into empty tanks, and a tank never takes two fluids. A handler without tank info can't prove that, so it
     * only accepts one fluid at a time.
     */
    private boolean canFitFluids(IFluidHandler handler, ForgeDirection side, List<PatternFluidStack> fluids, int sets) {
        return canFitFluids(Collections.singletonList(new Pair<>(handler, side)), fluids, sets);
    }

    /** Simulates a full batch across the tanks shared by all adjacent handlers. */
    static boolean canFitFluids(List<Pair<IFluidHandler, ForgeDirection>> handlers, List<PatternFluidStack> fluids,
            int sets) {
        List<PatternFluidStack> merged = mergeFluids(fluids, sets);
        if (merged.size() == 1) {
            PatternFluidStack fluid = merged.get(0);
            int remaining = fluid.getAmount();
            for (Pair<IFluidHandler, ForgeDirection> handler : handlers) {
                int filled = handler.getValue1()
                        .fill(handler.getValue2(), fluid.getFluid().makeFluidStack(remaining), false);
                remaining -= Math.max(0, Math.min(remaining, filled));
                if (remaining <= 0) {
                    return true;
                }
            }
            return false;
        }
        List<FluidCapacitySnapshot> snapshots = new ArrayList<>();
        for (Pair<IFluidHandler, ForgeDirection> handler : handlers) {
            snapshots.add(new FluidCapacitySnapshot(handler.getValue1(), handler.getValue2()));
        }
        for (PatternFluidStack fluid : merged) {
            int[] accepted = new int[snapshots.size()];
            long totalAccepted = 0;
            for (int h = 0; h < snapshots.size(); h++) {
                FluidCapacitySnapshot snapshot = snapshots.get(h);
                accepted[h] = Math.max(0, snapshot.handler.fill(snapshot.side, fluid.makeFluidStack(), false));
                totalAccepted += accepted[h];
            }
            if (totalAccepted < fluid.getAmount()) {
                return false;
            }
            long remaining = fluid.getAmount();
            // Use matching tanks across every handler before claiming any empty tank.
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int h = 0; h < snapshots.size() && remaining > 0; h++) {
                    FluidCapacitySnapshot snapshot = snapshots.get(h);
                    for (int i = 0; i < snapshot.room.length && remaining > 0 && accepted[h] > 0; i++) {
                        FluidIdentifier contents = snapshot.contents[i];
                        if (pass == 0 ? !fluid.getFluid().equals(contents) : contents != null) {
                            continue;
                        }
                        int used = (int) Math.min(remaining, Math.min(snapshot.room[i], accepted[h]));
                        if (used <= 0) {
                            continue;
                        }
                        snapshot.contents[i] = fluid.getFluid();
                        snapshot.room[i] -= used;
                        accepted[h] -= used;
                        remaining -= used;
                    }
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    /** Simulate different insertion endpoints against one shared snapshot per physical tank handler. */
    static boolean canFitFluidInputs(List<List<Pair<IFluidHandler, ForgeDirection>>> targets,
            List<PatternFluidStack> fluids) {
        Map<IFluidHandler, FluidCapacitySnapshot> shared = new java.util.IdentityHashMap<>();
        for (int group = 0; group < fluids.size(); group++) {
            PatternFluidStack fluid = fluids.get(group);
            List<Pair<IFluidHandler, ForgeDirection>> handlers = targets.get(group);
            List<FluidCapacitySnapshot> snapshots = new ArrayList<>();
            List<Integer> accepted = new ArrayList<>();
            java.util.Set<IFluidHandler> visited = java.util.Collections
                    .newSetFromMap(new java.util.IdentityHashMap<>());
            for (Pair<IFluidHandler, ForgeDirection> handler : handlers) {
                if (!visited.add(handler.getValue1())) continue;
                snapshots.add(
                        shared.computeIfAbsent(
                                handler.getValue1(),
                                ignored -> new FluidCapacitySnapshot(handler.getValue1(), handler.getValue2())));
                accepted.add(Math.max(0, handler.getValue1().fill(handler.getValue2(), fluid.makeFluidStack(), false)));
            }
            int remaining = fluid.getAmount();
            for (int pass = 0; pass < 2 && remaining > 0; pass++) {
                for (int h = 0; h < snapshots.size() && remaining > 0; h++) {
                    FluidCapacitySnapshot snapshot = snapshots.get(h);
                    for (int tank = 0; tank < snapshot.room.length && remaining > 0; tank++) {
                        FluidIdentifier contents = snapshot.contents[tank];
                        if (pass == 0 ? !fluid.getFluid().equals(contents) : contents != null) continue;
                        int used = Math.min(remaining, Math.min(snapshot.room[tank], accepted.get(h)));
                        if (used <= 0) continue;
                        snapshot.contents[tank] = fluid.getFluid();
                        snapshot.room[tank] -= used;
                        accepted.set(h, accepted.get(h) - used);
                        remaining -= used;
                    }
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }

    private static final class FluidCapacitySnapshot {

        private final IFluidHandler handler;
        private final ForgeDirection side;
        private final FluidIdentifier[] contents;
        private final int[] room;

        private FluidCapacitySnapshot(IFluidHandler handler, ForgeDirection side) {
            this.handler = handler;
            this.side = side;
            FluidTankInfo[] tanks = handler.getTankInfo(side);
            int size = tanks == null ? 0 : tanks.length;
            contents = new FluidIdentifier[size];
            room = new int[size];
            for (int i = 0; i < size; i++) {
                if (tanks[i] == null) {
                    continue;
                }
                FluidStack held = tanks[i].fluid;
                boolean empty = held == null || held.amount <= 0;
                contents[i] = empty ? null : FluidIdentifier.get(held);
                room[i] = Math.max(0, tanks[i].capacity - (empty ? 0 : held.amount));
            }
        }
    }

    /**
     * Copies the fluids multiplied by {@code sets}, with amounts of the same fluid added together.
     */
    private static List<PatternFluidStack> mergeFluids(List<PatternFluidStack> fluids, int sets) {
        List<PatternFluidStack> merged = new ArrayList<>();
        for (PatternFluidStack fluid : fluids) {
            long amount = (long) fluid.getAmount() * sets;
            PatternFluidStack existing = null;
            for (PatternFluidStack candidate : merged) {
                if (candidate.getFluid().equals(fluid.getFluid())) {
                    existing = candidate;
                    break;
                }
            }
            if (existing == null) {
                merged.add(new PatternFluidStack(fluid.getFluid(), (int) Math.min(Integer.MAX_VALUE, amount)));
            } else {
                existing.addAmount((int) Math.min(Integer.MAX_VALUE - existing.getAmount(), amount));
            }
        }
        return merged;
    }

    /**
     * The target inventory as seen from the insertion side, so capacity checks see the same slots the insert uses.
     */
    IInventory getInsertableInventory(AdjacentTile connected) {
        IInventory inventory = (IInventory) connected.tile;
        if (inventory instanceof net.minecraft.inventory.ISidedInventory) {
            return new SidedInventoryMinecraftAdapter(
                    (net.minecraft.inventory.ISidedInventory) inventory,
                    module.getInsertionOrientation(connected),
                    false);
        }
        return inventory;
    }

    static boolean canFitPatternSetsDisregardingSlots(IInventory inventory, List<ItemIdentifierStack> ingredients,
            int sets) {
        ItemStack[] snapshot = new ItemStack[inventory.getSizeInventory()];
        for (int i = 0; i < snapshot.length; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            snapshot[i] = existing == null ? null : existing.copy();
        }
        for (ItemIdentifierStack ingredient : ingredients) {
            ItemStack stack = ingredient.makeNormalStack();
            long amount = (long) ingredient.getStackSize() * sets;
            if (amount <= 0 || amount > Integer.MAX_VALUE) {
                return false;
            }
            stack.stackSize = (int) amount;
            if (!insertIntoSnapshot(inventory, snapshot, stack)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simulates inserting one stack into an inventory snapshot without changing the real adjacent inventory.
     * <p>
     * Existing compatible stacks are filled first, then empty slots are populated. The caller uses the result to decide
     * how many complete pattern sets can be routed before any real items are requested.
     */
    private static boolean insertIntoSnapshot(IInventory inventory, ItemStack[] snapshot, ItemStack stack) {
        ItemIdentifier stackIdentifier = ItemIdentifier.get(stack);
        if (stackIdentifier == null) {
            return false;
        }
        int remaining = stack.stackSize;
        for (int i = 0; i < snapshot.length && remaining > 0; i++) {
            ItemStack existing = snapshot[i];
            if (existing == null) {
                continue;
            }
            ItemIdentifier existingIdentifier = ItemIdentifier.get(existing);
            if (existingIdentifier == null || !existingIdentifier.equals(stackIdentifier)
                    || !inventory.isItemValidForSlot(i, stack)) {
                continue;
            }
            int room = Math.min(inventory.getInventoryStackLimit(), existing.getMaxStackSize()) - existing.stackSize;
            if (room <= 0) {
                continue;
            }
            int moved = Math.min(room, remaining);
            existing.stackSize += moved;
            remaining -= moved;
        }
        for (int i = 0; i < snapshot.length && remaining > 0; i++) {
            if (snapshot[i] != null || !inventory.isItemValidForSlot(i, stack)) {
                continue;
            }
            int moved = Math.min(remaining, Math.min(inventory.getInventoryStackLimit(), stack.getMaxStackSize()));
            ItemStack inserted = stack.copy();
            inserted.stackSize = moved;
            snapshot[i] = inserted;
            remaining -= moved;
        }
        return remaining <= 0;
    }

    private int insert(ItemIdentifierStack item) {
        AdjacentTile connected = getConnected();
        if (connected == null || item.getStackSize() <= 0) {
            module.debug("adjacent item insert skipped connected=%s item=%s", connected, item);
            return 0;
        }
        int amount = item.getStackSize();
        if (amount <= 0) {
            module.debug("adjacent item insert skipped after clamp item=%s", item);
            return 0;
        }
        ItemStack toInsert = item.makeNormalStack();
        toInsert.stackSize = amount;
        if (connected.tile instanceof PatternLogisticsCraftingTableTileEntity) {
            int inserted = ((PatternLogisticsCraftingTableTileEntity) connected.tile).insertFromPatternPipe(toInsert);
            if (inserted > 0) {
                invalidateContentCache();
            }
            module.debug(
                    "adjacent item inserted into pattern table item=%s amount=%d inserted=%d",
                    item.getItem(),
                    amount,
                    inserted);
            return inserted;
        }
        ForgeDirection side = module.getInsertionOrientation(connected);
        ITransactor transactor = InventoryHelper.getTransactorFor(connected.tile, side);
        if (transactor == null) {
            module.debug("adjacent item insert failed: no transactor tile=%s item=%s", connected.tile, item);
            return 0;
        }
        ItemStack added = transactor.add(toInsert, side, true);
        int inserted = added != null ? added.stackSize : 0;
        if (inserted > 0) {
            invalidateContentCache();
        }
        module.debug(
                "adjacent item inserted tile=%s item=%s amount=%d inserted=%d",
                connected.tile,
                item.getItem(),
                amount,
                inserted);
        return inserted;
    }

    private int insertFluid(PatternFluidStack fluid) {
        AdjacentTile connected = getConnected();
        if (connected == null || !(connected.tile instanceof IFluidHandler handler) || fluid.getAmount() <= 0) {
            module.debug("adjacent fluid insert skipped connected=%s fluid=%s", connected, fluid);
            return 0;
        }
        int inserted = handler.fill(getFluidInsertionOrientation(connected), fluid.makeFluidStack(), true);
        if (inserted > 0) {
            invalidateContentCache();
        }
        module.debug("adjacent fluid inserted tile=%s fluid=%s inserted=%d", connected.tile, fluid, inserted);
        return inserted;
    }

    ForgeDirection getFluidInsertionOrientation(AdjacentTile connected) {
        return module.getInsertionOrientation(connected);
    }

    boolean isEmpty(AdjacentTile connected) {
        if (connected == null
                || (!(connected.tile instanceof IInventory) && !(connected.tile instanceof IFluidHandler))) {
            return true;
        }
        refreshContentCache(connected);
        if (emptyCached) {
            return cachedEmpty;
        }
        cachedEmpty = calculateEmpty(connected);
        emptyCached = true;
        return cachedEmpty;
    }

    private boolean calculateEmpty(AdjacentTile connected) {
        if (connected.tile instanceof PatternLogisticsCraftingTableTileEntity) {
            return ((PatternLogisticsCraftingTableTileEntity) connected.tile).isIdle();
        }
        if (connected.tile instanceof IInventory inventory) {
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack != null && stack.stackSize > 0) {
                    return false;
                }
            }
        }
        if (connected.tile instanceof IFluidHandler) {
            FluidTankInfo[] tanks = ((IFluidHandler) connected.tile)
                    .getTankInfo(getFluidInsertionOrientation(connected));
            if (tanks != null) {
                for (FluidTankInfo tank : tanks) {
                    if (tank != null && tank.fluid != null && tank.fluid.amount > 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    ItemStack extract(IResource wanted, int count) {
        return extract(getConnected(), wanted, count);
    }

    ItemStack extract(AdjacentTile tile, IResource wanted, int count) {
        if (tile == null) return null;

        if (tile.tile instanceof PatternLogisticsCraftingTableTileEntity) {
            if (!pipe.useEnergy(Math.min(count, wanted.getRequestedAmount()))) {
                module.debug(
                        "adjacent extract item failed: no energy for pattern table wanted=%s count=%d",
                        wanted,
                        count);
                return null;
            }
            ItemStack extracted = ((PatternLogisticsCraftingTableTileEntity) tile.tile).extractOutput(wanted, count);
            if (extracted != null && extracted.stackSize > 0) {
                invalidateContentCache();
            }
            module.debug(
                    "adjacent extracted from pattern table wanted=%s count=%d extracted=%s",
                    wanted,
                    count,
                    extracted);
            return extracted;
        }
        if (!(tile.tile instanceof IInventory inventory)) return null;
        if (inventory instanceof net.minecraft.inventory.ISidedInventory) {
            inventory = new SidedInventoryMinecraftAdapter(
                    (net.minecraft.inventory.ISidedInventory) inventory,
                    tile.orientation.getOpposite(),
                    true);
        }
        IInventoryUtil util = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inventory, tile.orientation);
        ItemIdentifier item = wanted.getAsItem();
        int available = util.itemCount(item);
        if (available <= 0 || !pipe.useEnergy(Math.min(count, available))) {
            module.debug("adjacent extract item failed item=%s available=%d count=%d", item, available, count);
            return null;
        }
        ItemStack extracted = util.getMultipleItems(item, Math.min(count, available));
        if (extracted != null && extracted.stackSize > 0) {
            invalidateContentCache();
        }
        module.debug(
                "adjacent extracted item=%s available=%d count=%d extracted=%s",
                item,
                available,
                count,
                extracted);
        return extracted;
    }

    FluidStack extractFluid(AdjacentTile tile, PatternFluidStack wanted, int amount) {
        if (!(tile.tile instanceof IFluidHandler handler) || wanted == null || amount <= 0) {
            module.debug("adjacent extract fluid skipped tile=%s wanted=%s amount=%d", tile.tile, wanted, amount);
            return null;
        }
        ForgeDirection side = tile.orientation.getOpposite();
        FluidStack simulated = handler.drain(side, amount, false);
        if (simulated == null || simulated.amount <= 0
                || !wanted.getFluid().equals(logisticspipes.utils.FluidIdentifier.get(simulated))) {
            module.debug(
                    "adjacent extract fluid simulation failed wanted=%s amount=%d simulated=%s",
                    wanted,
                    amount,
                    simulated);
            return null;
        }
        if (!pipe.useEnergy(Math.min(amount, simulated.amount))) {
            module.debug(
                    "adjacent extract fluid failed: no energy wanted=%s amount=%d simulated=%d",
                    wanted,
                    amount,
                    simulated.amount);
            return null;
        }
        FluidStack drained = handler.drain(side, Math.min(amount, simulated.amount), true);
        if (drained != null && drained.amount > 0) {
            invalidateContentCache();
        }
        module.debug("adjacent extracted fluid wanted=%s amount=%d drained=%s", wanted, amount, drained);
        return drained;
    }

    private void refreshContentCache(AdjacentTile connected) {
        long tick = module.currentWorldTick();
        net.minecraft.tileentity.TileEntity tile = connected == null ? null : connected.tile;
        ForgeDirection orientation = connected == null ? ForgeDirection.UNKNOWN : connected.orientation;
        if (contentCacheTick == tick && contentCacheTile == tile && contentCacheOrientation == orientation) {
            return;
        }
        contentCacheTick = tick;
        contentCacheTile = tile;
        contentCacheOrientation = orientation;
        clearContentCacheValues();
    }

    private void invalidateContentCache() {
        clearContentCacheValues();
    }

    private void clearContentCacheValues() {
        emptyCached = false;
    }

}
