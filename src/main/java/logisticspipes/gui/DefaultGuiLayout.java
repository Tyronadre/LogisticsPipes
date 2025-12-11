package logisticspipes.gui;

import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * AutoGuiLayout
 * <p>
 * A small instantiable utility that takes over common GUI layout and rendering
 * decisions for Inventory-like GUIs: title, block inventory grid, gap and
 * player inventory/hotbar. Designed to be instantiated from a
 * LogisticsBaseGuiScreen and called from its draw methods.
 * <p>
 * Usage contract:
 * <ul>
 * <li> Use the builder methods and end with the build() method.</li>
 * <li> Builder methods need a specific order, otherwise strange things might happen</li>
 * <li> renderBackground() and renderForeground() should be called in the Gui respectively to render the component</li>
 */
public class DefaultGuiLayout {

    private final LogisticsBaseGuiScreen gui;
    private final IInventory inventory;

    @Setter
    private Minecraft mc;

    // --- helpers ---
    /**
     * How many slots should be displayed in one row
     */
    @Getter
    private final int slotsPerRow = 9;

    /**
     * The xOffset from the left side of the gui
     */
    @Getter
    private int slotXOffset = 8;

    /**
     * The yOffset from the top of the gui (or the title if present)
     */
    @Getter
    private int slotYOffset = 8;

    /**
     * number of pixels in each slot
     */
    @Getter
    private final int slotSpacing = 18;

    // number of pixels between the player inventory and the rest of the gui
    private int gapToPlayer = 10;


    private String title = null;
    private ResourceLocation backgroundTexture = null;

    private final DummyContainer invContainer;

    /**
     * Create an AutoGuiLayout that will operate on a pre-created DummyContainer.
     * If no DummyContainer is provided, a new one will be created.
     */
    public DefaultGuiLayout(LogisticsBaseGuiScreen gui, IInventory playerInventory, IInventory dummyInventory,
                            DummyContainer container) {
        this.gui = gui;
        this.inventory = dummyInventory;
        this.invContainer = container == null ? new DummyContainer(playerInventory, dummyInventory) : container;
    }


    //region [Builder methods]

    public DefaultGuiLayout setTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets a custom background texture
     *
     * @param texture the custom texture
     */
    public DefaultGuiLayout setBackgroundTexture(ResourceLocation texture) {
        this.backgroundTexture = texture;
        return this;
    }

    /**
     * Sets the origin of the slots in the gui. Default is x 8 and y 8
     *
     * @param x the xOffset
     * @param y the yOffset
     */
    public DefaultGuiLayout setSlotOrigin(int x, int y) {
        this.slotXOffset = x;
        this.slotYOffset = y;
        return this;
    }

    /**
     * Sets how much space is between the inventory and the player inventory. default is 8
     *
     * @param gap the gap
     */
    public DefaultGuiLayout setGapToPlayer(int gap) {
        this.gapToPlayer = gap;
        return this;
    }


    public DefaultGuiLayout build() {
        invContainer.addNormalSlotsForPlayerInventory(slotXOffset, computePlayerInventoryTop());

        // Only set GUI-specific fields if a GUI instance was provided (client-side).
        if (gui != null) {
            gui.inventorySlots = invContainer;
            gui.width = computeWidth();
            gui.height = computeHeight();
        }

        return this;
    }

    private int computeWidth() {
        return slotXOffset * 2 + slotSpacing * slotsPerRow;
    }

    private int computeHeight() {
        int playerInventoryHeight = 76;
        return slotYOffset * 2 + computeNumberOfRows() * slotSpacing + gapToPlayer + playerInventoryHeight + computeTitleHeight();
    }


    //endregion

    //region [Slot creation]

    private int currentSlotOffset = 0;

    public DefaultGuiLayout addSlots(int numberOfSlots) {
        if (numberOfSlots + currentSlotOffset > inventory.getSizeInventory()) {
            throw new IllegalArgumentException("Trying to add more slots than the inventory has (" + numberOfSlots + currentSlotOffset + " but inventory has only  " + inventory.getSizeInventory() + ")");
        }
        for (int i = 0; i < numberOfSlots; i++, currentSlotOffset++) {
            var xCord = slotXOffset + (slotSpacing * (currentSlotOffset % slotsPerRow));
            var yCord = slotYOffset + slotSpacing * (computeNumberOfRows() - 1) + computeTitleHeight();
            invContainer.addDummySlot(currentSlotOffset, inventory, xCord, yCord);
        }

        return this;
    }

    //endregion

    /**
     * Compute the offset caused by a possible title
     */
    private int computeTitleHeight() {
        return title != null ? 8 : 0;
    }

    /**
     * Compute number of rows required for the configured cols.
     */
    private int computeNumberOfRows() {
        return (currentSlotOffset - 1) / slotsPerRow + 1;
    }

    /**
     * Compute the Y (relative to guiTop) where the player inventory top should be placed.
     */
    private int computePlayerInventoryTop() {
        return computeTitleHeight() + slotYOffset + computeNumberOfRows() * slotSpacing + gapToPlayer;
    }


    /**
     * Render the automatic layout background. Call from
     * drawGuiContainerBackgroundLayer.
     */
    public void renderBackground() {
        if (mc == null || gui == null) return;

        //draw the background (texture if set or default background)
        if (backgroundTexture != null) {
            GL11.glColor4f(1F, 1F, 1F, 1F);
            mc.renderEngine.bindTexture(backgroundTexture);
            int j = gui.guiLeft;
            int k = gui.guiTop;
            gui.drawTexturedModalRect(j, k, 0, 0, gui.width, gui.height);
        } else {
            GuiGraphics.drawGuiBackGround(mc, gui.guiLeft, gui.guiTop, gui.guiLeft + computeWidth(), gui.guiTop + computeHeight(), GuiGraphics.zLevel, true);
        }

        // draw block slots and player inventory backgrounds translated to gui origin
        GL11.glTranslated(gui.guiLeft, gui.guiTop, 0);

        for (var slot : invContainer.inventorySlots) {
            GuiGraphics.drawSlotBackground(mc, slot.xDisplayPosition - 1, slot.yDisplayPosition - 1);
        }

        GuiGraphics.drawPlayerInventoryBackground(mc, slotXOffset, computePlayerInventoryTop());

        GL11.glTranslated(-gui.guiLeft, -gui.guiTop, 0);
    }

    /**
     * Render foreground elements: title and optional slot number labels.
     * Call from drawGuiContainerForegroundLayer.
     */
    public void renderForeground() {
        if (mc == null || gui == null) return;

        if (title != null && !title.isEmpty()) {
            mc.fontRenderer.drawString(title, 6, 6, 0x404040);
        }
    }

    public DummyContainer getContainer() {
        return invContainer;
    }

}
