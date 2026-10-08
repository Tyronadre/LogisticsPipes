package logisticspipes.crafting.requesttable;

import logisticspipes.network.PacketGuards;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.crafting.requesttable.RequestTableUpgradeStatePacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Per-viewer material escrow; completed tiers live on the table, never on this container. */
public class RequestTableUpgradeContainer extends DummyContainer {

    public static final int MATERIAL_SLOTS = 4;
    public static final int INVENTORY_LEFT = 62;
    private final RequestTablePipe table;
    private final EntityPlayer player;
    private final InventoryBasic materials = new InventoryBasic("Upgrade materials", false, MATERIAL_SLOTS);
    private final List<Slot> materialSlots = new ArrayList<>();
    private RequestTableItemUpgradeConfig config = RequestTableItemUpgradeConfig.getConfigured();
    private RequestTableItemUpgradeBranch selectedBranch = RequestTableItemUpgradeBranch.SLOT_COUNT;
    private int selectedTier;
    private int syncedSlotTier = -1;
    private int syncedSizeTier = -1;
    private boolean stateDirty = true;
    private boolean ready;
    private boolean materialsReturned;

    public RequestTableUpgradeContainer(EntityPlayer player, RequestTablePipe table) {
        super(player, null);
        this.player = player;
        this.table = table;
        selectedTier = Math.min(RequestTableItemUpgradeConfig.TIER_COUNT, table.getItemSlotTier() + 1);
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            Slot slot = addSlotToContainer(new Slot(materials, i, 0, 0) {

                @Override
                public boolean isItemValid(ItemStack stack) {
                    return config.matchesCircuit(stack, selectedTier);
                }
            });
            materialSlots.add(slot);
        }
        addNormalSlotsForPlayerInventory(0, 0);
    }

    public RequestTablePipe getTable() {
        return table;
    }

    public RequestTableItemUpgradeConfig getConfig() {
        return config;
    }

    public RequestTableItemUpgradeBranch getSelectedBranch() {
        return selectedBranch;
    }

    public int getSelectedTier() {
        return selectedTier;
    }

    public boolean isReady() {
        return ready;
    }

    public void awaitState() {
        ready = false;
    }

    public void layout(int guiHeight) {
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            Slot slot = materialSlots.get(i);
            slot.xDisplayPosition = INVENTORY_LEFT + i * 18;
            slot.yDisplayPosition = guiHeight - 117;
        }
        for (int i = 0; i < 36; i++) {
            Slot slot = inventorySlots.get(MATERIAL_SLOTS + i);
            slot.xDisplayPosition = INVENTORY_LEFT + (i % 9) * 18;
            slot.yDisplayPosition = guiHeight - 84 + (i < 27 ? (i / 9) * 18 : 58);
        }
    }

    public void applyState(RequestTableItemUpgradeConfig config, int slotTier, int sizeTier,
                           RequestTableItemUpgradeBranch branch, int tier) {
        this.config = config;
        table.applyItemUpgradeTiers(slotTier, sizeTier);
        selectedBranch = branch;
        selectedTier = tier;
        ready = true;
    }

    public void select(RequestTableItemUpgradeBranch branch, int tier) {
        if (branch == null || !RequestTableItemUpgradeConfig.isValidTier(tier)) return;
        selectedBranch = branch;
        selectedTier = tier;
        stateDirty = true;
        detectAndSendChanges();
    }

    public int getMaterialCount() {
        int count = 0;
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = materials.getStackInSlot(i);
            if (config.matchesCircuit(stack, selectedTier)) count += stack.stackSize;
        }
        return count;
    }

    public boolean canUpgrade() {
        return table.getItemUpgradeTier(selectedBranch) + 1 == selectedTier
            && getMaterialCount() >= config.getCost(selectedTier);
    }

    public void startUpgrade(RequestTableItemUpgradeBranch branch, int tier) {
        if (MainProxy.isClient(player.worldObj) || branch != selectedBranch
            || tier != selectedTier
            || !canInteractWith(player)
            || !canUpgrade())
            return;
        // Both validations run before consumption; another viewer cannot pay twice for the same tier.
        if (!table.unlockItemUpgrade(branch, tier)) return;
        int remaining = config.getCost(tier);
        for (int i = 0; i < MATERIAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = materials.getStackInSlot(i);
            if (!config.matchesCircuit(stack, tier)) continue;
            int consumed = Math.min(remaining, stack.stackSize);
            materials.decrStackSize(i, consumed);
            remaining -= consumed;
        }
        player.inventory.markDirty();
        player.worldObj.playSoundEffect(
            table.getX() + 0.5,
            table.getY() + 0.5,
            table.getZ() + 0.5,
            "random.anvil_use",
            0.35F,
            1.2F);
        stateDirty = true;
        detectAndSendChanges();
    }

    @Override
    public boolean canInteractWith(EntityPlayer viewer) {
        return viewer == player && PacketGuards.canConfigurePipe(viewer, table);
    }

    @Override
    public void detectAndSendChanges() {
        if (MainProxy.isClient(player.worldObj)) return;
        super.detectAndSendChanges();
        boolean changed = syncedSlotTier != table.getItemSlotTier() || syncedSizeTier != table.getItemSizeTier();
        if (!stateDirty && !changed) return;
        for (ICrafting crafter : crafters) {
            if (crafter instanceof EntityPlayer viewer) {
                MainProxy.sendPacketToPlayer(
                    PacketHandler.getPacket(RequestTableUpgradeStatePacket.class).setState(this),
                    viewer);
            }
        }
        syncedSlotTier = table.getItemSlotTier();
        syncedSizeTier = table.getItemSizeTier();
        stateDirty = false;
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        stateDirty = true;
        super.addCraftingToCrafters(crafter);
    }

    @Override
    public void onContainerClosed(EntityPlayer viewer) {
        super.onContainerClosed(viewer);
        if (MainProxy.isClient(viewer.worldObj) || materialsReturned) return;
        materialsReturned = true;
        for (int i = 0; i < MATERIAL_SLOTS; i++) {
            ItemStack stack = materials.getStackInSlotOnClosing(i);
            if (stack != null && (!viewer.isEntityAlive() || !viewer.inventory.addItemStackToInventory(stack))) {
                viewer.dropPlayerItemWithRandomChoice(stack, false);
            }
        }
        viewer.inventory.markDirty();
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer viewer, int index) {
        if (index < 0 || index >= inventorySlots.size()) return null;
        Slot source = inventorySlots.get(index);
        if (!source.getHasStack() || !source.canTakeStack(viewer)) return null;
        ItemStack moving = source.getStack();
        ItemStack original = moving.copy();
        if (index < MATERIAL_SLOTS) {
            if (!mergeItemStack(moving, MATERIAL_SLOTS, inventorySlots.size(), true)) return null;
        } else {
            if (!config.matchesCircuit(moving, selectedTier) || !mergeItemStack(moving, 0, MATERIAL_SLOTS, false))
                return null;
        }
        source.putStack(moving.stackSize == 0 ? null : moving);
        source.onPickupFromSlot(viewer, moving);
        source.onSlotChanged();
        return original;
    }

    @Override
    protected void retrySlotClick(int slot, int button, boolean shift, EntityPlayer viewer) {
        // The merge already fills every available destination; a partial transfer needs no retry.
    }
}
