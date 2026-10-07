package logisticspipes.network.packets.crafting.requesttable;

import logisticspipes.crafting.requesttable.RequestTableDisplaySettings;
import logisticspipes.crafting.requesttable.RequestTablePipe;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

/**
 * Saves a player's display and request-message preferences at one redesigned request table.
 */
public class RequestTableDisplaySettingsPacket extends CoordinatesPacket {

    private RequestTableDisplaySettings settings = RequestTableDisplaySettings.DEFAULT;

    public RequestTableDisplaySettingsPacket(int id) {
        super(id);
    }

    public RequestTableDisplaySettingsPacket setSettings(RequestTableDisplaySettings settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public ModernPacket template() {
        return new RequestTableDisplaySettingsPacket(getId());
    }

    @Override
    public void processPacket(EntityPlayer player) {
        LogisticsTileGenericPipe tile = getPipe(player.worldObj);
        if (tile == null || !(tile.pipe instanceof RequestTablePipe table)) {
            return;
        }
        table.setDisplaySettings(player, settings);
    }

    @Override
    public void writeData(LPDataOutputStream data) throws IOException {
        super.writeData(data);
        NBTTagCompound tag = new NBTTagCompound();
        settings.writeToNBT(tag);
        data.writeNBTTagCompound(tag);
    }

    @Override
    public void readData(LPDataInputStream data) throws IOException {
        super.readData(data);
        settings = RequestTableDisplaySettings.readFromNBT(data.readNBTTagCompound());
    }
}
