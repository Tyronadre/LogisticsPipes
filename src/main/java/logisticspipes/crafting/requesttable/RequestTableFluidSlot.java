package logisticspipes.crafting.requesttable;

import logisticspipes.crafting.patternStack.PatternFluidStack;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.UnmodifiableSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Displays the stored fluid with the same item used by NEI, without making it transferable. */
public class RequestTableFluidSlot extends UnmodifiableSlot {

    private final RequestTablePipe table;

    public RequestTableFluidSlot(RequestTablePipe table, int slot, int x, int y) {
        super(table.getFluidStorage(), slot, x, y);
        this.table = table;
    }

    @Override
    public ItemStack getStack() {
        if (MainProxy.isServer(table.getWorld())) {
            return super.getStack();
        }
        FluidStack fluid = table.getFluidStorage().getFluid(getSlotIndex());
        return fluid == null ? null
            : new PatternFluidStack(FluidIdentifier.get(fluid), fluid.amount).makeDisplayItemStack();
    }
}
