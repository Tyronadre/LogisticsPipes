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

/** Carries the selected branch/tier and explicit confirmation; the server owns costs and unlocks. */
public class RequestTableItemUpgradePacket extends ModernPacket {

    private int windowId;
    private RequestTableItemUpgradeBranch branch;
    private int tier;
    private boolean start;

    public RequestTableItemUpgradePacket(int id) {
        super(id);
    }

    public RequestTableItemUpgradePacket setUpgrade(int windowId, RequestTableItemUpgradeBranch branch, int tier,
                                                    boolean start) {
        this.windowId = windowId;
        this.branch = branch;
        this.tier = tier;
        this.start = start;
        return this;
    }

    @Override
    public ModernPacket template() {
        return new RequestTableItemUpgradePacket(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (PacketGuards.isOnClient(player) || !RequestTableItemUpgradeConfig.isValidTier(tier)
            || !(player.openContainer instanceof RequestTableUpgradeContainer upgrades)
            || upgrades.windowId != windowId
            || !upgrades.canInteractWith(player))
            return;
        if (start) {
            upgrades.startUpgrade(branch, tier);
            // Reconcile rejected, stale and duplicate confirmations too.
            upgrades.select(upgrades.getSelectedBranch(), upgrades.getSelectedTier());
        } else {
            upgrades.select(branch, tier);
        }
    }

    @Override
    public void writeData(LPDataOutputStream output) throws IOException {
        output.writeInt(windowId);
        output.writeEnum(branch);
        output.writeInt(tier);
        output.writeBoolean(start);
    }

    @Override
    public void readData(LPDataInputStream input) throws IOException {
        windowId = input.readInt();
        branch = input.readEnum(RequestTableItemUpgradeBranch.class);
        tier = input.readInt();
        start = input.readBoolean();
    }
}
