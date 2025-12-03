package logisticspipes.network.guis.pipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import logisticspipes.LogisticsPipes;
import logisticspipes.gui.GuiChassiPipe;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.abstractguis.BooleanModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.utils.gui.DummyContainer;

public class ChassiGuiProvider extends BooleanModuleCoordinatesGuiProvider {

    public ChassiGuiProvider(int id) {
        super(id);
    }

    @Override
    public Object getClientGui(EntityPlayer player) {
        LogisticsTileGenericPipe pipe = getPipe(player.getEntityWorld());
        if (pipe == null || !(pipe.pipe instanceof PipeLogisticsChassi)) {
            return null;
        }
        return new GuiChassiPipe(player, (PipeLogisticsChassi) pipe.pipe, isFlag());
    }

    @Override
    public DummyContainer getContainer(EntityPlayer player) {
        LogisticsTileGenericPipe pipe = getPipe(player.getEntityWorld());
        if (pipe == null || !(pipe.pipe instanceof PipeLogisticsChassi chassisPipe)) {
            return null;
        }
        IInventory moduleInventory = chassisPipe.getModuleInventory();
        DummyContainer dummy = new DummyContainer(player.inventory, moduleInventory);
        for (int moduleSlot = 0; moduleSlot < chassisPipe.getChassieSize(); moduleSlot++) {
            dummy.addModuleSlot(moduleSlot, moduleInventory, 19, 9 + 20 * moduleSlot, chassisPipe);
        }

        dummy.addNormalSlotsForPlayerInventory(18, 20 * chassisPipe.getChassieSize() + 20);

        if (chassisPipe.getUpgradeManager().hasUpgradeModuleUpgrade()) {
            for (int i = 0; i < chassisPipe.getChassiSize(); i++) {
                final int fI = i;
                ModuleUpgradeManager upgradeManager = chassisPipe.getModuleUpgradeManager(i);
                dummy.addRestrictedSlot(
                        0,
                        upgradeManager.getInv(),
                        145,
                        9 + i * 20,
                        itemStack -> ChassiGuiProvider.checkStack(itemStack, chassisPipe, fI));
                dummy.addRestrictedSlot(
                        1,
                        upgradeManager.getInv(),
                        165,
                        9 + i * 20,
                        itemStack -> ChassiGuiProvider.checkStack(itemStack, chassisPipe, fI));
            }
        }
        return dummy;
    }

    public static boolean checkStack(ItemStack stack, PipeLogisticsChassi chassiPipe, int moduleSlot) {
        if (stack == null) {
            return false;
        }
        if (!stack.getItem().equals(LogisticsPipes.UpgradeItem)) {
            return false;
        }
        LogisticsModule module = chassiPipe.getModules().getModule(moduleSlot);
        if (module == null) {
            return false;
        }
        return LogisticsPipes.UpgradeItem.getUpgradeForItem(stack, null).isAllowedForModule(module);
    }

    @Override
    public GuiProvider template() {
        return new ChassiGuiProvider(getId());
    }
}
