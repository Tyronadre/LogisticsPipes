package logisticspipes.crafting.requesttable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Vanilla buttons with pixel icons or the actual block item, keeping selected storage icons visible. */
@SideOnly(Side.CLIENT)
final class RequestTableIconButton extends GuiButton {

    private static final ResourceLocation ICONS = new ResourceLocation(
        "logisticspipes",
        "textures/gui/requesttable_icons.png");

    private Icon icon;
    private ItemStack item;
    private boolean selected;
    private float fill = -1;
    private int fillColor;
    private String tabLabel;

    RequestTableIconButton(int id, int x, int y, int width, int height, Icon icon) {
        super(id, x, y, width, height, "");
        this.icon = icon;
    }

    void setIcon(Icon icon) {
        this.icon = icon;
    }

    void setItem(ItemStack item) {
        this.item = item;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    void setFill(float fill, int color) {
        this.fill = fill;
        this.fillColor = color;
    }

    void setTabLabel(String label) {
        tabLabel = label;
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
        if (selected) {
            drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 2, 0xffe8c66a);
            if (tabLabel != null) {
                drawRect(xPosition + 1, yPosition + height - 1, xPosition + width - 1, yPosition + height, 0xffc6c6c6);
            }
        }
        int x = xPosition + (tabLabel == null ? (width - 16) / 2 : 5);
        int y = yPosition + (tabLabel == null ? (height - 16) / 2 : 2);
        if (item != null) {
            // The chest's lid is drawn before its base, so native block models need depth testing.
            GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDepthMask(true);
            new ItemStackRenderer(x, y, 100.0F, true, false, enabled)
                .setItemIdentifierStack(ItemIdentifierStack.getFromStack(item))
                .setDisplayAmount(DisplayAmount.NEVER).renderInGui();
            GL11.glPopAttrib();
        } else if (icon == Icon.MESSAGES_ON || icon == Icon.MESSAGES_OFF) {
            drawRequestMessagesIcon(x, y, icon == Icon.MESSAGES_ON);
        } else {
            mc.renderEngine.bindTexture(ICONS);
            GL11.glColor4f(enabled ? 1 : 0.5F, enabled ? 1 : 0.5F, enabled ? 1 : 0.5F, 1);
            if (width < 18 || height < 18) {
                GL11.glPushMatrix();
                GL11.glTranslatef(xPosition + 2, yPosition + 2, 0);
                GL11.glScalef(0.5F, 0.5F, 1);
                drawTexturedModalRect(0, 0, icon.ordinal() * 16, 0, 16, 16);
                GL11.glPopMatrix();
            } else {
                drawTexturedModalRect(x, y, icon.ordinal() * 16, 0, 16, 16);
            }
        }
        GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);
        if (tabLabel != null) {
            mc.fontRenderer.drawString(
                tabLabel,
                xPosition + 25,
                yPosition + 5,
                enabled ? RequestTableGuiStyle.TEXT : RequestTableGuiStyle.MUTED);
        }
        if (fill >= 0) {
            int length = Math.round((width - 8) * Math.max(0, Math.min(1, fill)));
            int top = yPosition + height - 7;
            drawRect(xPosition + 3, top, xPosition + width - 3, top + 5, 0xff373737);
            drawRect(xPosition + 4, top + 1, xPosition + width - 4, top + 4, 0xff555555);
            drawRect(xPosition + 4, top + 1, xPosition + 4 + length, top + 4, fillColor);
        }
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void drawRequestMessagesIcon(int x, int y, boolean on) {
        drawRect(x + 1, y + 2, x + 15, y + 12, 0xff373737);
        drawRect(x + 2, y + 3, x + 14, y + 11, 0xffeee4cc);
        drawRect(x + 3, y + 11, x + 6, y + 15, 0xff373737);
        drawRect(x + 4, y + 10, x + 5, y + 13, 0xffeee4cc);
        drawRect(x + 4, y + 5, x + 12, y + 6, 0xff777063);
        drawRect(x + 4, y + 8, x + 10, y + 9, 0xff777063);
        if (on) {
            drawRect(x + 8, y + 11, x + 10, y + 13, 0xff427b39);
            drawRect(x + 10, y + 13, x + 12, y + 15, 0xff427b39);
            drawRect(x + 12, y + 11, x + 14, y + 13, 0xff427b39);
            drawRect(x + 14, y + 9, x + 16, y + 11, 0xff427b39);
        } else {
            for (int i = 0; i < 5; i++) {
                drawRect(x + 10 + i, y + 10 + i, x + 12 + i, y + 12 + i, 0xffb14d43);
                drawRect(x + 14 - i, y + 10 + i, x + 16 - i, y + 12 + i, 0xffb14d43);
            }
        }
    }

    enum Icon {
        NETWORK,
        NAME,
        AMOUNT,
        ASCENDING,
        DESCENDING,
        BOTH,
        STORED,
        CRAFTABLE,
        SEND,
        REQUEST,
        CLEAR,
        MESSAGES_ON,
        MESSAGES_OFF
    }
}
