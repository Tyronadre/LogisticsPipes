package logisticspipes.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;

import logisticspipes.LogisticsPipes;
import logisticspipes.crafting.patternStack.PatternItemStack;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.PipeItemsPatternCraftingLogistics;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree;
import logisticspipes.routing.IRouter;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.AdjacentTile;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.InventoryHelper;
import logisticspipes.utils.SidedInventoryMinecraftAdapter;
import logisticspipes.utils.WorldUtil;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.transactor.ITransactor;
import lombok.Getter;

public class PipeItemsPatternSatelliteLogistics extends PipeItemsSatelliteLogistics
        implements IPatternSatellitePipe, PatternByproductExtractionTarget {

    private static final Set<PipeItemsPatternSatelliteLogistics> ALL_PATTERN_SATELLITES = Collections
            .newSetFromMap(new WeakHashMap<>());
    private static final String UUID_TAG = "patternSatelliteUuid";
    private static final String NAME_TAG = "patternSatelliteName";
    private static final int CANCELLED_ARRIVAL_TIMEOUT = 640;
    private static final int COMPLETED_DELIVERY_LIMIT = 4096;

    @Getter
    private String satelliteUuid = UUID.randomUUID().toString();
    private String satelliteName = "";
    private final List<PendingCancelledArrival> pendingCancelledArrivals = new ArrayList<>();
    private final Map<ItemIdentifier, Integer> reservationBaseline = new HashMap<>();
    private final Map<ItemIdentifier, Integer> reservationExpected = new HashMap<>();
    private final Map<PatternCraftingReference, ItemIdentifierStack> expectedDeliveries = new LinkedHashMap<>();
    private final Map<PatternCraftingReference, Integer> lostDeliveries = new HashMap<>();
    private final PatternSatelliteByproductExtractor byproductExtractor = new PatternSatelliteByproductExtractor(this);
    private UUID reservedOwnerRouter;
    private PatternCraftingReference reservedReference;

    public PipeItemsPatternSatelliteLogistics(Item item) {
        super(item);
    }

    public static void cleanup() {
        ALL_PATTERN_SATELLITES.clear();
    }

    public static PipeItemsPatternSatelliteLogistics findById(int satelliteId) {
        return findById(satelliteId, null);
    }

    public static PipeItemsPatternSatelliteLogistics findById(int satelliteId, IRouter requester) {
        if (satelliteId <= 0) {
            return null;
        }
        for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
            if (satellite != null && satellite.satelliteId == satelliteId
                    && (requester == null || satellite.canExtractByproductsFor(requester))) {
                return satellite;
            }
        }
        return null;
    }

    public static PipeItemsPatternSatelliteLogistics findByUuid(String satelliteUuid) {
        if (satelliteUuid == null || satelliteUuid.isEmpty()) {
            return null;
        }
        for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
            if (satellite != null && satelliteUuid.equals(satellite.satelliteUuid)) {
                return satellite;
            }
        }
        return null;
    }

    static List<PatternByproductExtractionTarget> getRegisteredByproductExtractionTargets() {
        return new ArrayList<>(ALL_PATTERN_SATELLITES);
    }

    public static List<Integer> getKnownSatelliteIds() {
        TreeSet<Integer> ids = new TreeSet<>();
        for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
            if (satellite != null && satellite.satelliteId > 0) {
                ids.add(satellite.satelliteId);
            }
        }
        return new ArrayList<>(ids);
    }

    public static List<PatternSatelliteInfo> getKnownSatellitesFor(EntityPlayer player) {
        Set<Integer> favoriteIds = getFavoriteSatelliteIds(player);
        Set<String> favoriteUuids = getFavoriteSatelliteUuids(player);
        List<PatternSatelliteInfo> satellites = new ArrayList<>();
        int playerDimension = player != null && player.worldObj != null
                ? MainProxy.getDimensionForWorld(player.worldObj)
                : Integer.MIN_VALUE;
        for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
            if (!isSelectableSatellite(satellite)) {
                continue;
            }
            int dimension = MainProxy.getDimensionForWorld(satellite.getWorld());
            satellites.add(
                    new PatternSatelliteInfo(
                            satellite.satelliteId,
                            satellite.getX(),
                            satellite.getY(),
                            satellite.getZ(),
                            dimension,
                            getDistance(player, playerDimension, satellite, dimension),
                            favoriteIds.contains(satellite.satelliteId)
                                    || favoriteUuids.contains(satellite.satelliteUuid),
                            satellite.satelliteUuid,
                            satellite.getDisplayName()));
        }
        satellites.sort((left, right) -> {
            if (left.favorite() != right.favorite()) {
                return left.favorite() ? -1 : 1;
            }
            boolean leftSameDimension = left.distance() >= 0;
            boolean rightSameDimension = right.distance() >= 0;
            if (leftSameDimension != rightSameDimension) {
                return leftSameDimension ? -1 : 1;
            }
            if (leftSameDimension && left.distance() != right.distance()) {
                return Integer.compare(left.distance(), right.distance());
            }
            return Integer.compare(left.id(), right.id());
        });
        return satellites;
    }

    private static boolean isSelectableSatellite(PipeItemsPatternSatelliteLogistics satellite) {
        return satellite != null && satellite.satelliteId > 0
                && satellite.container != null
                && !satellite.container.isInvalid()
                && satellite.getWorld() != null;
    }

    private static Set<Integer> getFavoriteSatelliteIds(EntityPlayer player) {
        Set<Integer> favoriteIds = new HashSet<>();
        if (player == null || player.inventory == null) {
            return favoriteIds;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            for (int id : ItemMemoryChip.getPatternSatelliteIds(stack)) {
                if (id > 0) {
                    favoriteIds.add(id);
                }
            }
        }
        return favoriteIds;
    }

    private static Set<String> getFavoriteSatelliteUuids(EntityPlayer player) {
        Set<String> favoriteUuids = new HashSet<>();
        if (player == null || player.inventory == null) {
            return favoriteUuids;
        }
        for (ItemStack stack : player.inventory.mainInventory) {
            for (ItemMemoryChip.StoredPatternSatellite satellite : ItemMemoryChip.getPatternSatellites(stack)) {
                if (!satellite.uuid().isEmpty()) {
                    favoriteUuids.add(satellite.uuid());
                }
            }
        }
        return favoriteUuids;
    }

    private static int getDistance(EntityPlayer player, int playerDimension,
            PipeItemsPatternSatelliteLogistics satellite, int satelliteDimension) {
        if (player == null || playerDimension != satelliteDimension) {
            return -1;
        }
        double dx = satellite.getX() + 0.5D - player.posX;
        double dy = satellite.getY() + 0.5D - player.posY;
        double dz = satellite.getZ() + 0.5D - player.posZ;
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public String getDisplayName() {
        return satelliteName == null || satelliteName.trim().isEmpty() ? Integer.toString(satelliteId)
                : satelliteName.trim();
    }

    @Override
    public String getSatelliteName() {
        return satelliteName == null ? "" : satelliteName.trim();
    }

    @Override
    public boolean canExtractByproductsFor(IRouter requester) {
        return byproductExtractor.canExtractFor(requester);
    }

    @Override
    public PatternByproductExtractionResult extractItemByproduct(ItemIdentifier item, int amount, int destination,
            IAdditionalTargetInformation info) {
        return byproductExtractor.extractItem(item, amount, destination, info);
    }

    @Override
    public PatternByproductExtractionResult extractFluidByproduct(FluidIdentifier fluid, int amount, int destination,
            IAdditionalTargetInformation info) {
        return byproductExtractor.extractFluid(fluid, amount, destination, info);
    }

    /**
     * Keeps routed pattern inputs on the same adjacent inventory that direct satellite dispatch would use.
     */
    @Override
    public boolean isLockedExit(ForgeDirection orientation) {
        if (reservedOwnerRouter == null) {
            return super.isLockedExit(orientation);
        }
        AdjacentTile target = getPatternTargetInventory();
        return (target != null && target.orientation != orientation) || super.isLockedExit(orientation);
    }

    @Override
    public void enabledUpdateEntity() {
        super.enabledUpdateEntity();
        if (!MainProxy.isClient(getWorld()) && isNthTick(40)) {
            ensureAllSatelliteStatus();
        }
    }

    @Override
    public void setSatelliteName(String satelliteName) {
        this.satelliteName = satelliteName == null ? "" : satelliteName.trim();
        ensureAllSatelliteStatus();
        if (container != null) {
            container.markDirty();
            container.sendUpdateToClient();
        }
    }

    /**
     * Returns true when this satellite is unlocked or only pre-reserved by the same crafting pipe.
     */
    public boolean canReserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference) {
        UUID ownerRouter = ownerRouterId(owner);
        return ownerRouter != null && reference != null
                && (reservedOwnerRouter == null
                        || (reservedOwnerRouter.equals(ownerRouter) && reference.equals(reservedReference)
                                && !hasActivePatternInputReservation()));
    }

    /**
     * Locks this satellite for a pattern crafting pipe before a complete buffered set is dispatched.
     */
    public boolean reserveFor(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference) {
        if (!canReserveFor(owner, reference)) {
            return false;
        }
        reservedOwnerRouter = ownerRouterId(owner);
        reservedReference = reference;
        markReservationDirty();
        return true;
    }

    /**
     * Releases the reservation held by the owner pipe.
     */
    public void releaseReservation(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference) {
        UUID ownerRouter = ownerRouterId(owner);
        if (ownerRouter != null && (!ownerRouter.equals(reservedOwnerRouter)
                || !java.util.Objects.equals(reservedReference, reference))) {
            return;
        }
        reservedOwnerRouter = null;
        reservedReference = null;
        reservationBaseline.clear();
        reservationExpected.clear();
        markReservationDirty();
    }

    /**
     * Returns true once all items inserted during the current reservation have been consumed.
     */
    public boolean isReservationConsumed(PipeItemsPatternCraftingLogistics owner, PatternCraftingReference reference) {
        UUID ownerRouter = ownerRouterId(owner);
        if (ownerRouter == null || !ownerRouter.equals(reservedOwnerRouter)
                || !java.util.Objects.equals(reservedReference, reference)) {
            return true;
        }
        for (int expected : reservationExpected.values()) {
            if (expected > 0) {
                return false;
            }
        }
        for (Map.Entry<ItemIdentifier, Integer> entry : reservationBaseline.entrySet()) {
            if (countAdjacentItem(entry.getKey()) > entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActivePatternInputReservation() {
        if (!reservationBaseline.isEmpty()) {
            return true;
        }
        for (int expected : reservationExpected.values()) {
            if (expected > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the adjacent satellite inventory can accept the complete stack.
     */
    public boolean canAcceptPatternInput(ItemIdentifierStack stack) {
        return stack != null && stack.getStackSize() > 0 && roomForPatternInput(stack) >= stack.getStackSize();
    }

    /** Checks the space shared by every item in one batch without inserting anything. */
    public boolean canAcceptPatternInputs(List<ItemIdentifierStack> stacks) {
        AdjacentTile target = getPatternTargetInventory();
        IInventory inventory = target == null ? null : getInsertableInventory(target);
        if (inventory == null || stacks.isEmpty()) {
            return false;
        }
        for (ItemIdentifierStack stack : stacks) {
            if (!canAcceptPatternInput(stack)) {
                return false;
            }
        }
        return AdjacentInventoryHandler.canFitPatternSetsDisregardingSlots(inventory, stacks, 1);
    }

    /**
     * Returns whether the adjacent target inventory is ready for a blocking-mode satellite batch.
     */
    public boolean isPatternTargetEmpty() {
        AdjacentTile target = getPatternTargetInventory();
        if (target == null) {
            return false;
        }
        if (target.tile instanceof PatternLogisticsCraftingTableTileEntity table) {
            return table.isIdle();
        }
        IInventory inventory = getInsertableInventory(target);
        if (inventory == null) {
            return false;
        }
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack != null && stack.stackSize > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Records a routed pattern input that has been sent to this satellite but has not reached the pipe yet.
     */
    public void expectPatternInput(ItemIdentifierStack stack, PatternCraftingReference delivery,
            boolean trackReservation) {
        if (stack == null || stack.getStackSize() <= 0) {
            return;
        }
        expectedDeliveries.put(delivery, stack.clone());
        trimCompletedDeliveries();
        if (trackReservation) {
            reservationBaseline.putIfAbsent(stack.getItem(), countAdjacentItem(stack.getItem()));
            reservationExpected.merge(stack.getItem(), stack.getStackSize(), Integer::sum);
        }
        markReservationDirty();
    }

    @Override
    public void itemLost(ItemIdentifierStack item, IAdditionalTargetInformation info) {
        if (item == null || item.getStackSize() <= 0) return;
        if (!(info instanceof PatternTargetInformation target) || !target.isTracked()) {
            super.itemLost(item, info);
            return;
        }
        ItemIdentifierStack expected = expectedDeliveries.get(target.deliveryReference());
        if (expected == null || !expected.getItem().equals(item.getItem()) || expected.getStackSize() <= 0) {
            return;
        }
        int lost = lostDeliveries.getOrDefault(target.deliveryReference(), 0);
        lostDeliveries.put(
                target.deliveryReference(),
                (int) Math.min(expected.getStackSize(), (long) lost + item.getStackSize()));
        markReservationDirty();
    }

    @Override
    public void throttledUpdateEntity() {
        super.throttledUpdateEntity();
        Iterator<Map.Entry<PatternCraftingReference, Integer>> iterator = lostDeliveries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PatternCraftingReference, Integer> entry = iterator.next();
            ItemIdentifierStack expected = expectedDeliveries.get(entry.getKey());
            int amount = expected == null ? 0 : Math.min(entry.getValue(), expected.getStackSize());
            PatternCraftingReference batch = entry.getKey().parent();
            if (amount <= 0 || batch == null || PatternCraftingInstanceRegistry.isCancelled(batch)) {
                iterator.remove();
                markReservationDirty();
                continue;
            }
            PatternTargetInformation target = new PatternTargetInformation(
                    PatternTargetInformation.NO_PATTERN_SLOT,
                    PatternTargetInformation.NO_INPUT_SLOT,
                    batch,
                    entry.getKey());
            int received = RequestTree.requestPartial(expected.getItem().makeStack(amount), this, target);
            if (received >= amount) iterator.remove();
            else entry.setValue(amount - received);
            if (received > 0) markReservationDirty();
        }
    }

    private void trimCompletedDeliveries() {
        if (expectedDeliveries.size() <= COMPLETED_DELIVERY_LIMIT) return;
        Iterator<Map.Entry<PatternCraftingReference, ItemIdentifierStack>> iterator = expectedDeliveries.entrySet()
                .iterator();
        while (expectedDeliveries.size() > COMPLETED_DELIVERY_LIMIT && iterator.hasNext()) {
            if (iterator.next().getValue().getStackSize() == 0) iterator.remove();
        }
    }

    /**
     * Inserts a complete pattern input stack into the satellite's adjacent inventory.
     */
    public int insertPatternInput(ItemIdentifierStack stack) {
        return insertPatternInput(stack, true);
    }

    /**
     * Inserts a complete pattern input stack and optionally tracks it as a blocking-mode reservation.
     */
    public int insertPatternInput(ItemIdentifierStack stack, boolean trackReservation) {
        AdjacentTile target = getPatternTargetInventory();
        if (stack == null || stack.getStackSize() <= 0 || target == null) {
            return 0;
        }
        int before = countAdjacentItem(stack.getItem());
        ITransactor transactor = InventoryHelper.getTransactorFor(target.tile, target.orientation.getOpposite());
        if (transactor == null) {
            return 0;
        }
        ItemStack inserted = transactor.add(stack.makeNormalStack(), target.orientation.getOpposite(), true);
        int amount = inserted == null ? 0 : inserted.stackSize;
        if (amount > 0 && trackReservation) {
            reservationBaseline.putIfAbsent(stack.getItem(), before);
            markReservationDirty();
        }
        return amount;
    }

    /**
     * Retrieves cancelled craft ingredients from the adjacent inventory and optionally catches still-traveling items.
     *
     * @param stack            item and amount that belonged to the cancelled craft
     * @param interceptMissing whether missing amounts should be intercepted if they arrive shortly after cancellation
     * @return amount that was already extracted from adjacent inventories
     */
    public int retrieveOrCancelToStorage(ItemIdentifierStack stack, boolean interceptMissing,
            PatternCraftingReference deliveryReference) {
        if (stack == null || stack.getStackSize() <= 0 || MainProxy.isClient(getWorld())) {
            return 0;
        }
        ItemIdentifierStack expected = expectedDeliveries.get(deliveryReference);
        if (expected != null) expected.setStackSize(0);
        lostDeliveries.remove(deliveryReference);
        markReservationDirty();
        int extracted = retrieveLandedItemsToStorage(stack);
        int missing = stack.getStackSize() - extracted;
        if (interceptMissing && missing > 0) {
            addPendingCancelledArrival(stack.getItem(), missing, deliveryReference);
        }
        return extracted;
    }

    /**
     * Intercepts cancelled deliveries before the transport layer inserts them into the satellite inventory.
     */
    @Override
    public void itemArrived(ItemIdentifierStack item, IAdditionalTargetInformation info) {
        super.itemArrived(item, info);
        if (item == null || item.getStackSize() <= 0 || MainProxy.isClient(getWorld())) {
            return;
        }
        purgeExpiredCancelledArrivals();
        int cancelled = removePendingCancelledArrival(item, info);
        if (cancelled > 0) {
            ItemIdentifierStack rerouted = new ItemIdentifierStack(item.getItem(), cancelled);
            queueToStorage(rerouted.makeNormalStack(), getPointedOrientation());
            item.lowerStackSize(cancelled);
        }
        if (info instanceof PatternTargetInformation target && target.isTracked()) {
            ItemIdentifierStack expected = expectedDeliveries.get(target.deliveryReference());
            if (PatternCraftingInstanceRegistry.isCancelled(target.orderReference())) {
                if (expected != null) expected.setStackSize(0);
                lostDeliveries.remove(target.deliveryReference());
                queueToStorage(item.makeNormalStack(), getPointedOrientation());
                item.setStackSize(0);
                markReservationDirty();
                return;
            }
            if (expected != null) {
                int accepted = expected.getItem().equals(item.getItem())
                        ? Math.min(item.getStackSize(), expected.getStackSize())
                        : 0;
                int excess = item.getStackSize() - accepted;
                if (excess > 0)
                    queueToStorage(item.getItem().makeStack(excess).makeNormalStack(), getPointedOrientation());
                item.setStackSize(accepted);
                expected.lowerStackSize(accepted);
                markReservationDirty();
            }
            if (java.util.Objects.equals(reservedReference, target.orderReference())) markExpectedInputArrived(item);
        } else {
            markExpectedInputArrived(item);
        }
    }

    private void markExpectedInputArrived(ItemIdentifierStack item) {
        if (item == null || item.getStackSize() <= 0) {
            return;
        }
        int expected = reservationExpected.getOrDefault(item.getItem(), 0);
        if (expected <= 0) {
            return;
        }
        int remainingExpected = expected - Math.min(expected, item.getStackSize());
        if (remainingExpected > 0) {
            reservationExpected.put(item.getItem(), remainingExpected);
        } else {
            reservationExpected.remove(item.getItem());
        }
        markReservationDirty();
    }

    private int retrieveLandedItemsToStorage(ItemIdentifierStack stack) {
        int remaining = stack.getStackSize();
        int extracted = 0;
        WorldUtil worldUtil = new WorldUtil(getWorld(), getX(), getY(), getZ());
        for (AdjacentTile tile : worldUtil.getAdjacentTileEntities(true)) {
            if (remaining <= 0) {
                break;
            }
            if (!(tile.tile instanceof IInventory base)
                    || SimpleServiceLocator.pipeInformationManager.isItemPipe(tile.tile)) {
                continue;
            }
            if (base instanceof ISidedInventory) {
                base = new SidedInventoryMinecraftAdapter((ISidedInventory) base, tile.orientation.getOpposite(), true);
            }
            IInventoryUtil inventory = SimpleServiceLocator.inventoryUtilFactory
                    .getInventoryUtil(base, tile.orientation.getOpposite());
            ItemStack removed = inventory.getMultipleItems(stack.getItem(), remaining);
            if (removed == null || removed.stackSize <= 0) {
                continue;
            }
            remaining -= removed.stackSize;
            extracted += removed.stackSize;
            queueToStorage(removed, tile.orientation);
        }
        return extracted;
    }

    private int roomForPatternInput(ItemIdentifierStack stack) {
        AdjacentTile target = getPatternTargetInventory();
        if (target == null) {
            return 0;
        }
        IInventory inventory = getInsertableInventory(target);
        if (inventory == null) {
            return 0;
        }
        IInventoryUtil inventoryUtil = SimpleServiceLocator.inventoryUtilFactory
                .getInventoryUtil(inventory, target.orientation.getOpposite());
        return inventoryUtil.roomForItem(stack.getItem(), stack.getStackSize());
    }

    private int countAdjacentItem(ItemIdentifier item) {
        if (item == null) {
            return 0;
        }
        AdjacentTile target = getPatternTargetInventory();
        if (target == null) {
            return 0;
        }
        IInventory inventory = getInsertableInventory(target);
        if (inventory == null) {
            return 0;
        }
        IInventoryUtil inventoryUtil = SimpleServiceLocator.inventoryUtilFactory
                .getInventoryUtil(inventory, target.orientation.getOpposite());
        return inventoryUtil.itemCount(item);
    }

    private AdjacentTile getPatternTargetInventory() {
        WorldUtil worldUtil = new WorldUtil(getWorld(), getX(), getY(), getZ());
        ForgeDirection pointed = getPointedOrientation();
        AdjacentTile fallback = null;
        for (AdjacentTile tile : worldUtil.getAdjacentTileEntities(true)) {
            if (!(tile.tile instanceof IInventory)
                    || SimpleServiceLocator.pipeInformationManager.isItemPipe(tile.tile)) {
                continue;
            }
            if (tile.orientation == pointed) {
                return tile;
            }
            if (fallback == null) {
                fallback = tile;
            }
        }
        return fallback;
    }

    private IInventory getInsertableInventory(AdjacentTile target) {
        if (!(target.tile instanceof IInventory inventory)) {
            return null;
        }
        if (inventory instanceof ISidedInventory) {
            return new SidedInventoryMinecraftAdapter(
                    (ISidedInventory) inventory,
                    target.orientation.getOpposite(),
                    false);
        }
        return inventory;
    }

    private UUID ownerRouterId(PipeItemsPatternCraftingLogistics owner) {
        return owner == null || owner.getRouter() == null ? null : owner.getRouter().getId();
    }

    private void markReservationDirty() {
        if (container != null) container.markDirty();
    }

    private void queueToStorage(ItemStack stack, ForgeDirection from) {
        if (stack == null || stack.stackSize <= 0) {
            return;
        }
        ForgeDirection safeFrom = from == null ? ForgeDirection.UNKNOWN : from;
        IRoutedItem routedItem = SimpleServiceLocator.routedItemHelper.createNewTravelItem(stack);
        routedItem.setDestination(-1);
        routedItem.setTransportMode(TransportMode.Active);
        queueRoutedItem(routedItem, safeFrom);
    }

    private void addPendingCancelledArrival(ItemIdentifier item, int amount,
            PatternCraftingReference deliveryReference) {
        if (item == null || amount <= 0) {
            return;
        }
        long expires = getWorld() == null ? 0 : getWorld().getTotalWorldTime() + CANCELLED_ARRIVAL_TIMEOUT;
        for (PendingCancelledArrival pending : pendingCancelledArrivals) {
            if (pending.matches(item, deliveryReference)) {
                pending.amount += amount;
                pending.expires = Math.max(pending.expires, expires);
                markReservationDirty();
                return;
            }
        }
        pendingCancelledArrivals.add(new PendingCancelledArrival(item, amount, expires, deliveryReference));
        markReservationDirty();
    }

    private int removePendingCancelledArrival(ItemIdentifierStack arriving, IAdditionalTargetInformation info) {
        int matched = 0;
        int available = arriving.getStackSize();
        Iterator<PendingCancelledArrival> iterator = pendingCancelledArrivals.iterator();
        while (iterator.hasNext() && matched < available) {
            PendingCancelledArrival pending = iterator.next();
            if (!pending.matches(arriving, info)) {
                continue;
            }
            int used = Math.min(pending.amount, available - matched);
            pending.amount -= used;
            matched += used;
            if (pending.amount <= 0) {
                iterator.remove();
            }
        }
        if (matched > 0) markReservationDirty();
        return matched;
    }

    private void purgeExpiredCancelledArrivals() {
        if (getWorld() == null) {
            return;
        }
        long now = getWorld().getTotalWorldTime();
        Iterator<PendingCancelledArrival> iterator = pendingCancelledArrivals.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expires <= now) {
                iterator.remove();
                markReservationDirty();
            }
        }
    }

    @Override
    protected int findId(int increment) {
        if (MainProxy.isClient(getWorld())) {
            return satelliteId;
        }
        int potentialId = satelliteId;
        boolean conflict = true;
        while (conflict) {
            potentialId += increment;
            if (potentialId < 0) {
                return 0;
            }
            conflict = false;
            for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
                if (satellite != this && satellite.satelliteId == potentialId) {
                    conflict = true;
                    break;
                }
            }
        }
        return potentialId;
    }

    @Override
    protected void ensureAllSatelliteStatus() {
        if (MainProxy.isClient()) {
            return;
        }
        if (satelliteId == 0) {
            satelliteId = findId(1);
        }
        if (satelliteId == 0) {
            ALL_PATTERN_SATELLITES.remove(this);
        } else {
            ALL_PATTERN_SATELLITES.add(this);
            ensureUniqueDisplayNameInNetwork();
        }
    }

    private void ensureUniqueDisplayNameInNetwork() {
        String displayName = getDisplayName();
        if (displayName.isEmpty()) {
            return;
        }
        int suffix = 2;
        String baseName = displayName;
        while (hasDisplayNameConflict(displayName)) {
            displayName = baseName + "-" + suffix++;
        }
        if (!displayName.equals(getDisplayName())) {
            satelliteName = displayName;
        }
    }

    private boolean hasDisplayNameConflict(String displayName) {
        for (PipeItemsPatternSatelliteLogistics satellite : ALL_PATTERN_SATELLITES) {
            if (satellite == null || satellite == this || !isSelectableSatellite(satellite)) {
                continue;
            }
            if (displayName.equalsIgnoreCase(satellite.getDisplayName()) && isInSameNetwork(satellite)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInSameNetwork(PipeItemsPatternSatelliteLogistics other) {
        try {
            IRouter router = getRouter();
            IRouter otherRouter = other.getRouter();
            return router == otherRouter
                    || (router != null && otherRouter != null && !router.getDistanceTo(otherRouter).isEmpty());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public boolean handleClick(EntityPlayer player, SecuritySettings settings) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() == LogisticsPipes.LogisticsMemoryChip) {
            if (MainProxy.isServer(getWorld())) {
                if (settings == null || settings.openGui) {
                    ensureAllSatelliteStatus();
                    if (held.hasDisplayName()) {
                        setSatelliteName(held.getDisplayName());
                    }
                    boolean added = ItemMemoryChip.addPatternSatellite(held, this);
                    player.addChatComponentMessage(
                            new ChatComponentText(
                                    (added ? "Stored" : "Already stored") + " pattern satellite "
                                            + getDisplayName()
                                            + " on memory chip"));
                } else {
                    player.addChatComponentMessage(
                            new net.minecraft.util.ChatComponentTranslation("lp.chat.permissiondenied"));
                }
            }
            return true;
        }
        return super.handleClick(player, settings);
    }

    @Override
    public void setSatelliteId(int satelliteId) {
        this.satelliteId = satelliteId;
        ensureAllSatelliteStatus();
    }

    @Override
    public void onWrenchClicked(EntityPlayer entityplayer) {
        // legacy wrenches get the MUI as well; the name field lives there
        openGui(entityplayer, this);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound) {
        super.readFromNBT(nbttagcompound);
        satelliteUuid = nbttagcompound.hasKey(UUID_TAG) ? nbttagcompound.getString(UUID_TAG)
                : UUID.randomUUID().toString();
        satelliteName = nbttagcompound.getString(NAME_TAG);
        reservedReference = PatternCraftingReference.readFromNBT(nbttagcompound, "reservation");
        String owner = nbttagcompound.getString("reservationOwner");
        reservedOwnerRouter = reservedReference == null || owner.isEmpty() ? null : UUID.fromString(owner);
        reservationBaseline.clear();
        reservationExpected.clear();
        NBTTagList inputs = nbttagcompound.getTagList("reservationInputs", 10);
        for (int i = 0; i < inputs.tagCount(); i++) {
            NBTTagCompound entry = inputs.getCompoundTagAt(i);
            ItemIdentifierStack stack = PatternItemStack.readItem(entry);
            if (stack == null || reservedOwnerRouter == null) continue;
            reservationBaseline.put(stack.getItem(), Math.max(0, entry.getInteger("baseline")));
            int expected = entry.getInteger("expected");
            if (expected > 0) reservationExpected.put(stack.getItem(), expected);
        }
        expectedDeliveries.clear();
        lostDeliveries.clear();
        NBTTagList deliveries = nbttagcompound.getTagList("patternDeliveries", 10);
        for (int i = 0; i < deliveries.tagCount(); i++) {
            NBTTagCompound entry = deliveries.getCompoundTagAt(i);
            ItemIdentifierStack stack = PatternItemStack.readItem(entry);
            PatternCraftingReference delivery = PatternCraftingReference.readFromNBT(entry, "delivery");
            if (stack == null || delivery == null) continue;
            stack.setStackSize(Math.max(0, entry.getInteger("remaining")));
            expectedDeliveries.put(delivery, stack);
            int lost = Math.min(stack.getStackSize(), entry.getInteger("lost"));
            if (lost > 0) lostDeliveries.put(delivery, lost);
        }
        pendingCancelledArrivals.clear();
        NBTTagList cancelled = nbttagcompound.getTagList("cancelledPatternArrivals", 10);
        for (int i = 0; i < cancelled.tagCount(); i++) {
            NBTTagCompound entry = cancelled.getCompoundTagAt(i);
            ItemIdentifierStack stack = PatternItemStack.readItem(entry);
            PatternCraftingReference delivery = PatternCraftingReference.readFromNBT(entry, "delivery");
            if (stack != null && stack.getStackSize() > 0 && delivery != null) {
                pendingCancelledArrivals.add(
                        new PendingCancelledArrival(
                                stack.getItem(),
                                stack.getStackSize(),
                                entry.getLong("expires"),
                                delivery));
            }
        }
        ensureAllSatelliteStatus();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
        super.writeToNBT(nbttagcompound);
        nbttagcompound.setString(UUID_TAG, satelliteUuid);
        nbttagcompound.setString(NAME_TAG, satelliteName == null ? "" : satelliteName);
        if (reservedOwnerRouter != null && reservedReference != null) {
            nbttagcompound.setString("reservationOwner", reservedOwnerRouter.toString());
            reservedReference.writeToNBT(nbttagcompound, "reservation");
        } else {
            nbttagcompound.removeTag("reservationOwner");
        }
        NBTTagList inputs = new NBTTagList();
        for (Map.Entry<ItemIdentifier, Integer> baseline : reservationBaseline.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            PatternItemStack.writeItem(entry, baseline.getKey().makeStack(1));
            entry.setInteger("baseline", baseline.getValue());
            entry.setInteger("expected", reservationExpected.getOrDefault(baseline.getKey(), 0));
            inputs.appendTag(entry);
        }
        nbttagcompound.setTag("reservationInputs", inputs);
        NBTTagList deliveries = new NBTTagList();
        for (Map.Entry<PatternCraftingReference, ItemIdentifierStack> delivery : expectedDeliveries.entrySet()) {
            NBTTagCompound entry = new NBTTagCompound();
            PatternItemStack.writeItem(entry, delivery.getValue().getItem().makeStack(1));
            delivery.getKey().writeToNBT(entry, "delivery");
            entry.setInteger("remaining", delivery.getValue().getStackSize());
            entry.setInteger("lost", lostDeliveries.getOrDefault(delivery.getKey(), 0));
            deliveries.appendTag(entry);
        }
        nbttagcompound.setTag("patternDeliveries", deliveries);
        NBTTagList cancelled = new NBTTagList();
        for (PendingCancelledArrival arrival : pendingCancelledArrivals) {
            NBTTagCompound entry = new NBTTagCompound();
            PatternItemStack.writeItem(entry, arrival.item.makeStack(arrival.amount));
            if (arrival.deliveryReference != null) arrival.deliveryReference.writeToNBT(entry, "delivery");
            entry.setLong("expires", arrival.expires);
            cancelled.appendTag(entry);
        }
        nbttagcompound.setTag("cancelledPatternArrivals", cancelled);
    }

    @Override
    public void onAllowedRemoval() {
        if (MainProxy.isClient(getWorld())) {
            return;
        }
        ALL_PATTERN_SATELLITES.remove(this);
    }

    private static class PendingCancelledArrival {

        private final ItemIdentifier item;
        private final PatternCraftingReference deliveryReference;
        private int amount;
        private long expires;

        private PendingCancelledArrival(ItemIdentifier item, int amount, long expires,
                PatternCraftingReference deliveryReference) {
            this.item = item;
            this.amount = amount;
            this.expires = expires;
            this.deliveryReference = deliveryReference;
        }

        private boolean matches(ItemIdentifier item, PatternCraftingReference deliveryReference) {
            return this.item.equals(item) && java.util.Objects.equals(this.deliveryReference, deliveryReference);
        }

        private boolean matches(ItemIdentifierStack arriving, IAdditionalTargetInformation info) {
            if (!item.equals(arriving.getItem())) {
                return false;
            }
            if (!(info instanceof PatternTargetInformation target)) {
                return false;
            }
            return deliveryReference != null && deliveryReference.equals(target.deliveryReference());
        }
    }
}
