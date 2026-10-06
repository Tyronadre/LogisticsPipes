package logisticspipes.crafting.requesttable;

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
    private RequestTableIconButton networkButton;
    private RequestTableIconButton itemButton;
    private RequestTableIconButton fluidButton;
    private RequestTableIconButton clearCraftingButton;
    private RequestTableView view = RequestTableView.NETWORK;
    private RequestTableDisplaySettings displaySettings = RequestTableDisplaySettings.DEFAULT;
    private int storageScrollRow;
    private final int dimension;

    /**
     * Creates the client GUI for the new request table.
     */
    public RequestTableGui(EntityPlayer player, RequestTablePipe table) {
        super(new RequestTableContainer(player, table), 264, 300, 0, 0);
        this.player = player;
        this.table = table;
        this.container = (RequestTableContainer) inventorySlots;
        dimension = MainProxy.getDimensionForWorld(table.getWorld());
        refreshNetwork();
    }

    @Override
    public void initGui() {
        xSize = 264;
        ySize = Math.max(232, Math.min(352, height - 12));
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
                layout.displayButtonX,
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
            search = new GuiSearchBar("new_request_table_search");
        }
        search.reposition(layout.searchX, layout.searchY, layout.searchWidth, layout.searchHeight);
        initIngredientAmountField();
        updateContainerLayout();
    }

    /**
     * Receives the server-built network list.
     */
    public void handleNetworkContent(List<RequestTableNetworkEntry> entries,
            RequestTableDisplaySettings displaySettings) {
        this.displaySettings = displaySettings;
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

        GuiGraphics.drawGuiBackGround(
            mc,
            layout.centralLeft,
            layout.centralTop,
            layout.centralLeft + layout.centralWidth,
            bottom,
            zLevel,
            true);
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
            if (search.isEmpty() && !search.isFocused()) {
                mc.fontRenderer
                    .drawString("Search items and fluids...", layout.searchX + 3, layout.searchY + 3, 0xaaaaaa);
            }
        } else {
            String title = view == RequestTableView.ITEM_STORAGE ? "Item storage" : "Fluid storage";
            mc.fontRenderer.drawString(title, layout.searchX, layout.searchY + 3, RequestTableGuiStyle.TEXT);
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
        mc.fontRenderer.drawString("Crafting", layout.craftingLeft, layout.craftingTop + 2, RequestTableGuiStyle.TEXT);
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
            layout.craftingLeft,
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
        if (requestOverlay.mouseClicked(mouseX, mouseY, button, this::submitOverlayRequest)) {
            return;
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
        if (view == RequestTableView.NETWORK) {
            search.handleClick(mouseX, mouseY, button);
            networkGrid.setSearch(getSearchText());
            RequestTableNetworkEntry entry = networkGrid.getEntryAt(layout, mouseX, mouseY);
            if (entry != null) {
                if (shouldOpenRequestOverlay(button)) {
                    requestOverlay.open(entry, mc.fontRenderer, width, height, button == 0 ? 64 : 1);
                } else if (button == 0 || button == 1) {
                    MainProxy.sendPacketToServer(
                            PacketHandler.getPacket(RequestTableNetworkInteractPacket.class).setFluid(entry.isFluid())
                                    .setMouseButton(button).setShift(isShiftDown()).setDimension(dimension)
                                    .setStack(entry.getStack()).setTilePos(table.container));
                }
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
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
        if (view == RequestTableView.NETWORK && keyCode != 1 && search.handleKey(typed, keyCode)) {
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
        if (wheel != 0) {
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
        displaySettings = newSettings;
        networkGrid.setDisplaySettings(newSettings);
        updateDisplayButtons();
        MainProxy.sendPacketToServer(
                PacketHandler.getPacket(RequestTableDisplaySettingsPacket.class).setSettings(newSettings)
                        .setTilePos(table.container));
    }

    private void updateDisplayButtons() {
        if (sortModeButton == null) {
            return;
        }
        requestMessagesButton
            .setIcon(displaySettings.isRequestMessagesEnabled() ? Icon.MESSAGES_ON : Icon.MESSAGES_OFF);
        requestMessagesButton.setSelected(displaySettings.isRequestMessagesEnabled());
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
        boolean enabled = view == RequestTableView.NETWORK;
        sortModeButton.enabled = enabled;
        sortDirectionButton.enabled = enabled;
        filterModeButton.enabled = enabled;
        sortModeButton.visible = sortModeButton.enabled;
        sortDirectionButton.visible = sortDirectionButton.enabled;
        filterModeButton.visible = filterModeButton.enabled;
        sortModeButton.xPosition = layout.displayButtonX;
        sortModeButton.yPosition = layout.sortModeButtonY;
        sortDirectionButton.xPosition = layout.displayButtonX;
        sortDirectionButton.yPosition = layout.sortDirectionButtonY;
        filterModeButton.xPosition = layout.displayButtonX;
        filterModeButton.yPosition = layout.filterModeButtonY;
        requestMessagesButton.enabled = true;
        requestMessagesButton.visible = true;
        requestMessagesButton.xPosition = layout.displayButtonX;
        requestMessagesButton.yPosition = enabled ? layout.requestMessagesButtonY
            : layout.sendButtonY + (sendButton.visible ? 24 : 0);
    }

    private void drawDisplayButtonTooltip(int mouseX, int mouseY) {
        GuiButton button = getHoveredDisplayButton(mouseX, mouseY);
        if (button == null) {
            if (drawUpgradeSlotTooltip(mouseX, mouseY)) {
                return;
            }
            if (inside(mouseX, mouseY, layout.craftingLeft, layout.getCraftableLabelY(), 160, 10)) {
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
        int columns = 9;
        int size = view == RequestTableView.FLUID_STORAGE ? table.getFluidStorage().getSizeInventory()
                : table.inv.getSizeInventory();
        int rows = (size + columns - 1) / columns;
        int visibleRows = layout.getVisibleStorageRows();
        storageScrollRow = Math.max(0, Math.min(Math.max(0, rows - visibleRows), storageScrollRow));
    }

}
