package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.crafting.requesttable.RequestTableContainer;
import logisticspipes.crafting.requesttable.RequestTableView;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.PacketGuards;
import logisticspipes.network.abstractpackets.ModernPacket;
import net.minecraft.entity.player.EntityPlayer;

import java.io.IOException;

/** Carries the active view with a player inventory slot, independent of the table's storage slot count. */
public class RequestTableShiftClickPacket extends ModernPacket {

    private int windowId;
    private int inventorySlot;
    private RequestTableView view = RequestTableView.NETWORK;

    public RequestTableShiftClickPacket(int id) {
        super(id);
    }

    public RequestTableShiftClickPacket setClick(int windowId, int inventorySlot, RequestTableView view) {
        this.windowId = windowId;
        this.inventorySlot = inventorySlot;
        this.view = view;
        return this;
    }

    @Override
    public ModernPacket template() {
        return new RequestTableShiftClickPacket(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (PacketGuards.isOnClient(player) || PacketGuards.getOpenRequestTable(player) == null
            || !(player.openContainer instanceof RequestTableContainer container)
            || container.windowId != windowId) {
            return;
        }
        container.shiftPlayerStack(player, inventorySlot, view);
    }

    @Override
    public void writeData(LPDataOutputStream data) throws IOException {
        data.writeInt(windowId);
        data.writeInt(inventorySlot);
        data.writeEnum(view);
    }

    @Override
    public void readData(LPDataInputStream data) throws IOException {
        windowId = data.readInt();
        inventorySlot = data.readInt();
        view = data.readEnum(RequestTableView.class);
    }
}
