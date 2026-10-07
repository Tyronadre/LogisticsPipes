package logisticspipes.crafting.requesttable;

import codechicken.nei.LayoutManager;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import logisticspipes.crafting.requesttable.RequestTableIconButton.Icon;
import logisticspipes.gui.popup.GuiRequestPopup;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.crafting.requesttable.RequestTableClearCraftingPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableDisplaySettingsPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableNetworkInteractPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableRefreshPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableRequestIngredientsPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableSendStoragePacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableShiftClickPacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableSubmitPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.GuiSearchBar;
import logisticspipes.utils.gui.ISubGuiControler;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.string.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Redesigned request table GUI with a combined item/fluid network list, internal storage views and fake crafting grid.
 */
@SideOnly(Side.CLIENT)
public class RequestTableGui extends LogisticsBaseGuiScreen {

    private static final int SEND_BUTTON = 0;
    private static final int REQUEST_INGREDIENTS_BUTTON = 1;
    private static final int SORT_MODE_BUTTON = 2;
    private static final int SORT_DIRECTION_BUTTON = 3;
    private static final int FILTER_MODE_BUTTON = 4;
    private static final int ITEM_VIEW_BUTTON = 5;
    private static final int FLUID_VIEW_BUTTON = 6;
    private static final int NETWORK_VIEW_BUTTON = 7;
    private static final int CLEAR_CRAFTING_BUTTON = 8;
    private static final int REQUEST_MESSAGES_BUTTON = 9;
    private static final int SEARCH_MODE_BUTTON = 10;
    private static final int SAVE_SEARCH_BUTTON = 11;
    private static final int TERMINAL_STYLE_BUTTON = 12;
    private static final int SHOW_ITEMS_BUTTON = 13;
    private static final int SHOW_FLUIDS_BUTTON = 14;
    private static final ResourceLocation UPGRADE_SLOT = new ResourceLocation(
        "logisticspipes",
        "textures/gui/upgrade_slot.png");
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(
        "logisticspipes",
        "textures/gui/GuiBackground.png");

    private final RequestTablePipe table;
    private final EntityPlayer player;
    private final RequestTableContainer container;
    private final RequestTableNetworkGrid networkGrid = new RequestTableNetworkGrid();
    private final RequestTableRequestOverlay requestOverlay = new RequestTableRequestOverlay();
    private final RequestTableItemRenderer storageItemRenderer = new RequestTableItemRenderer();

    private RequestTableLayout layout;
    private GuiSearchBar search;
    private GuiTextField ingredientAmountField;
    private RequestTableIconButton sendButton;
    private RequestTableIconButton requestIngredientsButton;
    private RequestTableIconButton sortModeButton;
    private RequestTableIconButton sortDirectionButton;
    private RequestTableIconButton filterModeButton;
    private RequestTableIconButton requestMessagesButton;
    private RequestTableIconButton searchModeButton;
    private RequestTableIconButton saveSearchButton;
    private RequestTableIconButton terminalStyleButton;
    private RequestTableIconButton showItemsButton;
    private RequestTableIconButton showFluidsButton;
    private RequestTableIconButton networkButton;
    private RequestTableIconButton itemButton;
    private RequestTableIconButton fluidButton;
    private RequestTableIconButton clearCraftingButton;
    private RequestTableView view = RequestTableView.NETWORK;
    private RequestTableDisplaySettings displaySettings = RequestTableDisplaySettings.DEFAULT;
    private int storageScrollRow;
    private boolean scrollbarDragging;
    private int scrollbarDragOffset;
    private boolean settingsReceived;
    private boolean searchInitialized;
    private boolean searchEdited;
    private boolean searchFocusChosen;
    private boolean restoringSearch;
    private final int dimension;

    /**
     * Creates the client GUI for the new request table.
     */
    public RequestTableGui(EntityPlayer player, RequestTablePipe table) {
        super(new RequestTableContainer(player, table), RequestTableLayout.GUI_WIDTH, 300, 0, 0);
        this.player = player;
        this.table = table;
        this.container = (RequestTableContainer) inventorySlots;
        dimension = MainProxy.getDimensionForWorld(table.getWorld());
        refreshNetwork();
    }

