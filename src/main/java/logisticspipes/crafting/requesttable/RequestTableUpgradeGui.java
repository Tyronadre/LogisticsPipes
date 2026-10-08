package logisticspipes.crafting.requesttable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.crafting.requesttable.RequestTableItemUpgradePacket;
import logisticspipes.network.packets.crafting.requesttable.RequestTableOpenUpgradesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.GuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Mouse;

import java.util.Arrays;
import java.util.List;

/** A scrollable Minecraft-style tree with independent slot-count and slot-size branches. */
@SideOnly(Side.CLIENT)
public class RequestTableUpgradeGui extends LogisticsBaseGuiScreen {

    private static final int WIDTH = 286;
    private static final int ROW_HEIGHT = 18;
    private static final int BACK = 0;
    private static final int APPLY = 1;
    private final RequestTableUpgradeContainer upgrades;
    private final NodeButton[][] nodes = new NodeButton[2][RequestTableItemUpgradeConfig.TIER_COUNT];
    private GuiButton applyButton;
    private int scrollRow;
    private boolean scrollbarDragging;
    private int dragOffset;

    public RequestTableUpgradeGui(EntityPlayer player, RequestTablePipe table) {
        super(new RequestTableUpgradeContainer(player, table), WIDTH, 300, 0, 0);
        upgrades = (RequestTableUpgradeContainer) inventorySlots;
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public void initGui() {
        ySize = Math.max(216, Math.min(360, height - 40));
        super.initGui();
        buttonList.clear();
        buttonList.add(new ActionButton(BACK, guiLeft + xSize - 50, guiTop + 5, 42, 14, "Back"));
        applyButton = new ActionButton(APPLY, guiLeft + 156, detailsTop() + 29, 110, 18, "Apply upgrade");
        buttonList.add(applyButton);
        for (RequestTableItemUpgradeBranch branch : RequestTableItemUpgradeBranch.values()) {
            for (int tier = 1; tier <= RequestTableItemUpgradeConfig.TIER_COUNT; tier++) {
                NodeButton node = new NodeButton(10 + branch.ordinal() * 10 + tier - 1, branch, tier);
                nodes[branch.ordinal()][tier - 1] = node;
                buttonList.add(node);
            }
        }
        updateNodes();
        upgrades.layout(ySize);
    }

    private int treeTop() {
        return guiTop + 36;
    }

    private int detailsTop() {
        return guiTop + ySize - 146;
    }

    private int visibleRows() {
        return Math.max(1, (detailsTop() - treeTop() - 4) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, RequestTableItemUpgradeConfig.TIER_COUNT - visibleRows());
    }

    private int treeHeight() {
        return visibleRows() * ROW_HEIGHT;
    }

    private int thumbHeight() {
        return Math.max(8, treeHeight() * Math.min(10, visibleRows()) / 10);
    }

    private int thumbTop() {
        return treeTop() + (maxScroll() == 0 ? 0 : (treeHeight() - thumbHeight()) * scrollRow / maxScroll());
    }

    private void updateNodes() {
        scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow));
        for (RequestTableItemUpgradeBranch branch : RequestTableItemUpgradeBranch.values()) {
            int installed = upgrades.getTable().getItemUpgradeTier(branch);
            for (NodeButton node : nodes[branch.ordinal()]) {
                if (node == null) continue;
                int row = node.tier - 1 - scrollRow;
                node.xPosition = guiLeft + (branch == RequestTableItemUpgradeBranch.SLOT_COUNT ? 13 : 153);
                node.yPosition = treeTop() + row * ROW_HEIGHT;
                node.visible = row >= 0 && row < visibleRows();
                node.completed = node.tier <= installed;
                node.unlocked = node.tier <= installed + 1;
                node.selected = branch == upgrades.getSelectedBranch() && node.tier == upgrades.getSelectedTier();
                node.displayString = RequestTableItemUpgradeConfig.getTierName(node.tier) + "  +"
                    + upgrades.getConfig().getBonus(branch, node.tier);
            }
        }
        applyButton.enabled = upgrades.isReady() && upgrades.canUpgrade();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        updateNodes();
        upgrades.layout(ySize);
        GuiGraphics.drawGuiBackGround(mc, guiLeft, guiTop, right, bottom, zLevel, true);
        mc.fontRenderer.drawString("Item storage upgrades", guiLeft + 12, guiTop + 8, RequestTableGuiStyle.TEXT);
        mc.fontRenderer.drawString("Amount of slots", guiLeft + 15, guiTop + 23, RequestTableGuiStyle.TEXT);
        mc.fontRenderer.drawString("Slot size", guiLeft + 187, guiTop + 23, RequestTableGuiStyle.TEXT);
        RequestTableGuiStyle.raised(guiLeft + 126, guiTop + 20, 34, 12, false);
        mc.fontRenderer.drawString("Base", guiLeft + 131, guiTop + 22, RequestTableGuiStyle.TEXT);
        Gui.drawRect(guiLeft + 69, guiTop + 33, guiLeft + 210, guiTop + 34, 0xff777777);
        Gui.drawRect(guiLeft + 142, guiTop + 32, guiLeft + 143, guiTop + 34, 0xff777777);
        for (int x : new int[]{69, 209}) {
            Gui.drawRect(guiLeft + x, guiTop + 33, guiLeft + x + 1, treeTop() + treeHeight(), 0xff777777);
        }
        int sx = guiLeft + 272;
        RequestTableGuiStyle.inset(sx, treeTop(), 7, treeHeight());
        RequestTableGuiStyle.raised(sx, thumbTop(), 7, thumbHeight(), scrollbarDragging);
        drawRequirements();
        int playerTop = guiTop + ySize - 84;
        mc.fontRenderer.drawString(
            "Inventory",
            guiLeft + RequestTableUpgradeContainer.INVENTORY_LEFT,
            playerTop - 11,
            RequestTableGuiStyle.TEXT);
        GuiGraphics.drawPlayerInventoryBackground(mc, guiLeft + RequestTableUpgradeContainer.INVENTORY_LEFT, playerTop);
    }

    private void drawRequirements() {
        RequestTableItemUpgradeBranch branch = upgrades.getSelectedBranch();
        int tier = upgrades.getSelectedTier();
        RequestTableItemUpgradeConfig config = upgrades.getConfig();
        int current = config.getTotal(branch, upgrades.getTable().getItemUpgradeTier(branch));
        int target = config.getTotal(branch, tier);
        String unit = branch == RequestTableItemUpgradeBranch.SLOT_COUNT ? "slots" : "items/slot";
        String heading = branch.getLabel() + " " + RequestTableItemUpgradeConfig.getTierName(tier);
        mc.fontRenderer.drawString(heading, guiLeft + 12, detailsTop(), RequestTableGuiStyle.TEXT);
        String change = current + " -> " + target + " " + unit;
        mc.fontRenderer.drawString(
            change,
            guiLeft + xSize - 12 - mc.fontRenderer.getStringWidth(change),
            detailsTop(),
            RequestTableGuiStyle.MUTED);
        int materialsLeft = guiLeft + RequestTableUpgradeContainer.INVENTORY_LEFT;
        ItemStack example = config.getExampleCircuit(tier);
        if (example != null) {
            new ItemStackRenderer(materialsLeft, detailsTop() + 12, 100.0F, true, false, true)
                .setItemIdentifierStack(ItemIdentifierStack.getFromStack(example))
                .setDisplayAmount(DisplayAmount.NEVER).renderInGui();
        }
        String requirement = "Need " + config.getCost(tier)
            + "x "
            + RequestTableItemUpgradeConfig.getTierName(tier)
            + " circuit";
        mc.fontRenderer.drawString(requirement, materialsLeft + 21, detailsTop() + 16, RequestTableGuiStyle.TEXT);
        for (int i = 0; i < RequestTableUpgradeContainer.MATERIAL_SLOTS; i++) {
            GuiGraphics.drawSlotBackground(mc, materialsLeft - 1 + i * 18, detailsTop() + 28);
        }
        String count = upgrades.getMaterialCount() + "/" + config.getCost(tier);
        mc.fontRenderer.drawString(
            count,
            guiLeft + xSize - 12 - mc.fontRenderer.getStringWidth(count),
            detailsTop() + 16,
            upgrades.getMaterialCount() >= config.getCost(tier) ? 0x37622e : RequestTableGuiStyle.MUTED);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == BACK) {
            MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestTableOpenUpgradesPacket.class).setInteger(1));
        } else if (button.id == APPLY) {
            if (!upgrades.isReady() || !upgrades.canUpgrade()) return;
            sendSelection(upgrades.getSelectedBranch(), upgrades.getSelectedTier(), true);
        } else if (button instanceof NodeButton node) {
            sendSelection(node.branch, node.tier, false);
        }
    }

    private void sendSelection(RequestTableItemUpgradeBranch branch, int tier, boolean start) {
        upgrades.awaitState();
        MainProxy.sendPacketToServer(
            PacketHandler.getPacket(RequestTableItemUpgradePacket.class)
                .setUpgrade(upgrades.windowId, branch, tier, start));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (scrollbarDragging && !Mouse.isButtonDown(0)) scrollbarDragging = false;
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hasSubGui()) return;
        for (NodeButton[] branch : nodes) {
            for (NodeButton node : branch) {
                if (!node.visible || !inside(mouseX, mouseY, node.xPosition, node.yPosition, node.width, node.height))
                    continue;
                String state = node.completed ? "Installed permanently."
                    : node.unlocked ? "Ready to upgrade."
                    : "Requires " + RequestTableItemUpgradeConfig.getTierName(node.tier - 1)
                    + " on this branch.";
                GuiGraphics.drawToolTip(
                    mouseX,
                    mouseY,
                    Arrays.asList(
                        node.branch.getLabel() + " " + RequestTableItemUpgradeConfig.getTierName(node.tier),
                        state,
                        "Total: " + upgrades.getConfig().getTotal(node.branch, node.tier),
                        "Consumes " + upgrades.getConfig().getCost(node.tier)
                            + "x "
                            + upgrades.getConfig().getCircuitOreName(node.tier)),
                    EnumChatFormatting.WHITE);
                return;
            }
        }
        if (inside(
            mouseX,
            mouseY,
            applyButton.xPosition,
            applyButton.yPosition,
            applyButton.width,
            applyButton.height)) {
            int installed = upgrades.getTable().getItemUpgradeTier(upgrades.getSelectedBranch());
            List<String> tooltip = !upgrades.isReady() ? List.of("Waiting for the server...")
                : installed >= upgrades.getSelectedTier() ? List.of("Already installed.")
                : installed + 1 != upgrades.getSelectedTier()
                ? List.of("Install the preceding tier on this branch first.")
                : Arrays.asList(
                "Consume the required materials to unlock this tier.",
                "Permanent, including after breaking and placing the table.");
            GuiGraphics.drawToolTip(mouseX, mouseY, tooltip, EnumChatFormatting.WHITE);
        } else if (inside(
            mouseX,
            mouseY,
            guiLeft + RequestTableUpgradeContainer.INVENTORY_LEFT,
            detailsTop() + 12,
            16,
            16)) {
            GuiGraphics.drawToolTip(
                mouseX,
                mouseY,
                Arrays.asList(
                    "Accepted circuit: "
                        + upgrades.getConfig().getCircuitOreName(upgrades.getSelectedTier()),
                    "Insert matching circuits into the four slots below.",
                    "Unused materials return when you leave this screen."),
                EnumChatFormatting.WHITE);
        }
    }

    @Override
    public void handleMouseInputSub() {
        int wheel = Mouse.getEventDWheel();
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (wheel != 0 && inside(mouseX, mouseY, guiLeft + 8, treeTop(), 272, treeHeight())) {
            scrollRow = Math.max(0, Math.min(maxScroll(), scrollRow + (wheel > 0 ? -1 : 1)));
            updateNodes();
        }
        super.handleMouseInputSub();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, guiLeft + 272, treeTop(), 7, treeHeight())) {
            if (maxScroll() == 0) return;
            dragOffset = mouseY >= thumbTop() && mouseY < thumbTop() + thumbHeight() ? mouseY - thumbTop()
                : thumbHeight() / 2;
            scrollbarDragging = true;
            dragScrollbar(mouseY);
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    private void dragScrollbar(int mouseY) {
        int travel = treeHeight() - thumbHeight();
        int position = Math.max(0, Math.min(travel, mouseY - treeTop() - dragOffset));
        scrollRow = travel <= 0 ? 0 : Math.round((float) position * maxScroll() / travel);
        updateNodes();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        if (scrollbarDragging) {
            dragScrollbar(mouseY);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, button, elapsed);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        if (scrollbarDragging) {
            if (button == 0) scrollbarDragging = false;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    /** Draws complete bevels even when the button is shorter than vanilla's 20-pixel texture. */
    private static final class ActionButton extends GuiButton {

        private ActionButton(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hover = enabled && inside(mouseX, mouseY, xPosition, yPosition, width, height);
            RequestTableGuiStyle.raised(xPosition, yPosition, width, height, hover);
            mc.fontRenderer.drawString(
                displayString,
                xPosition + (width - mc.fontRenderer.getStringWidth(displayString)) / 2,
                yPosition + (height - 8) / 2,
                enabled ? RequestTableGuiStyle.TEXT : 0x808080);
        }
    }

    private static final class NodeButton extends GuiButton {

        private final RequestTableItemUpgradeBranch branch;
        private final int tier;
        private boolean completed;
        private boolean unlocked;
        private boolean selected;

        private NodeButton(int id, RequestTableItemUpgradeBranch branch, int tier) {
            super(id, 0, 0, 112, 16, "");
            this.branch = branch;
            this.tier = tier;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hover = inside(mouseX, mouseY, xPosition, yPosition, width, height);
            RequestTableGuiStyle.raised(xPosition, yPosition, width, height, hover);
            Gui.drawRect(
                xPosition + 2,
                yPosition + 2,
                xPosition + width - 2,
                yPosition + height - 2,
                completed ? 0xffb0c0a9 : unlocked ? 0xffc6c6c6 : 0xff969696);
            if (selected) {
                Gui.drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + 2, 0xffe8c66a);
                Gui.drawRect(
                    xPosition + 1,
                    yPosition + height - 2,
                    xPosition + width - 1,
                    yPosition + height - 1,
                    0xffe8c66a);
            }
            Gui.drawRect(
                xPosition + 4,
                yPosition + 6,
                xPosition + 7,
                yPosition + 9,
                completed ? 0xff37622e : unlocked ? 0xffcf9d53 : 0xff626262);
            mc.fontRenderer.drawString(displayString, xPosition + 12, yPosition + 4, RequestTableGuiStyle.TEXT);
        }
    }
}
