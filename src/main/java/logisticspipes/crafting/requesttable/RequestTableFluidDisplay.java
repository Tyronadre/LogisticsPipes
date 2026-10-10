package logisticspipes.crafting.requesttable;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.Loader;
import gregtech.api.util.GTUtility;
import logisticspipes.utils.FluidIdentifier;

/** Uses NEI's fluid display item while retaining the LP fluid identifier for packets and storage. */
final class RequestTableFluidDisplay {

    private RequestTableFluidDisplay() {}

    static ItemStack of(FluidStack fluid, int amount) {
        FluidStack display = fluid.copy();
        display.amount = Math.max(0, amount);
        return Loader.isModLoaded("gregtech") ? GregTechDisplay.create(display)
                : FluidIdentifier.get(display).getItemIdentifier().makeStack(display.amount).makeNormalStack();
    }

    private static final class GregTechDisplay {

        private static ItemStack create(FluidStack fluid) {
            return GTUtility.getFluidDisplayStack(fluid, true);
        }
    }
}
