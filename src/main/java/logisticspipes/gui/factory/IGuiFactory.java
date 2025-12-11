package logisticspipes.gui.factory;

import logisticspipes.modules.abstractmodules.LogisticsGuiModule;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.DummyModuleContainer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;

public interface IGuiFactory<V extends LogisticsGuiModule> {

    /***
     * Creates the GuiContainer that will be displayed in the client
     * @param player the player
     * @param module the module
     * @param data additional data
     * @return the created GuiContainer
     */
    GuiContainer createClientGui(EntityPlayer player, V module, Object ...data);

    /**
     * Creates a DummyContainer for the given module that is used on the serverside.
     * It contains the player inventory, and the module inventory normally
     * @param player the player
     * @param module the module
     * @return the DummyContainer
     */
    default DummyContainer createContainer(EntityPlayer player, V module) {
        return createContainer(player, module, null);
    }

    /**
     *
     * @param player
     * @param module
     * @param container
     * @return
     */
    DummyContainer createContainer(EntityPlayer player, V module, DummyModuleContainer container);



}
