package logisticspipes.network.guis.module.inhand;

import logisticspipes.gui.factory.SupplierGuiFactory;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.utils.gui.DummyModuleContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ActiveSupplierInHand extends ModuleInHandGuiProvider {

    public ActiveSupplierInHand(int id) {
        super(id);
    }

    @Override
    public Object getClientGui(EntityPlayer player) {
        if (logisticsModule == null) logisticsModule = getLogisticsModule(player);
        if (!(logisticsModule instanceof ModuleActiveSupplier module)) return null;

        return SupplierGuiFactory.INSTANCE.createClientGui(player, module, false, null);
    }

    @Override
    public Container getContainer(EntityPlayer player) {
        if (logisticsModule == null || !(logisticsModule instanceof ModuleActiveSupplier module)) return null;

        DummyModuleContainer dummy = new DummyModuleContainer(player, getInvSlot(), module);
        dummy.setInventory(module.getDummyInventory());

        return SupplierGuiFactory.INSTANCE.createContainer(player, module, dummy);
    }

    @Override
    public GuiProvider template() {
        return new ActiveSupplierInHand(getId());
    }
}
