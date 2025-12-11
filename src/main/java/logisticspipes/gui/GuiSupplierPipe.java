/*
 * Copyright (c) Krapht, 2011 "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0,
 * or MMPL. Please check the contents of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package logisticspipes.gui;

import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.module.SupplierPipeLimitedPacket;
import logisticspipes.network.packets.module.SupplierPipeModePacket;
import logisticspipes.network.packets.pipe.SlotFinderOpenGuiPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;

public class GuiSupplierPipe extends LogisticsBaseGuiScreen {

    private final ModuleActiveSupplier module;
    private boolean hasPatternUpgrade;
    @Getter
    private final DefaultGuiLayout autoGuiLayout;


    public GuiSupplierPipe(IInventory playerInventory, ModuleActiveSupplier module, DummyContainer container) {
        super(container);
        hasPatternUpgrade = module.hasPatternUpgrade();

        autoGuiLayout = new DefaultGuiLayout(this, playerInventory, module.getDummyInventory(), container)
            .setTitle("Supplier")
            .addSlots(9)
            .build();

        this.module = module;
    }

    public GuiSupplierPipe(InventoryPlayer inventory, ModuleActiveSupplier module, boolean patternUpgrade, int[] slots) {
        this(inventory, module, null);
        hasPatternUpgrade = patternUpgrade;
        module.slotArray =  slots;
    }


    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        this.autoGuiLayout.setMc(mc);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        autoGuiLayout.renderForeground();
        if (hasPatternUpgrade) {
            for (int i = 0; i < 9; i++) {
                mc.fontRenderer.drawString(Integer.toString(module.slotArray[i]), 22 + i * 18, 55, 0x404040);
            }
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        autoGuiLayout.renderBackground();
    }

    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(
            new GuiButton(
                0,
                width / 2 + 35,
                height / 2 - 25,
                50,
                20,
                (hasPatternUpgrade ? module.getPatternMode() : module.getSupplyMode()).toString()));
        if (hasPatternUpgrade) {
            buttonList.add(
                new SmallGuiButton(
                    1,
                    guiLeft + 5,
                    guiTop + 68,
                    45,
                    10,
                    module.isLimited() ? "Limited" : "Unlimited"));
            for (int i = 0; i < 9; i++) {
                buttonList.add(new SmallGuiButton(i + 2, guiLeft + 18 + i * 18, guiTop + 40, 17, 10, "Set"));
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton guibutton) {
        if (guibutton.id == 0) {
            if (hasPatternUpgrade) {
                int currentMode = module.getPatternMode().ordinal() + 1;
                if (currentMode >= PatternMode.values().length) {
                    currentMode = 0;
                }
                module.setPatternMode(PatternMode.values()[currentMode]);
                ((GuiButton) buttonList.get(0)).displayString = module.getPatternMode().toString();
            } else {
                int currentMode = module.getSupplyMode().ordinal() + 1;
                if (currentMode >= SupplyMode.values().length) {
                    currentMode = 0;
                }
                module.setSupplyMode(SupplyMode.values()[currentMode]);
                ((GuiButton) buttonList.get(0)).displayString = module.getSupplyMode().toString();
            }
            MainProxy.sendPacketToServer(PacketHandler.getPacket(SupplierPipeModePacket.class).setModulePos(module));
        } else if (hasPatternUpgrade) {
            if (guibutton.id == 1) {
                module.setLimited(!module.isLimited());
                ((GuiButton) buttonList.get(1)).displayString = module.isLimited() ? "Limited" : "Unlimited";
                MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(SupplierPipeLimitedPacket.class).setLimited(module.isLimited())
                        .setModulePos(module));
            } else if (guibutton.id >= 2 && guibutton.id <= 10) {
                MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(SlotFinderOpenGuiPacket.class).setSlot(guibutton.id - 2)
                        .setModulePos(module));
            }
        }
        super.actionPerformed(guibutton);
    }

    public void refreshMode() {
        ((GuiButton) buttonList.get(0)).displayString = (hasPatternUpgrade ? module.getPatternMode()
            : module.getSupplyMode()).toString();
        if (hasPatternUpgrade) {
            ((GuiButton) buttonList.get(1)).displayString = module.isLimited() ? "Limited" : "Unlimited";
        }
    }
}
