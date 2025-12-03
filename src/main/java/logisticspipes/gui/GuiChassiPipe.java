/*
 * Copyright (c) Krapht, 2011 "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0,
 * or MMPL. Please check the contents of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package logisticspipes.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import logisticspipes.items.ItemModule;
import logisticspipes.modules.abstractmodules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.pipe.ChassiGuiProvider;
import logisticspipes.network.packets.chassis.ChassisGUI;
import logisticspipes.pipes.PipeLogisticsChassi;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.string.StringUtils;

public class GuiChassiPipe extends LogisticsBaseGuiScreen {

    private final PipeLogisticsChassi chassisPipe;
    private final EntityPlayer _player;
    private final IInventory _moduleInventory;
    // private final GuiScreen _previousGui;

    private int left;
    private int top;

    private final boolean hasUpgradeModuleUpgrade;

    public GuiChassiPipe(EntityPlayer player, PipeLogisticsChassi chassi, boolean hasUpgradeModuleUpgrade) { // ,
        // GuiScreen
        // previousGui)
        // {
        super(null);
        _player = player;
        chassisPipe = chassi;
        _moduleInventory = chassi.getModuleInventory();
        // _previousGui = previousGui;
        this.hasUpgradeModuleUpgrade = hasUpgradeModuleUpgrade;

        IInventory moduleInventory = chassisPipe.getModuleInventory();
        DummyContainer dummy = new DummyContainer(player.inventory, moduleInventory);
        for (int moduleSlot = 0; moduleSlot < chassisPipe.getChassieSize(); moduleSlot++) {
            dummy.addModuleSlot(moduleSlot, moduleInventory, 19, 9 + 20 * moduleSlot, chassisPipe);
        }

        dummy.addNormalSlotsForPlayerInventory(18, 20 * chassisPipe.getChassieSize() + 17);

        if (hasUpgradeModuleUpgrade) {
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

        inventorySlots = dummy;

        xSize = 195;
        ySize = chassi.getChassiSize() * 20 + 107;
    }

    @Override
    public void initGui() {
        super.initGui();

        left = width / 2 - xSize / 2;
        top = height / 2 - ySize / 2;

        buttonList.clear();
        for (int i = 0; i < chassisPipe.getChassiSize(); i++) {
            buttonList.add(new SmallGuiButton(i, left + 5, top + 12 + 20 * i, 10, 10, "!"));
            if (_moduleInventory == null) {
                continue;
            }
            ItemStack module = _moduleInventory.getStackInSlot(i);
            if (module == null || chassisPipe.getLogisticsModule().getSubModule(i) == null) {
                buttonList.get(i).visible = false;
            } else {
                buttonList.get(i).visible = chassisPipe.getLogisticsModule().getSubModule(i).hasGui();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        LogisticsModule module = chassisPipe.getLogisticsModule().getSubModule(guibutton.id);
        if (module != null) {
            final ModernPacket packet = PacketHandler.getPacket(ChassisGUI.class).setButtonID(guibutton.id)
                    .setPosX(chassisPipe.getX()).setPosY(chassisPipe.getY()).setPosZ(chassisPipe.getZ());
            MainProxy.sendPacketToServer(packet);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        super.drawGuiContainerForegroundLayer(par1, par2);
        for (int i = 0; i < chassisPipe.getChassiSize(); i++) {
            ItemStack module = _moduleInventory.getStackInSlot(i);
            if (module == null || chassisPipe.getLogisticsModule().getSubModule(i) == null) {
                buttonList.get(i).visible = false;
            } else {
                buttonList.get(i).visible = chassisPipe.getLogisticsModule().getSubModule(i).hasGui();
            }
        }
        for (int moduleSlot = 0; moduleSlot < chassisPipe.getChassieSize(); moduleSlot++) {
            mc.fontRenderer
                    .drawString(getModuleName(moduleSlot), 40, 14 + moduleSlot * CHASSIS_SLOT_TEXTURE_HEIGHT, 0x404040);
        }
    }

    private String getModuleName(int slot) {
        if (_moduleInventory == null) {
            return "";
        }
        if (_moduleInventory.getStackInSlot(slot) == null) {
            return "";
        }
        if (!(_moduleInventory.getStackInSlot(slot).getItem() instanceof ItemModule)) {
            return "";
        }
        String name = _moduleInventory.getStackInSlot(slot).getItem()
                .getItemStackDisplayName(_moduleInventory.getStackInSlot(slot));
        if (!hasUpgradeModuleUpgrade) {
            return name;
        }
        return StringUtils.getWithMaxWidth(name, 100, fontRendererObj);
    }

    private static final int CHASSIS_TOP_TEXTURE_HEIGHT = 8;
    private static final ResourceLocation CHASSIS_TOP_TEXTURE = new ResourceLocation(
            "logisticspipes",
            "textures/gui/chassipipe_top.png");

    private static final int CHASSIS_BOTTOM_TEXTURE_HEIGHT = 98;
    private static final ResourceLocation CHASSIS_BOTTOM_TEXTURE = new ResourceLocation(
            "logisticspipes",
            "textures/gui/chassipipe_bottom.png");

    private static final int CHASSIS_SLOT_TEXTURE_HEIGHT = 20;
    private static final ResourceLocation CHASSIS_SLOT_TEXTURE = new ResourceLocation(
            "logisticspipes",
            "textures/gui/chassipipe_slot.png");

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int x, int y) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        var currentYOffset = 0;

        mc.renderEngine.bindTexture(CHASSIS_TOP_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, CHASSIS_TOP_TEXTURE_HEIGHT);
        currentYOffset += CHASSIS_TOP_TEXTURE_HEIGHT;

        mc.renderEngine.bindTexture(CHASSIS_SLOT_TEXTURE);
        for (int i = 0; i < chassisPipe.getChassiSize(); i++) {
            drawTexturedModalRect(guiLeft, guiTop + currentYOffset, 0, 0, xSize, CHASSIS_SLOT_TEXTURE_HEIGHT);
            currentYOffset += CHASSIS_SLOT_TEXTURE_HEIGHT;
        }

        mc.renderEngine.bindTexture(CHASSIS_BOTTOM_TEXTURE);
        drawTexturedModalRect(guiLeft, guiTop + currentYOffset, 0, 0, xSize, CHASSIS_BOTTOM_TEXTURE_HEIGHT);
        currentYOffset += CHASSIS_BOTTOM_TEXTURE_HEIGHT;

        if (hasUpgradeModuleUpgrade) {
            for (int i = 0; i < chassisPipe.getChassiSize(); i++) {
                GuiGraphics.drawSlotBackground(mc, guiLeft + 144, guiTop + 8 + i * 20);
                GuiGraphics.drawSlotBackground(mc, guiLeft + 164, guiTop + 8 + i * 20);
            }
        }
    }
}
