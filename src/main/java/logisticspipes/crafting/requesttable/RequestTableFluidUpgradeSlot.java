package logisticspipes.crafting.requesttable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/** A single fluid-enabling upgrade that remains locked until every internal tank is empty. */
final class RequestTableFluidUpgradeSlot extends Slot {

    private final RequestTablePipe table;

    RequestTableFluidUpgradeSlot(RequestTablePipe table, int x, int y) {
        super(table.getFluidUpgradeInventory(), 0, x, y);
        this.table = table;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return table.isFluidUpgrade(stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return !getHasStack() || table.canRemoveFluidUpgrade();
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
