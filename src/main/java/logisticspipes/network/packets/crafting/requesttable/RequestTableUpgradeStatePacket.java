package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.crafting.requesttable.RequestTableItemUpgradeBranch;
import logisticspipes.crafting.requesttable.RequestTableItemUpgradeConfig;
import logisticspipes.crafting.requesttable.RequestTableUpgradeContainer;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.PacketGuards;
import logisticspipes.network.abstractpackets.ModernPacket;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;

/** Sends authoritative tree progress and configured bonuses/costs to the current upgrade window. */
public class RequestTableUpgradeStatePacket extends ModernPacket {

    private int windowId;
    private int slotTier;
    private int sizeTier;
    private RequestTableItemUpgradeBranch branch;
    private int tier;
    private RequestTableItemUpgradeConfig config;

    public RequestTableUpgradeStatePacket(int id) {
        super(id);
    }

    public RequestTableUpgradeStatePacket setState(RequestTableUpgradeContainer upgrades) {
        windowId = upgrades.windowId;
        slotTier = upgrades.getTable().getItemSlotTier();
        sizeTier = upgrades.getTable().getItemSizeTier();
        branch = upgrades.getSelectedBranch();
        tier = upgrades.getSelectedTier();
        config = upgrades.getConfig();
        return this;
    }

    @Override
    public ModernPacket template() {
        return new RequestTableUpgradeStatePacket(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (!PacketGuards.isOnClient(player) || !RequestTableItemUpgradeConfig.isValidTier(tier)
            || !(player.openContainer instanceof RequestTableUpgradeContainer upgrades)
            || upgrades.windowId != windowId)
            return;
        upgrades.applyState(config, slotTier, sizeTier, branch, tier);
    }

    @Override
    public void writeData(LPDataOutputStream output) throws IOException {
        output.writeInt(windowId);
        output.writeInt(slotTier);
        output.writeInt(sizeTier);
        output.writeEnum(branch);
        output.writeInt(tier);
        config.writeData(output);
    }

    @Override
    public void readData(LPDataInputStream input) throws IOException {
        windowId = input.readInt();
        slotTier = input.readInt();
        sizeTier = input.readInt();
        branch = input.readEnum(RequestTableItemUpgradeBranch.class);
        tier = input.readInt();
        config = RequestTableItemUpgradeConfig.readData(input);
    }
}