    @Override
    public void initGui() {
        xSize = RequestTableLayout.GUI_WIDTH;
        ySize = RequestTableLayout.getGuiHeight(height, displaySettings.getTerminalStyle());
        super.initGui();
        updateLayout();
        buttonList.clear();
        sendButton = new RequestTableIconButton(
                SEND_BUTTON,
                layout.sendButtonX,
                layout.sendButtonY,
                layout.sendButtonWidth,
                layout.sendButtonHeight,
            Icon.SEND);
        buttonList.add(sendButton);
        requestIngredientsButton = new RequestTableIconButton(
                REQUEST_INGREDIENTS_BUTTON,
                layout.craftingRequestX,
                layout.craftingRequestY,
                layout.craftingRequestWidth,
                layout.craftingRequestHeight,
            Icon.REQUEST);
        buttonList.add(requestIngredientsButton);
        sortModeButton = new RequestTableIconButton(
                SORT_MODE_BUTTON,
                layout.displayButtonX,
                layout.sortModeButtonY,
                layout.displayButtonWidth,
                layout.displayButtonHeight,
            Icon.NAME);
        buttonList.add(sortModeButton);
        sortDirectionButton = new RequestTableIconButton(
                SORT_DIRECTION_BUTTON,
                layout.displayButtonX,
                layout.sortDirectionButtonY,
                layout.displayButtonWidth,
                layout.displayButtonHeight,
            Icon.ASCENDING);
        buttonList.add(sortDirectionButton);
        filterModeButton = new RequestTableIconButton(
                FILTER_MODE_BUTTON,
            layout.visibilityButtonX,
                layout.filterModeButtonY,
                layout.displayButtonWidth,
                layout.displayButtonHeight,
            Icon.BOTH);
        buttonList.add(filterModeButton);
        requestMessagesButton = new RequestTableIconButton(
            REQUEST_MESSAGES_BUTTON,
            layout.displayButtonX,
            layout.requestMessagesButtonY,
            layout.displayButtonWidth,
            layout.displayButtonHeight,
            Icon.MESSAGES_ON);
        buttonList.add(requestMessagesButton);
        searchModeButton = addSidebarButton(SEARCH_MODE_BUTTON, Icon.SEARCH_STANDARD);
        saveSearchButton = addSidebarButton(SAVE_SEARCH_BUTTON, Icon.SAVE_SEARCH_OFF);
        terminalStyleButton = addSidebarButton(TERMINAL_STYLE_BUTTON, Icon.TERMINAL_SMALL);
        showItemsButton = addSidebarButton(SHOW_ITEMS_BUTTON, Icon.ITEMS_ON);
        showFluidsButton = addSidebarButton(SHOW_FLUIDS_BUTTON, Icon.FLUIDS_ON);
        networkButton = new RequestTableIconButton(
            NETWORK_VIEW_BUTTON,
            layout.networkButtonX,
            layout.networkButtonY,
            RequestTableLayout.MAIN_TAB_WIDTH,
            RequestTableLayout.TAB_HEIGHT,
            Icon.NETWORK);
        networkButton.setTabLabel("Main");
        itemButton = new RequestTableIconButton(
            ITEM_VIEW_BUTTON,
            layout.itemButtonX,
            layout.itemButtonY,
            RequestTableLayout.STORAGE_TAB_WIDTH,
            RequestTableLayout.TAB_HEIGHT,
            Icon.STORED);
        itemButton.setTabLabel("Items");
        itemButton.setItem(new ItemStack(Blocks.chest));
        fluidButton = new RequestTableIconButton(
            FLUID_VIEW_BUTTON,
            layout.fluidButtonX,
            layout.fluidButtonY,
            RequestTableLayout.STORAGE_TAB_WIDTH,
            RequestTableLayout.TAB_HEIGHT,
            Icon.STORED);
        fluidButton.setTabLabel("Fluids");
        Block tank = GameRegistry.findBlock("BuildCraft|Factory", "tankBlock");
        fluidButton.setItem(tank == null ? new ItemStack(Items.water_bucket) : new ItemStack(tank));
        clearCraftingButton = new RequestTableIconButton(
            CLEAR_CRAFTING_BUTTON,
            layout.craftingClearX,
            layout.craftingClearY,
            layout.craftingClearSize,
            layout.craftingClearSize,
            Icon.CLEAR);
        buttonList.add(networkButton);
        buttonList.add(itemButton);
        buttonList.add(fluidButton);
        buttonList.add(clearCraftingButton);
        updateDisplayButtons();
        if (search == null) {
            search = new GuiSearchBar("new_request_table_search") {

                @Override
                public void reposition(int left, int top, int width, int height) {
                    super.reposition(left, top, width, height);
                    setDimensionsAndColor();
                }

                @Override
                protected void setDimensionsAndColor() {
                    field.setEnableBackgroundDrawing(false);
                    field.setCanLoseFocus(false);
                    field.xPosition = x + 3;
                    field.yPosition = y + Math.max(1, (h - 8) / 2);
                    field.width = w - 6;
                    field.height = 8;
                    field.setTextColor(0xffe0e0e0);
                }

                @Override
                public void draw(int mouseX, int mouseY) {
                    RequestTableGuiStyle.inset(x, y, w, h);
                    if (isFocused()) {
                        Gui.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0xff555555);
                    }
                    super.draw(mouseX, mouseY);
                }

                @Override
                public void setText(String text) {
                    super.setText(text);
                    field.setCursorPositionEnd();
                }

                @Override
                public void onTextChange(String oldText) {
                    handleSearchTextChange(oldText);
                }
            };
        }
        search.reposition(layout.searchX, layout.searchY, layout.searchWidth, layout.searchHeight);
        initializeSearch();
        initIngredientAmountField();
        updateContainerLayout();
        updateDisplayButtonLayout();
    }

    private RequestTableIconButton addSidebarButton(int id, Icon icon) {
        RequestTableIconButton button = new RequestTableIconButton(
            id,
            layout.displayButtonX,
            layout.sortModeButtonY,
            layout.displayButtonWidth,
            layout.displayButtonHeight,
            icon);
        buttonList.add(button);
        return button;
    }

    /**
     * Receives the server-built network list.
     */
    public void handleNetworkContent(List<RequestTableNetworkEntry> entries,
            RequestTableDisplaySettings displaySettings) {
        boolean styleChanged = this.displaySettings.getTerminalStyle() != displaySettings.getTerminalStyle();
        this.displaySettings = displaySettings;
        settingsReceived = true;
        if (styleChanged && mc != null) {
            initGui();
        }
        initializeSearch();
        networkGrid.setDisplaySettings(displaySettings);
        networkGrid.setEntries(entries);
        requestOverlay.updateEntry(entries);
        updateDisplayButtons();
    }

    /**
     * Prevents a delayed content packet from another request table from replacing this GUI's state.
     */
    public boolean isForTable(int x, int y, int z) {
        return table.container != null && table.container.xCoord == x
                && table.container.yCoord == y
                && table.container.zCoord == z;
    }

    /**
     * @return backing pipe for integration points such as NEI recipe overlays
     */
    public RequestTablePipe getTable() {
        return table;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        updateLayout();
        updateContainerLayout();
        sendButton.enabled = view != RequestTableView.NETWORK && container.isInventoryReady();
        sendButton.visible = sendButton.enabled;
        sendButton.xPosition = layout.sendButtonX;
        sendButton.yPosition = layout.sendButtonY;
        sendButton.width = layout.sendButtonWidth;
        sendButton.height = layout.sendButtonHeight;
        requestIngredientsButton.xPosition = layout.craftingRequestX;
        requestIngredientsButton.yPosition = layout.craftingRequestY;
        requestIngredientsButton.width = layout.craftingRequestWidth;
        requestIngredientsButton.height = layout.craftingRequestHeight;
        requestIngredientsButton.visible = true;
        requestIngredientsButton.enabled = container.isInventoryReady() && hasCraftingInputs();
        updateDisplayButtonLayout();
        networkButton.setSelected(view == RequestTableView.NETWORK);
        itemButton.setSelected(view == RequestTableView.ITEM_STORAGE);
        fluidButton.setSelected(view == RequestTableView.FLUID_STORAGE);
        itemButton.setFill(table.getItemStorageFillLevel(), 0xffcf9d53);
        fluidButton.setFill(table.getFluidStorageFillLevel(), 0xff58a9cf);
        clearCraftingButton.xPosition = layout.craftingClearX;
        clearCraftingButton.yPosition = layout.craftingClearY;
        clearCraftingButton.enabled = hasCraftingInputs();

        networkButton.drawBehindPanel(mc, mouseX, mouseY);
        itemButton.drawBehindPanel(mc, mouseX, mouseY);
        fluidButton.drawBehindPanel(mc, mouseX, mouseY);
        GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GuiGraphics.drawGuiBackGround(
            mc,
            layout.centralLeft,
            layout.centralTop,
            layout.centralLeft + layout.centralWidth,
            bottom,
            zLevel,
            true);
        GL11.glPopAttrib();
        drawHeader();
        drawMainPanel(mouseX, mouseY);
        drawCraftingArea();
        drawUpgradePanel();
        if (!layout.compact) {
            mc.fontRenderer
                .drawString("Inventory", layout.playerLeft, layout.playerTop - 12, RequestTableGuiStyle.TEXT);
        }
        GuiGraphics.drawPlayerInventoryBackground(mc, layout.playerLeft, layout.playerTop);
    }

    private void drawHeader() {
        if (view == RequestTableView.NETWORK) {
            search.reposition(layout.searchX, layout.searchY, layout.searchWidth, layout.searchHeight);
            search.renderSearchBar();
        } else {
            String title = view == RequestTableView.ITEM_STORAGE ? "Item storage" : "Fluid storage";
            mc.fontRenderer.drawString(
                title,
                layout.searchX,
                layout.searchY + (layout.searchHeight - 8) / 2,
                RequestTableGuiStyle.TEXT);
        }
    }

    private void drawMainPanel(int mouseX, int mouseY) {
        if (view == RequestTableView.NETWORK) {
            networkGrid.setSearch(getSearchText());
            networkGrid.render(this, layout, mouseX, mouseY);
            return;
        }
        drawStoragePanel();
    }

    private void drawStoragePanel() {
        int columns = RequestTableLayout.INVENTORY_COLUMNS;
        int size = view == RequestTableView.ITEM_STORAGE ? table.inv.getSizeInventory()
                : table.getFluidStorage().getSizeInventory();
        int rows = (size + columns - 1) / columns;
        int visibleRows = layout.getVisibleStorageRows();
        int maxScroll = Math.max(0, rows - visibleRows);
        int storageTop = layout.panelTop + 1;
        int storageBottom = storageTop + visibleRows * RequestTableLayout.SLOT;
        drawStorageScrollbar(maxScroll);
        for (int i = 0; i < size; i++) {
            int x = layout.panelLeft + 1 + (i % columns) * RequestTableLayout.SLOT;
            int y = layout.panelTop + 1 + (i / columns - storageScrollRow) * RequestTableLayout.SLOT;
            if (y < storageTop || y + RequestTableLayout.SLOT > storageBottom) {
                continue;
            }
            GuiGraphics.drawSlotBackground(mc, x - 1, y - 1);
        }
    }

    private void drawStorageScrollbar(int maxScroll) {
        RequestTableGuiStyle.scrollbar(layout, storageScrollRow, maxScroll);
    }

    private void drawCraftingArea() {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                GuiGraphics.drawSlotBackground(
                    mc,
                    layout.craftingLeft + x * 18 - 1,
                    layout.getCraftingGridTop() + y * 18 - 1);
            }
        }
        GuiGraphics.drawBigSlotBackground(mc, layout.craftingResultX - 5, layout.craftingResultY - 5);
        int arrowX = layout.craftingLeft + 54;
        int arrowY = layout.craftingResultY + 7;
        Gui.drawRect(arrowX, arrowY, arrowX + 10, arrowY + 3, 0xff707070);
        for (int i = 0; i < 5; i++) {
            Gui.drawRect(arrowX + 8 + i, arrowY - 4 + i, arrowX + 9 + i, arrowY + 7 - i, 0xff707070);
        }
        mc.fontRenderer
            .drawString("Sets", layout.craftingAmountX, layout.craftingAmountY - 12, RequestTableGuiStyle.TEXT);
        if (ingredientAmountField != null) {
            ingredientAmountField.drawTextBox();
        }
        String amount = StringUtils.getFormatedStackSize(container.getCraftableAmount(), true);
        mc.fontRenderer.drawString(
            "Craftable: " + amount,
            layout.getCraftableLabelX(),
            layout.getCraftableLabelY(),
            container.getCraftableAmount() > 0 ? 0x37622e : RequestTableGuiStyle.MUTED);
    }

    private boolean hasCraftingInputs() {
        for (int i = 0; i < table.matrix.getSizeInventory(); i++) {
            if (table.matrix.getStackInSlot(i) != null) {
                return true;
            }
        }
        return false;
    }

    private void drawUpgradePanel() {
        GuiGraphics.drawNineSlice(
            mc,
            layout.upgradePanelLeft,
            layout.upgradePanelTop,
            layout.upgradePanelWidth,
            layout.upgradePanelHeight,
            GUI_BACKGROUND,
            45,
            4,
            zLevel);
        int slots = table.getOriginalUpgradeManager().getInv().getSizeInventory();
        for (int i = 0; i < slots; i++) {
            GuiGraphics.drawSlotBackground(mc, layout.upgradeLeft - 1, layout.getUpgradeSlotY(i) - 1, UPGRADE_SLOT);
        }
    }

    @Override
    protected void func_146977_a(Slot slot) {
        if (slot.inventory != table.inv && slot.inventory != table.getFluidStorage()) {
            super.func_146977_a(slot);
            return;
        }
        // Scope the count renderer to this slot so vanilla retains its drag and touchscreen previews.
        RenderItem previousRenderer = itemRender;
        storageItemRenderer.configure(previousRenderer, slot instanceof RequestTableFluidSlot);
        itemRender = storageItemRenderer;
        try {
            super.func_146977_a(slot);
        } finally {
            itemRender = previousRenderer;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (scrollbarDragging && (!Mouse.isButtonDown(0) || requestOverlay.isOpen() || hasSubGui())) {
            scrollbarDragging = false;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (requestOverlay.isOpen()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            Gui.drawRect(0, 0, width, height, 0x66000000);
            requestOverlay.render(this, requestOverlay.getEntry().getInternalAmount());
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else if (!hasSubGui()) {
            drawDisplayButtonTooltip(mouseX, mouseY);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
        if (view == RequestTableView.NETWORK && !hasSubGui() && !requestOverlay.isOpen()) {
            GuiGraphics.displayItemToolTip(networkGrid.getTooltip(), this, zLevel, guiLeft, guiTop);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == SEND_BUTTON) {
            int mode = view == RequestTableView.FLUID_STORAGE ? 1 : 0;
            MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(RequestTableSendStoragePacket.class).setInteger(mode)
                            .setTilePos(table.container));
        } else if (button.id == REQUEST_INGREDIENTS_BUTTON) {
            requestCraftingIngredients();
        } else if (button.id == SORT_MODE_BUTTON) {
            applyDisplaySettings(displaySettings.nextSortMode());
        } else if (button.id == SORT_DIRECTION_BUTTON) {
            applyDisplaySettings(displaySettings.nextSortDirection());
        } else if (button.id == FILTER_MODE_BUTTON) {
            applyDisplaySettings(displaySettings.nextFilterMode());
        } else if (button.id == REQUEST_MESSAGES_BUTTON) {
            applyDisplaySettings(displaySettings.toggleRequestMessages());
        } else if (button.id == SEARCH_MODE_BUTTON) {
            applyDisplaySettings(displaySettings.nextSearchBoxMode());
        } else if (button.id == SAVE_SEARCH_BUTTON) {
            applyDisplaySettings(displaySettings.toggleSaveSearch());
        } else if (button.id == TERMINAL_STYLE_BUTTON) {
            applyDisplaySettings(displaySettings.nextTerminalStyle());
        } else if (button.id == SHOW_ITEMS_BUTTON) {
            applyDisplaySettings(displaySettings.toggleItems());
        } else if (button.id == SHOW_FLUIDS_BUTTON) {
            applyDisplaySettings(displaySettings.toggleFluids());
        } else if (button.id == NETWORK_VIEW_BUTTON) {
            setView(RequestTableView.NETWORK);
        } else if (button.id == ITEM_VIEW_BUTTON) {
            setView(RequestTableView.ITEM_STORAGE);
        } else if (button.id == FLUID_VIEW_BUTTON) {
            setView(RequestTableView.FLUID_STORAGE);
        } else if (button.id == CLEAR_CRAFTING_BUTTON) {
            table.clearCraftingGrid();
            MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(RequestTableClearCraftingPacket.class).setTilePos(table.container));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (scrollbarDragging) {
            return;
        }
        if (requestOverlay.mouseClicked(mouseX, mouseY, button, this::submitOverlayRequest)) {
            return;
        }
        searchFocusChosen = true;
        if (search != null) {
            search.onGuiClick(mouseX, mouseY);
        }
        if (ingredientAmountField != null) {
            ingredientAmountField.mouseClicked(mouseX, mouseY, button);
            if (inside(
                    mouseX,
                    mouseY,
                    layout.craftingAmountX,
                    layout.craftingAmountY,
                    layout.craftingAmountWidth,
                    layout.craftingAmountHeight)) {
                return;
            }
        }
        if (inside(mouseX, mouseY, layout.scrollbarX, layout.panelTop, 7, layout.panelHeight)) {
            if (button == 0) {
                startScrollbarDrag(mouseY);
            }
            return;
        }
        if (view == RequestTableView.NETWORK) {
            if (inside(mouseX, mouseY, layout.searchX, layout.searchY, layout.searchWidth, layout.searchHeight)) {
                focusSearch(true);
                search.handleClick(mouseX, mouseY, button);
                return;
            }
            networkGrid.setSearch(getSearchText());
            RequestTableNetworkEntry entry = networkGrid.getEntryAt(layout, mouseX, mouseY);
            if ((button == 0 || button == 1) && !shouldOpenRequestOverlay(button)
                && mc.thePlayer.inventory.getItemStack() != null
                && inside(
                mouseX,
                mouseY,
                layout.panelLeft,
                layout.panelTop,
                layout.panelWidth,
                layout.panelHeight)) {
                MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(RequestTableNetworkInteractPacket.class).setCursorInteraction(true)
                        .setFluid(entry != null && entry.isFluid()).setMouseButton(button)
                        .setShift(isShiftDown()).setStack(entry == null ? null : entry.getStack())
                        .setTilePos(table.container));
                return;
            }
            if (entry != null) {
                if (shouldOpenRequestOverlay(button)) {
                    requestOverlay.open(entry, mc.fontRenderer, width, height, button == 0 ? 64 : 1);
                } else if (button == 0 || button == 1) {
                    MainProxy.sendPacketToServer(
                            PacketHandler.getPacket(RequestTableNetworkInteractPacket.class).setFluid(entry.isFluid())
                                .setMouseButton(button).setShift(isShiftDown()).setStack(entry.getStack())
                                .setTilePos(table.container));
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        if (scrollbarDragging) {
            if (button == 0) {
                dragScrollbar(mouseY);
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (scrollbarDragging) {
            if (button == 0) {
                scrollbarDragging = false;
            }
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    private void startScrollbarDrag(int mouseY) {
        int maxScroll = getMaxScrollRow();
        if (maxScroll == 0) {
            return;
        }
        int scrollRow = view == RequestTableView.NETWORK ? networkGrid.getScrollRow() : storageScrollRow;
        int thumbHeight = RequestTableGuiStyle.getScrollbarThumbHeight(layout, maxScroll);
        int thumbTop = RequestTableGuiStyle.getScrollbarThumbTop(layout, Math.min(scrollRow, maxScroll), maxScroll);
        boolean onThumb = mouseY >= thumbTop && mouseY < thumbTop + thumbHeight;
        scrollbarDragOffset = onThumb ? mouseY - thumbTop : thumbHeight / 2;
        scrollbarDragging = true;
        if (!onThumb) {
            dragScrollbar(mouseY);
        }
    }

    private void dragScrollbar(int mouseY) {
        int maxScroll = getMaxScrollRow();
        int travel = layout.panelHeight - RequestTableGuiStyle.getScrollbarThumbHeight(layout, maxScroll);
        int position = Math.max(0, Math.min(travel, mouseY - layout.panelTop - scrollbarDragOffset));
        int row = travel <= 0 ? 0 : Math.round((float) position * maxScroll / travel);
        if (view == RequestTableView.NETWORK) {
            networkGrid.setScrollRow(row, layout);
        } else {
            storageScrollRow = row;
            updateContainerLayout();
        }
    }

    private int getMaxScrollRow() {
        return view == RequestTableView.NETWORK ? networkGrid.getMaxScrollRow(layout) : getMaxStorageScrollRow();
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, int mode) {
        // Wait for the server's layout before sending container slot indexes.
        if (container.isInventoryReady()) {
            if (mode == 1 && slot != null && slot.inventory == player.inventory) {
                container.awaitInventory();
                MainProxy.sendPacketToServer(
                    PacketHandler.getPacket(RequestTableShiftClickPacket.class)
                        .setClick(container.windowId, slot.getSlotIndex(), view));
                return;
            }
            if ((slot != null && slot.inventory == table.getOriginalUpgradeManager().getInv() && mode != 5)
                || (mode == 5 && (mouseButton & 3) == 2)) {
                container.awaitInventory();
            }
            super.handleMouseClick(slot, slotId, mouseButton, mode);
        }
    }

    @Override
    protected void keyTyped(char typed, int keyCode) {
        if (requestOverlay.keyTyped(typed, keyCode, this::submitOverlayRequest)) {
            return;
        }
        if (ingredientAmountField != null && ingredientAmountField.isFocused()) {
            if (keyCode == 1) {
                ingredientAmountField.setFocused(false);
                return;
            }
            if (keyCode == 28 || keyCode == 156) {
                requestCraftingIngredients();
                return;
            }
            ingredientAmountField.textboxKeyTyped(typed, keyCode);
            sanitizeIngredientAmountField();
            return;
        }
        if (view == RequestTableView.NETWORK && search.handleKey(typed, keyCode)) {
            networkGrid.setSearch(getSearchText());
            return;
        }
        super.keyTyped(typed, keyCode);
    }

    @Override
    public void handleMouseInputSub() {
        if (requestOverlay.isOpen()) {
            super.handleMouseInputSub();
            Mouse.getEventDWheel();
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && !scrollbarDragging) {
            int rows = wheel > 0 ? -1 : 1;
            if (view == RequestTableView.NETWORK) {
                networkGrid.setSearch(getSearchText());
                networkGrid.scroll(rows, layout);
            } else {
                storageScrollRow += rows;
                clampStorageScroll();
            }
        }
        super.handleMouseInputSub();
    }

    @Override
    public void resetSubGui() {
        super.resetSubGui();
        refreshNetwork();
    }

    /**
     * Opens the normal request-result popup for this GUI.
     */
    public void handleRequestAnswer(Collection<IResource> items, boolean error, ISubGuiControler control,
            EntityPlayer player) {
        while (control.hasSubGui()) {
            control = control.getSubGui();
        }
        if (error) {
            control.setSubGui(new GuiRequestPopup(player, "You are missing:", items));
        } else {
            control.setSubGui(new GuiRequestPopup(player, "Request successful!", items));
        }
    }

    private void submitOverlayRequest() {
        if (!requestOverlay.isOpen() || !requestOverlay.hasValidAmount()) {
            return;
        }
        RequestTableNetworkEntry entry = requestOverlay.getEntry();
        MainProxy.sendPacketToServer(
                PacketHandler.getPacket(RequestTableSubmitPacket.class).setFluid(entry.isFluid())
                        .setDimension(dimension)
                        .setStack(entry.getStack().getItem().makeStack(requestOverlay.getAmount()))
                        .setTilePos(table.container));
        requestOverlay.close();
        refreshNetwork();
    }

    private void setView(RequestTableView newView) {
        if (view == newView) {
            return;
        }
        view = newView;
        scrollbarDragging = false;
        if (view != RequestTableView.NETWORK) {
            search.setFocus(false);
        }
        storageScrollRow = 0;
        if (view == RequestTableView.NETWORK) {
            refreshNetwork();
        }
        updateContainerLayout();
    }

    private void refreshNetwork() {
        MainProxy.sendPacketToServer(
                PacketHandler.getPacket(RequestTableRefreshPacket.class).setInteger(dimension)
                        .setTilePos(table.container));
    }

    private void applyDisplaySettings(RequestTableDisplaySettings newSettings) {
        boolean styleChanged = displaySettings.getTerminalStyle() != newSettings.getTerminalStyle();
        boolean searchModeChanged = displaySettings.getSearchBoxMode() != newSettings.getSearchBoxMode();
        displaySettings = newSettings.rememberSearch(getSearchText());
        networkGrid.setDisplaySettings(displaySettings);
        if (styleChanged) {
            initGui();
        }
        if (searchModeChanged) {
            focusSearch(displaySettings.getSearchBoxMode().isAutoFocus());
            syncNeiSearch();
        }
        updateDisplayButtons();
        sendDisplaySettings();
    }

    private void sendDisplaySettings() {
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(RequestTableDisplaySettingsPacket.class).setSettings(displaySettings)
                        .setTilePos(table.container));
    }

    private void initializeSearch() {
        if (search == null || !settingsReceived || searchInitialized) {
            return;
        }
        if (!searchEdited) {
            restoringSearch = true;
            search.setText(displaySettings.isSaveSearch() ? displaySettings.getSavedSearchText() : "");
            restoringSearch = false;
        }
        if (!searchFocusChosen) {
            focusSearch(displaySettings.getSearchBoxMode().isAutoFocus());
        }
        searchInitialized = true;
        syncNeiSearch();
    }

    private void focusSearch(boolean focused) {
        boolean focusTerminal = focused && view == RequestTableView.NETWORK;
        if (focusTerminal && LayoutManager.searchField != null) {
            LayoutManager.searchField.setFocus(false);
        }
        search.setFocus(focusTerminal);
    }

    private void handleSearchTextChange(String oldText) {
        if (oldText.equals(getSearchText())) {
            return;
        }
        networkGrid.setSearch(getSearchText());
        if (!restoringSearch) {
            searchEdited = true;
            syncNeiSearch();
        }
    }

    /** Only terminal edits are sent to NEI; changes made in NEI never replace terminal text. */
    private void syncNeiSearch() {
        if (displaySettings.getSearchBoxMode().isNeiSynced() && LayoutManager.searchField != null
            && !getSearchText().equals(LayoutManager.searchField.text())) {
            LayoutManager.searchField.setText(getSearchText());
        }
    }

    @Override
    public void onGuiClosed() {
        scrollbarDragging = false;
        if (search != null) {
            search.setFocus(false);
        }
        if (settingsReceived) {
            RequestTableDisplaySettings saved = displaySettings.rememberSearch(getSearchText());
            if (!saved.equals(displaySettings)) {
                displaySettings = saved;
                sendDisplaySettings();
            }
        }
        super.onGuiClosed();
    }

    private void updateDisplayButtons() {
        if (sortModeButton == null) {
            return;
        }
        requestMessagesButton
            .setIcon(displaySettings.isRequestMessagesEnabled() ? Icon.MESSAGES_ON : Icon.MESSAGES_OFF);
        requestMessagesButton.setSelected(displaySettings.isRequestMessagesEnabled());
        switch (displaySettings.getSearchBoxMode()) {
            case AUTO:
                searchModeButton.setIcon(Icon.SEARCH_AUTO);
                break;
            case NEI_SYNC_AUTO:
                searchModeButton.setIcon(Icon.SEARCH_NEI_AUTO);
                break;
            case NEI_SYNC_STANDARD:
                searchModeButton.setIcon(Icon.SEARCH_NEI_STANDARD);
                break;
            case STANDARD:
            default:
                searchModeButton.setIcon(Icon.SEARCH_STANDARD);
                break;
        }
        saveSearchButton.setIcon(displaySettings.isSaveSearch() ? Icon.SAVE_SEARCH_ON : Icon.SAVE_SEARCH_OFF);
        saveSearchButton.setSelected(displaySettings.isSaveSearch());
        boolean tall = displaySettings.getTerminalStyle() == RequestTableDisplaySettings.TerminalStyle.TALL;
        terminalStyleButton.setIcon(tall ? Icon.TERMINAL_TALL : Icon.TERMINAL_SMALL);
        terminalStyleButton.setSelected(tall);
        showItemsButton.setIcon(displaySettings.isShowItems() ? Icon.ITEMS_ON : Icon.ITEMS_OFF);
        showItemsButton.setSelected(displaySettings.isShowItems());
        showFluidsButton.setIcon(displaySettings.isShowFluids() ? Icon.FLUIDS_ON : Icon.FLUIDS_OFF);
        showFluidsButton.setSelected(displaySettings.isShowFluids());
        sortModeButton.setIcon(
            displaySettings.getSortMode() == RequestTableDisplaySettings.SortMode.NAME ? Icon.NAME : Icon.AMOUNT);
        sortDirectionButton.setIcon(
            displaySettings.getSortDirection() == RequestTableDisplaySettings.SortDirection.ASCENDING
                ? Icon.ASCENDING
                : Icon.DESCENDING);
        switch (displaySettings.getFilterMode()) {
            case STORED:
                filterModeButton.setIcon(Icon.STORED);
                break;
            case CRAFTABLE:
                filterModeButton.setIcon(Icon.CRAFTABLE);
                break;
            case BOTH:
            default:
                filterModeButton.setIcon(Icon.BOTH);
                break;
        }
    }

    private void updateDisplayButtonLayout() {
        boolean enabled = view == RequestTableView.NETWORK && settingsReceived;
        sortModeButton.enabled = enabled;
        sortDirectionButton.enabled = enabled;
        filterModeButton.enabled = enabled;
        searchModeButton.enabled = enabled;
        saveSearchButton.enabled = enabled;
        showItemsButton.enabled = enabled;
        showFluidsButton.enabled = enabled;
        terminalStyleButton.enabled = settingsReceived;
        requestMessagesButton.enabled = settingsReceived;
        sendButton.enabled = view != RequestTableView.NETWORK && container.isInventoryReady();
        layoutSidebarColumn(
            layout.displayButtonX,
            sendButton,
            sortModeButton,
            sortDirectionButton,
            searchModeButton,
            saveSearchButton,
            terminalStyleButton,
            requestMessagesButton);
        layoutSidebarColumn(layout.visibilityButtonX, filterModeButton, showItemsButton, showFluidsButton);
    }

    private void layoutSidebarColumn(int x, RequestTableIconButton... buttons) {
        int top = layout.sortModeButtonY;
        for (RequestTableIconButton button : buttons) {
            button.visible = button.enabled;
            if (button.visible) {
                button.xPosition = x;
                button.yPosition = top;
                button.width = layout.displayButtonWidth;
                button.height = layout.displayButtonHeight;
                top += layout.displayButtonHeight + RequestTableLayout.SIDEBAR_ROW_GAP;
            }
        }
    }

    private void drawDisplayButtonTooltip(int mouseX, int mouseY) {
        GuiButton button = getHoveredDisplayButton(mouseX, mouseY);
        if (button == null) {
            if (drawUpgradeSlotTooltip(mouseX, mouseY)) {
                return;
            }
            if (inside(
                mouseX,
                mouseY,
                layout.getCraftableLabelX(),
                layout.getCraftableLabelY(),
                layout.centralLeft + layout.centralWidth - layout.getCraftableLabelX(),
                10)) {
                GuiGraphics.drawToolTip(
                    mouseX,
                    mouseY,
                    Arrays.asList(
                        "Craftable items",
                        container.getCraftableAmount() + " output items",
                        "Table storage and your inventory.",
                        "Before reusing returned containers or tools."),
                    EnumChatFormatting.WHITE);
            }
            return;
        }
        List<String> tooltip;
        if (button.id == SORT_MODE_BUTTON) {
            String mode = displaySettings.getSortMode() == RequestTableDisplaySettings.SortMode.NAME ? "Item name"
                    : "Item amount";
            tooltip = Arrays.asList("Sort by", mode);
        } else if (button.id == SORT_DIRECTION_BUTTON) {
            String direction = displaySettings.getSortDirection() == RequestTableDisplaySettings.SortDirection.ASCENDING
                    ? "Ascending"
                    : "Descending";
            tooltip = Arrays.asList("Sort direction", direction);
        } else if (button.id == FILTER_MODE_BUTTON) {
            String filter;
            switch (displaySettings.getFilterMode()) {
                case STORED:
                    filter = "Stored";
                    break;
                case CRAFTABLE:
                    filter = "Craftable";
                    break;
                case BOTH:
                default:
                    filter = "Stored and craftable";
                    break;
            }
            tooltip = Arrays.asList("Show entries", filter);
        } else if (button.id == REQUEST_MESSAGES_BUTTON) {
            tooltip = Arrays.asList(
                "Request messages: " + (displaySettings.isRequestMessagesEnabled() ? "On" : "Off"),
                "Toggle request-result popups and chat.",
                "Items, fluids and crafting ingredients.");
        } else if (button.id == SEARCH_MODE_BUTTON) {
            RequestTableDisplaySettings.SearchBoxMode mode = displaySettings.getSearchBoxMode();
            tooltip = Arrays.asList(
                "Search box mode: " + mode.getLabel(),
                "Focus on opening: " + (mode.isAutoFocus() ? "Yes" : "No"),
                "Sync to NEI: " + (mode.isNeiSynced() ? "Yes (one way)" : "No"),
                "Click to cycle search mode.");
        } else if (button.id == SAVE_SEARCH_BUTTON) {
            tooltip = Arrays.asList(
                "Save search text: " + (displaySettings.isSaveSearch() ? "Yes" : "No"),
                "Remember the search when reopening this table.");
        } else if (button.id == TERMINAL_STYLE_BUTTON) {
            boolean tall = displaySettings.getTerminalStyle() == RequestTableDisplaySettings.TerminalStyle.TALL;
            tooltip = Arrays.asList(
                "Terminal size: " + (tall ? "Tall" : "Small"),
                tall ? "Use the available window height."
                    : "Up to " + RequestTableLayout.SMALL_ROWS + " content rows.",
                "Leave room for NEI's top and bottom controls.");
        } else if (button.id == SHOW_ITEMS_BUTTON) {
            tooltip = Arrays.asList(
                "Show items: " + (displaySettings.isShowItems() ? "Yes" : "No"),
                "Toggle items in the Main list.");
        } else if (button.id == SHOW_FLUIDS_BUTTON) {
            tooltip = Arrays.asList(
                "Show fluids: " + (displaySettings.isShowFluids() ? "Yes" : "No"),
                "Toggle fluids in the Main list.");
        } else if (button.id == ITEM_VIEW_BUTTON) {
            tooltip = Arrays.asList(
                "Internal item storage",
                table.inv.getSizeInventory() + " slots",
                table.inv.getInventoryStackLimit() + " items per slot");
        } else if (button.id == FLUID_VIEW_BUTTON) {
            tooltip = Arrays.asList(
                "Internal fluid storage",
                table.getFluidStorage().getSizeInventory() + " tanks",
                table.getFluidStorage().getSlotCapacity() + " mB per tank");
        } else if (button.id == REQUEST_INGREDIENTS_BUTTON) {
            tooltip = Arrays.asList(
                "Request missing ingredients",
                getIngredientRequestAmount() + " craft sets",
                "Uses ingredients already in the table first.");
        } else if (button.id == CLEAR_CRAFTING_BUTTON) {
            tooltip = List.of("Clear crafting grid");
        } else if (button.id == SEND_BUTTON) {
            tooltip = Arrays.asList(
                "Send all " + (view == RequestTableView.FLUID_STORAGE ? "fluids" : "items"),
                "Return stored contents to the network.");
        } else {
            tooltip = Arrays.asList("Network", "Browse stored and craftable items and fluids.");
        }
        GuiGraphics.drawToolTip(mouseX, mouseY, tooltip, EnumChatFormatting.WHITE);
    }

    private boolean drawUpgradeSlotTooltip(int mouseX, int mouseY) {
        for (int i = 0; i < table.getOriginalUpgradeManager().getInv().getSizeInventory(); i++) {
            if (table.getOriginalUpgradeManager().getInv().getStackInSlot(i) == null
                && inside(mouseX, mouseY, layout.upgradeLeft, layout.getUpgradeSlotY(i), 16, 16)) {
                GuiGraphics.drawToolTip(
                    mouseX,
                    mouseY,
                    i == 0 ? Arrays.asList("Crafting monitor upgrade", "Crafting Monitoring Upgrade only.")
                        : Arrays.asList("Storage upgrades", "Item/fluid slot and capacity upgrades only."),
                    EnumChatFormatting.WHITE);
                return true;
            }
        }
        return false;
    }

    private GuiButton getHoveredDisplayButton(int mouseX, int mouseY) {
        for (GuiButton button : Arrays.asList(
            networkButton,
            itemButton,
            fluidButton,
            sortModeButton,
            sortDirectionButton,
            filterModeButton,
            requestMessagesButton,
            searchModeButton,
            saveSearchButton,
            terminalStyleButton,
            showItemsButton,
            showFluidsButton,
            sendButton,
            requestIngredientsButton,
            clearCraftingButton)) {
            if (isHovered(button, mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    private boolean isHovered(GuiButton button, int mouseX, int mouseY) {
        return button != null && button.visible
                && inside(mouseX, mouseY, button.xPosition, button.yPosition, button.width, button.height);
    }

    private void initIngredientAmountField() {
        String text = ingredientAmountField == null ? "1" : ingredientAmountField.getText();
        boolean focused = ingredientAmountField != null && ingredientAmountField.isFocused();
        ingredientAmountField = new GuiTextField(
                mc.fontRenderer,
                layout.craftingAmountX,
                layout.craftingAmountY,
                layout.craftingAmountWidth,
                layout.craftingAmountHeight);
        ingredientAmountField.setMaxStringLength(5);
        ingredientAmountField.setText(text);
        ingredientAmountField.setFocused(focused);
        sanitizeIngredientAmountField();
    }

    private void sanitizeIngredientAmountField() {
        String text = ingredientAmountField.getText();
        StringBuilder digits = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        String sanitized = digits.toString();
        if (!sanitized.equals(text)) {
            ingredientAmountField.setText(sanitized);
        }
    }

    private int getIngredientRequestAmount() {
        String text = ingredientAmountField == null ? "" : ingredientAmountField.getText();
        if (text.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(9999, Integer.parseInt(text)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void requestCraftingIngredients() {
        MainProxy.sendPacketToServer(
                PacketHandler.getPacket(RequestTableRequestIngredientsPacket.class)
                        .setInteger(getIngredientRequestAmount()).setTilePos(table.container));
        refreshNetwork();
    }

    private void updateContainerLayout() {
        if (layout == null) {
            return;
        }
        clampStorageScroll();
        container.layout(layout, view, storageScrollRow);
    }

    private void updateLayout() {
        layout = new RequestTableLayout(guiLeft, guiTop, xSize, ySize);
        if (ingredientAmountField != null) {
            ingredientAmountField.xPosition = layout.craftingAmountX;
            ingredientAmountField.yPosition = layout.craftingAmountY;
            ingredientAmountField.width = layout.craftingAmountWidth;
            ingredientAmountField.height = layout.craftingAmountHeight;
        }
    }

    private String getSearchText() {
        return search == null ? "" : search.getContent();
    }

    private boolean shouldOpenRequestOverlay(int button) {
        return button == 2 || (isCtrlDown() && (button == 0 || button == 1));
    }

    private boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private void clampStorageScroll() {
        storageScrollRow = Math.max(0, Math.min(getMaxStorageScrollRow(), storageScrollRow));
    }

    private int getMaxStorageScrollRow() {
        int columns = RequestTableLayout.INVENTORY_COLUMNS;
        int size = view == RequestTableView.FLUID_STORAGE ? table.getFluidStorage().getSizeInventory()
                : table.inv.getSizeInventory();
        int rows = (size + columns - 1) / columns;
        int visibleRows = layout.getVisibleStorageRows();
        return Math.max(0, rows - visibleRows);
    }

}
