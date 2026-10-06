package logisticspipes.crafting.requesttable;

/**
 * Pixel coordinates for the adaptive request table GUI.
 */
public class RequestTableLayout {

    public static final int SLOT = 18;
    public static final int PANEL_CELL = 18;
    public static final int INVENTORY_COLUMNS = 9;
    public static final int TAB_HEIGHT = 26;
    public static final int MAIN_TAB_WIDTH = 56;
    public static final int STORAGE_TAB_WIDTH = 64;
    public static final int UPGRADE_SLOT_COUNT = 9;

    private static final int PLAYER_HEIGHT = 76;
    private static final int BOTTOM_MARGIN = 8;

    public final int guiLeft;
    public final int guiTop;
    public final int xSize;
    public final int ySize;

    public final int searchX;
    public final int searchY;
    public final int searchWidth;
    public final int searchHeight;
    public final int sendButtonX;
    public final int sendButtonY;
    public final int sendButtonWidth;
    public final int sendButtonHeight;
    public final int itemButtonX;
    public final int fluidButtonX;
    public final int displayButtonX;
    public final int sortModeButtonY;
    public final int sortDirectionButtonY;
    public final int filterModeButtonY;
    public final int requestMessagesButtonY;
    public final int displayButtonWidth;
    public final int displayButtonHeight;
    public final int networkButtonY;
    public final int networkButtonX;
    public final int itemButtonY;
    public final int fluidButtonY;
    public final int centralLeft;
    public final int centralWidth;
    public final int centralTop;
    public final int upgradeLeft;
    public final int upgradeTop;
    public final int upgradePanelLeft;
    public final int upgradePanelWidth;
    public final int upgradePanelTop;
    public final int upgradePanelHeight;
    public final boolean compact;

    public final int panelLeft;
    public final int panelTop;
    public final int panelWidth;
    public final int panelHeight;
    public final int scrollbarX;

    public final int craftingLeft;
    public final int craftingTop;
    public final int craftingResultX;
    public final int craftingResultY;
    public final int craftingClearX;
    public final int craftingClearY;
    public final int craftingClearSize;
    public final int craftingAmountX;
    public final int craftingAmountY;
    public final int craftingAmountWidth;
    public final int craftingAmountHeight;
    public final int craftingRequestX;
    public final int craftingRequestY;
    public final int craftingRequestWidth;
    public final int craftingRequestHeight;

    public final int playerLeft;
    public final int playerTop;

    /**
     * Calculates all GUI coordinates from the current screen bounds.
     */
    public RequestTableLayout(int guiLeft, int guiTop, int xSize, int ySize) {
        this.guiLeft = guiLeft;
        this.guiTop = guiTop;
        this.xSize = xSize;
        this.ySize = ySize;

        compact = ySize < 268;
        centralLeft = guiLeft + 34;
        centralWidth = 190;
        centralTop = guiTop + 22;
        networkButtonX = centralLeft + 1;
        itemButtonX = networkButtonX + MAIN_TAB_WIDTH + 2;
        fluidButtonX = itemButtonX + STORAGE_TAB_WIDTH + 2;
        networkButtonY = guiTop;
        itemButtonY = guiTop;
        fluidButtonY = guiTop;

        displayButtonWidth = 20;
        displayButtonHeight = 20;
        displayButtonX = guiLeft + 7;
        sortModeButtonY = centralTop + 8;
        sortDirectionButtonY = sortModeButtonY + 24;
        filterModeButtonY = sortDirectionButtonY + 24;
        requestMessagesButtonY = filterModeButtonY + 24;

        searchHeight = compact ? 12 : 16;
        searchY = guiTop + (compact ? 27 : 28);
        searchX = centralLeft + 6;
        searchWidth = 176;

        sendButtonHeight = 20;
        sendButtonWidth = 20;
        sendButtonX = displayButtonX;
        sendButtonY = sortModeButtonY;

        playerLeft = centralLeft + 14;
        playerTop = guiTop + ySize - PLAYER_HEIGHT - BOTTOM_MARGIN;

        craftingTop = playerTop - (compact ? 80 : 102);
        craftingLeft = centralLeft + 6;
        craftingResultX = centralLeft + 78;
        craftingResultY = craftingTop + (compact ? 30 : 37);
        craftingClearSize = 12;
        craftingClearX = centralLeft + 67;
        craftingClearY = craftingTop;
        craftingAmountWidth = 30;
        craftingAmountHeight = 16;
        craftingAmountX = centralLeft + 114;
        craftingAmountY = craftingResultY;
        craftingRequestWidth = 28;
        craftingRequestHeight = 20;
        craftingRequestX = craftingAmountX + craftingAmountWidth + 4;
        craftingRequestY = craftingResultY - 2;

        panelLeft = playerLeft - 1;
        panelTop = guiTop + (compact ? 42 : 50);
        panelWidth = INVENTORY_COLUMNS * SLOT;
        panelHeight = Math.max(PANEL_CELL, (craftingTop - panelTop - 4) / PANEL_CELL * PANEL_CELL);
        scrollbarX = panelLeft + panelWidth + 3;

        upgradePanelLeft = centralLeft + centralWidth + 6;
        upgradePanelWidth = 28;
        upgradePanelTop = centralTop;
        upgradePanelHeight = UPGRADE_SLOT_COUNT * SLOT + 10;
        upgradeLeft = upgradePanelLeft + 6;
        upgradeTop = upgradePanelTop + 6;
    }

    /**
     * @return number of complete network rows that fit into the adaptive panel
     */
    public int getVisiblePanelRows() {
        return Math.max(1, panelHeight / PANEL_CELL);
    }

    /**
     * @return number of item cells per row in the network panel
     */
    public int getNetworkColumns() {
        return INVENTORY_COLUMNS;
    }

    /**
     * @return number of complete storage rows that fit into the adaptive panel
     */
    public int getVisibleStorageRows() {
        return Math.max(1, panelHeight / SLOT);
    }

    public int getCraftingGridTop() {
        return craftingTop + (compact ? 11 : 18);
    }

    public int getCraftableLabelY() {
        return craftingTop + (compact ? 68 : 77);
    }

    /** Packs the upgrade slots in one column along the detached bar. */
    public int getUpgradeSlotY(int index) {
        return upgradeTop + index * SLOT;
    }
}
