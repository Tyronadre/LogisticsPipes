package logisticspipes.utils.gui;

import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import logisticspipes.LogisticsPipes;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.modules.abstractmodules.LogisticsModule.ModulePositionType;
import logisticspipes.utils.DummyWorldProvider;

public class DummyModuleContainer extends DummyContainer {

    @Getter
    private final LogisticsModule module;
    private final int slot;

    public DummyModuleContainer(EntityPlayer player, int slot) {
        super(player.inventory, null);
        this.slot = slot;
        var moduleStack = player.inventory.mainInventory[slot];
        module = LogisticsPipes.ModuleItem
                .getModuleForItem(moduleStack, null, new DummyWorldProvider(player.worldObj), null);
        module.registerPosition(ModulePositionType.IN_HAND, slot);
        ItemModuleInformationManager.readInformation(moduleStack, module);
    }

    public DummyModuleContainer(EntityPlayer player, int slot, LogisticsModule module) {
        super(player.inventory, null);
        this.slot = slot;
        this.module = module;
        module.registerPosition(ModulePositionType.IN_HAND, slot);
        module.registerHandler(new DummyWorldProvider(player.worldObj), null);
        ItemModuleInformationManager.readInformation(player.inventory.mainInventory[slot], module);
    }

    public void setInventory(IInventory inv) {
        _dummyInventory = inv;
    }

    @Override
    protected Slot addSlotToContainer(Slot par1Slot) {
        if (par1Slot != null && par1Slot.getSlotIndex() == slot && par1Slot.inventory == _playerInventory) {
            return super.addSlotToContainer(new UnmodifiableSlot(par1Slot));
        }
        return super.addSlotToContainer(par1Slot);
    }

    @Override
    public void onContainerClosed(EntityPlayer par1EntityPlayer) {
        super.onContainerClosed(par1EntityPlayer);
        ItemModuleInformationManager.saveInformation(par1EntityPlayer.inventory.mainInventory[slot], module);
        par1EntityPlayer.inventory.markDirty();
    }
}
