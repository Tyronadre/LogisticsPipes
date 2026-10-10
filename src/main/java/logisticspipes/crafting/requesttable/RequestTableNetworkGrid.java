package logisticspipes.crafting.requesttable;

import static logisticspipes.crafting.requesttable.RequestTableRender.inside;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

/**
 * Scrollable icon grid for requestable network items and fluids.
 */
@SideOnly(Side.CLIENT)
public class RequestTableNetworkGrid {

    private final RequestTableNetworkList entries = new RequestTableNetworkList();
    private int scrollRow;
    private Object[] tooltip;

    /**
     * Replaces the complete network list.
     */
    public void setEntries(List<RequestTableNetworkEntry> newEntries) {
        entries.setEntries(newEntries);
    }

    public void setFluidsEnabled(boolean enabled) {
        if (entries.setFluidsEnabled(enabled)) {
            scrollRow = 0;
        }
    }

    /**
     * Applies a new sort/filter state and invalidates the cached visible list once.
     */
    public void setDisplaySettings(RequestTableDisplaySettings settings) {
        if (entries.setDisplaySettings(settings)) {
            scrollRow = 0;
        }
    }

    /**
     * Updates the cached search result if the text actually changed.
     */
    public void setSearch(String search) {
        if (entries.setSearch(search)) {
            scrollRow = 0;
        }
    }

    /**
     * @return tooltip data for the hovered entry
     */
    public Object[] getTooltip() {
        return tooltip;
    }

    /**
     * Scrolls the grid by whole rows.
     */
    public void scroll(int rows, RequestTableLayout layout) {
        setScrollRow(scrollRow + rows, layout);
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public void setScrollRow(int row, RequestTableLayout layout) {
        scrollRow = Math.max(0, Math.min(getMaxScrollRow(layout), row));
    }

    /**
     * Renders the grid and updates the hover tooltip.
     */
    public void render(LogisticsBaseGuiScreen screen, RequestTableLayout layout, int mouseX, int mouseY) {
        List<RequestTableNetworkEntry> filtered = entries.getVisibleEntries();
        int columns = layout.getNetworkColumns();
        int visibleRows = layout.getVisiblePanelRows();
        int maxScroll = Math.max(0, (filtered.size() + columns - 1) / columns - visibleRows);
        scrollRow = Math.min(scrollRow, maxScroll);

        RequestTableRender.slotGrid(screen.getMC(), layout.panelLeft, layout.panelTop, columns, visibleRows);
        RequestTableGuiStyle.scrollbar(layout, scrollRow, maxScroll);

        tooltip = null;
        int first = scrollRow * columns;
        int visible = visibleRows * columns;
        for (int i = first; i < filtered.size() && i < first + visible; i++) {
            RequestTableNetworkEntry entry = filtered.get(i);
            int localIndex = i - first;
            int x = layout.panelLeft + 1 + (localIndex % columns) * RequestTableLayout.PANEL_CELL;
            int y = layout.panelTop + 1 + (localIndex / columns) * RequestTableLayout.PANEL_CELL;
            boolean hover = inside(
                mouseX,
                mouseY,
                x - 1,
                y - 1,
                RequestTableLayout.PANEL_CELL,
                RequestTableLayout.PANEL_CELL);
            if (hover) {
                Gui.drawRect(x, y, x + 16, y + 16, 0x50ffffff);
                ItemStack tooltipStack = entry.getDisplayStack();
                List<String> details = new ArrayList<>();
                String unit = entry.isFluid() ? " mB" : "";
                details.add("\u00a77Network: " + entry.getNetworkAmount() + unit);
                details.add("\u00a77Internal: " + entry.getInternalAmount() + unit);
                if (entry.isCraftable()) {
                    details.add("\u00a77Craftable");
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                    details.add("\u00a77Total: " + entry.getTotalAmount() + unit);
                }
                tooltip = new Object[] { mouseX, mouseY, tooltipStack, true, details };
            }
            RequestTableRender.item(entry.getDisplayStack(), x, y, 100.0F, false);
            if (!entry.isFluid() && entry.getTotalAmount() != 1) {
                RequestTableGuiStyle.drawCount(
                    screen.getMC().fontRenderer,
                    RequestTableGuiStyle.formatCount(entry.getTotalAmount()),
                    x,
                    y,
                    0xffffff,
                    false);
            }
            drawInternalAmount(screen, entry, x, y);
            if (entry.isCraftable()) {
                RequestTableIcons.craftable(x, y);
            }
        }
    }

    private void drawInternalAmount(LogisticsBaseGuiScreen screen, RequestTableNetworkEntry entry, int x, int y) {
        if (entry.getInternalAmount() <= 0) {
            return;
        }
        RequestTableGuiStyle.drawCount(
            screen.getMC().fontRenderer,
            RequestTableGuiStyle.formatCount(entry.getInternalAmount()),
            x,
            y,
            0xffdf80,
            true,
            entry.isCraftable() ? 11 : 0);
    }

    /**
     * Finds an entry at the given mouse position.
     */
    public RequestTableNetworkEntry getEntryAt(RequestTableLayout layout, int mouseX, int mouseY) {
        if (!inside(
            mouseX,
            mouseY,
            layout.panelLeft,
            layout.panelTop,
            layout.panelWidth,
            layout.getVisiblePanelRows() * RequestTableLayout.PANEL_CELL)) {
            return null;
        }
        List<RequestTableNetworkEntry> filtered = entries.getVisibleEntries();
        int columns = layout.getNetworkColumns();
        int column = (mouseX - layout.panelLeft) / RequestTableLayout.PANEL_CELL;
        int row = (mouseY - layout.panelTop) / RequestTableLayout.PANEL_CELL;
        if (column < 0 || row < 0 || column >= columns || row >= layout.getVisiblePanelRows()) {
            return null;
        }
        int index = (scrollRow + row) * columns + column;
        if (index < 0 || index >= filtered.size()) {
            return null;
        }
        return filtered.get(index);
    }

    public int getMaxScrollRow(RequestTableLayout layout) {
        int columns = layout.getNetworkColumns();
        int rows = (entries.getVisibleEntries().size() + columns - 1) / columns;
        return Math.max(0, rows - layout.getVisiblePanelRows());
    }
}
