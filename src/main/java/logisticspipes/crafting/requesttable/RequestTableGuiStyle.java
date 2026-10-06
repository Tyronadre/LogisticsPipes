package logisticspipes.crafting.requesttable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

/** Shared Minecraft bevels for the table, scrollbars, and request popup. */
@SideOnly(Side.CLIENT)
final class RequestTableGuiStyle {

    static final int TEXT = 0x404040;
    static final int MUTED = 0x686868;

    private RequestTableGuiStyle() {
    }

    static String formatCount(int amount) {
        if (amount >= 1_000_000_000) {
            return amount / 1_000_000_000 + "G";
        }
        if (amount >= 1_000_000) {
            return amount / 1_000_000 + "M";
        }
        if (amount >= 1_000) {
            return amount / 1_000 + "k";
        }
        return Integer.toString(amount);
    }

    /** Matches NEI's fluid display text scale, shadow and bottom-left placement. */
    static void drawCount(FontRenderer font, String amount, int x, int y, int color, boolean topRight) {
        float scale = font.getUnicodeFlag() ? 3.0F / 4.0F : 1.0F / 2.0F;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glTranslatef(x, y, 150.0F);
        GL11.glScalef(scale, scale, 1.0F);
        int drawX = topRight ? (int) (16 / scale) - font.getStringWidth(amount) : 0;
        int drawY = topRight ? Math.round(-1 / scale) : (int) (16 / scale) - font.FONT_HEIGHT + 1;
        font.drawString(amount, drawX, drawY, color, true);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    static void inset(int x, int y, int width, int height) {
        Gui.drawRect(x, y, x + width, y + height, 0xff373737);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, 0xff8b8b8b);
        Gui.drawRect(x, y + height - 1, x + width, y + height, 0xffffffff);
        Gui.drawRect(x + width - 1, y, x + width, y + height, 0xffffffff);
    }

    static void raised(int x, int y, int width, int height, boolean hover) {
        Gui.drawRect(x, y, x + width, y + height, 0xff373737);
        Gui.drawRect(x + 1, y + 1, x + width - 1, y + height - 1, hover ? 0xffa8b4c2 : 0xffc6c6c6);
        Gui.drawRect(x + 1, y + 1, x + width - 2, y + 2, 0xffffffff);
        Gui.drawRect(x + 1, y + 1, x + 2, y + height - 2, 0xffffffff);
        Gui.drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xff555555);
        Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xff555555);
    }

    static void scrollbar(RequestTableLayout layout, int scrollRow, int maxScroll) {
        int x = layout.scrollbarX;
        int y = layout.panelTop;
        int height = layout.panelHeight;
        inset(x, y, 7, height);
        int rows = layout.getVisiblePanelRows();
        int thumbHeight = Math.max(12, height * rows / Math.max(1, rows + maxScroll));
        int travel = Math.max(0, height - thumbHeight);
        int top = y + (maxScroll == 0 ? 0 : travel * scrollRow / maxScroll);
        raised(x, top, 7, thumbHeight, false);
    }
}
