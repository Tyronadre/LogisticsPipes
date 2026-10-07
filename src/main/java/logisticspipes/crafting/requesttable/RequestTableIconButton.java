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
    private static final ResourceLocation CLEAR_ICON = new ResourceLocation(
        "logisticspipes",
        "textures/gui/requesttable_clear.png");
    private static final int CLEAR_TEXTURE_SIZE = 128;
    private static final int TAB_LABEL_TOP = 4;
    private static final int TAB_LETTER_HEIGHT = 7;
    private static final int TAB_FILL_GAP = 2;

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
        if (tabLabel == null || selected) {
            drawContents(mc, mouseX, mouseY);
        }
    }

    /** Inactive tabs are occluded by the panel; the selected tab joins it in the normal button pass. */
    void drawBehindPanel(Minecraft mc, int mouseX, int mouseY) {
        if (tabLabel != null && !selected) {
            drawContents(mc, mouseX, mouseY);
        }
    }

    private void drawContents(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        boolean hover = enabled && mouseX >= xPosition
            && mouseX < xPosition + width
            && mouseY >= yPosition
            && mouseY < yPosition + height;
        RequestTableGuiStyle.raised(xPosition, yPosition, width, height, hover);
        if (tabLabel != null && !selected) {
            drawRect(
                xPosition + 2,
                yPosition + 2,
                xPosition + width - 2,
                yPosition + height - 2,
                hover ? 0xffb8bec6 : 0xffaeaeae);
        }
        if (selected) {
            drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 2, 0xffe8c66a);
            if (tabLabel != null) {
                drawRect(xPosition + 1, yPosition + height - 2, xPosition + width - 1, yPosition + height, 0xffc6c6c6);
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
        } else if (icon == Icon.CLEAR) {
            drawClearIcon(mc);
        } else if (icon == Icon.MESSAGES_ON || icon == Icon.MESSAGES_OFF) {
            drawRequestMessagesIcon(x, y, icon == Icon.MESSAGES_ON);
        } else if (icon.ordinal() >= Icon.SEARCH_STANDARD.ordinal()) {
            drawTerminalControlIcon(x, y);
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
                yPosition + TAB_LABEL_TOP,
                enabled ? RequestTableGuiStyle.TEXT : RequestTableGuiStyle.MUTED);
        }
        if (fill >= 0) {
            int left = xPosition + (tabLabel == null ? 3 : 25);
            int barWidth = xPosition + width - 3 - left;
            int length = Math.round((barWidth - 2) * Math.max(0, Math.min(1, fill)));
            int top = tabLabel == null ? yPosition + height - 7
                : yPosition + TAB_LABEL_TOP + TAB_LETTER_HEIGHT + TAB_FILL_GAP;
            drawRect(left, top, left + barWidth, top + 5, 0xff373737);
            drawRect(left + 1, top + 1, left + barWidth - 1, top + 4, 0xff555555);
            drawRect(left + 1, top + 1, left + 1 + length, top + 4, fillColor);
        }
        GL11.glPopAttrib();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void drawClearIcon(Minecraft mc) {
        int size = Math.max(1, Math.min(width, height) - 2);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.renderEngine.bindTexture(CLEAR_ICON);
        GL11.glColor4f(enabled ? 1 : 0.5F, enabled ? 1 : 0.5F, enabled ? 1 : 0.5F, 1);
        func_152125_a(
            xPosition + (width - size) / 2,
            yPosition + (height - size) / 2,
            0,
            0,
            CLEAR_TEXTURE_SIZE,
            CLEAR_TEXTURE_SIZE,
            size,
            size,
            CLEAR_TEXTURE_SIZE,
            CLEAR_TEXTURE_SIZE);
        GL11.glPopAttrib();
    }

    /** Pixel icons drawn at native GUI resolution, without adding another texture atlas. */
    private void drawTerminalControlIcon(int x, int y) {
        switch (icon) {
            case SEARCH_STANDARD:
            case SEARCH_AUTO:
            case SEARCH_NEI_AUTO:
            case SEARCH_NEI_STANDARD:
                drawRect(x + 2, y + 2, x + 10, y + 10, 0xff373737);
                drawRect(x + 3, y + 3, x + 9, y + 9, 0xffa8c4d6);
                drawRect(x + 4, y + 4, x + 6, y + 6, 0xffe5f1f8);
                for (int i = 0; i < 3; i++) {
                    drawRect(x + 9 + i * 2, y + 9 + i * 2, x + 11 + i * 2, y + 11 + i * 2, 0xff73604a);
                }
                if (icon == Icon.SEARCH_AUTO || icon == Icon.SEARCH_NEI_AUTO) {
                    drawRect(x + 12, y + 1, x + 14, y + 7, 0xffd6a637);
                    drawRect(x + 10, y + 3, x + 16, y + 5, 0xffd6a637);
                }
                if (icon == Icon.SEARCH_NEI_AUTO || icon == Icon.SEARCH_NEI_STANDARD) {
                    drawRect(x + 1, y + 12, x + 8, y + 14, 0xff477dab);
                    drawRect(x + 6, y + 10, x + 8, y + 12, 0xff477dab);
                    drawRect(x + 6, y + 14, x + 8, y + 16, 0xff477dab);
                }
                break;
            case SAVE_SEARCH_ON:
            case SAVE_SEARCH_OFF:
                drawRect(x + 2, y + 1, x + 14, y + 15, 0xff373737);
                drawRect(x + 3, y + 2, x + 13, y + 14, 0xff5c7490);
                drawRect(x + 5, y + 2, x + 12, y + 6, 0xffdeded2);
                drawRect(x + 4, y + 9, x + 12, y + 14, 0xffdeded2);
                drawRect(x + 6, y + 10, x + 10, y + 11, 0xff777063);
                break;
            case TERMINAL_SMALL:
            case TERMINAL_TALL:
                boolean tall = icon == Icon.TERMINAL_TALL;
                int top = tall ? 1 : 4;
                int bottom = tall ? 15 : 12;
                drawRect(x + 1, y + top, x + 15, y + bottom, 0xff373737);
                drawRect(x + 2, y + top + 1, x + 14, y + bottom - 1, 0xffdeded2);
                for (int row = top + 2; row < bottom - 1; row += 3) {
                    drawRect(x + 4, y + row, x + 12, y + row + 1, 0xff777063);
                }
                break;
            case ITEMS_ON:
            case ITEMS_OFF:
                drawRect(x + 1, y + 3, x + 15, y + 14, 0xff373737);
                drawRect(x + 2, y + 4, x + 14, y + 13, 0xffcf9d53);
                drawRect(x + 2, y + 6, x + 14, y + 7, 0xff73604a);
                drawRect(x + 7, y + 5, x + 9, y + 9, 0xffe8c66a);
                break;
            case FLUIDS_ON:
            case FLUIDS_OFF:
                drawRect(x + 7, y + 1, x + 9, y + 4, 0xff477dab);
                drawRect(x + 5, y + 4, x + 11, y + 6, 0xff5b9bbd);
                drawRect(x + 3, y + 6, x + 13, y + 12, 0xff5b9bbd);
                drawRect(x + 4, y + 12, x + 12, y + 14, 0xff5b9bbd);
                drawRect(x + 6, y + 14, x + 10, y + 15, 0xff477dab);
                drawRect(x + 5, y + 7, x + 6, y + 11, 0xffa8d6ef);
                break;
            default:
                break;
        }
        if (icon == Icon.SAVE_SEARCH_OFF || icon == Icon.ITEMS_OFF || icon == Icon.FLUIDS_OFF) {
            for (int i = 0; i < 7; i++) {
                drawRect(x + 2 + i * 2, y + 12 - i * 2, x + 4 + i * 2, y + 14 - i * 2, 0xffb14d43);
            }
        }
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
        MESSAGES_OFF,
        SEARCH_STANDARD,
        SEARCH_AUTO,
        SEARCH_NEI_AUTO,
        SEARCH_NEI_STANDARD,
        SAVE_SEARCH_ON,
        SAVE_SEARCH_OFF,
        TERMINAL_SMALL,
        TERMINAL_TALL,
        ITEMS_ON,
        ITEMS_OFF,
        FLUIDS_ON,
        FLUIDS_OFF
    }
}
