package logisticspipes.crafting.requesttable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

/** A small CRT and power lamp tied to the monitoring-upgrade socket below it. */
@SideOnly(Side.CLIENT)
final class RequestTableMonitorButton extends GuiButton {

    static final int POWER_ON = 0xff86be65;
    static final int POWER_OFF = 0xff777777;

    private boolean selected;

    RequestTableMonitorButton(int id, RequestTableLayout layout) {
        super(
            id,
            layout.monitorButtonX,
            layout.monitorButtonY,
            layout.monitorButtonWidth,
            layout.monitorButtonHeight,
            "");
    }

    static void drawMonitorIcon(int x, int y, boolean powered, boolean animate) {
        Gui.drawRect(x, y, x + 16, y + 11, 0xff373737);
        Gui.drawRect(x + 1, y + 1, x + 15, y + 10, powered ? 0xff475446 : 0xff626262);
        int phosphor = powered ? POWER_ON : 0xff8a8a8a;
        // A root and two branches suggest the crafting tree rather than generic storage.
        Gui.drawRect(x + 7, y + 2, x + 9, y + 4, phosphor);
        Gui.drawRect(x + 7, y + 4, x + 8, y + 6, phosphor);
        Gui.drawRect(x + 4, y + 5, x + 12, y + 6, phosphor);
        Gui.drawRect(x + 3, y + 6, x + 6, y + 8, phosphor);
        Gui.drawRect(x + 10, y + 6, x + 13, y + 8, phosphor);
        if (powered && animate) {
            int scan = (int) ((Minecraft.getSystemTime() / 450) % 3);
            Gui.drawRect(x + 12, y + 2 + scan, x + 13, y + 3 + scan, 0xffc8e7ae);
        }
        Gui.drawRect(x + 7, y + 11, x + 9, y + 13, 0xff555555);
        Gui.drawRect(x + 4, y + 13, x + 12, y + 14, 0xff373737);
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        boolean hover = enabled && mouseX >= xPosition
            && mouseX < xPosition + width
            && mouseY >= yPosition
            && mouseY < yPosition + height;
        RequestTableGuiStyle.raised(xPosition, yPosition, width, height, hover);
        if (!enabled) {
            drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, 0xffa0a0a0);
        } else if (selected) {
            drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 2, 0xffe8c66a);
        }
        drawMonitorIcon(xPosition + (width - 16) / 2, yPosition + 3, enabled, true);
        drawRect(xPosition + 3, yPosition + height - 5, xPosition + 6, yPosition + height - 2, 0xff373737);
        drawRect(
            xPosition + 4,
            yPosition + height - 4,
            xPosition + 5,
            yPosition + height - 3,
            enabled ? POWER_ON : POWER_OFF);
    }
}
