package logisticspipes.network.packets.crafting;

import logisticspipes.LogisticsPipes;
import logisticspipes.crafting.pattern.AbstractPattern;
import logisticspipes.crafting.pattern.ItemPattern;
import logisticspipes.network.LPDataInputStream;
import logisticspipes.network.LPDataOutputStream;
import logisticspipes.network.abstractpackets.ModernPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.io.IOException;

@Setter
@Getter
@Accessors(chain = true)
public class PatternSatelliteAssignmentPacket extends ModernPacket {

    private int inventorySlot;
    private int inputSlot;
    private int satelliteId;
    private String satelliteUuid = "";
    private boolean fluidTarget;
    private boolean outputTarget;

    public PatternSatelliteAssignmentPacket(int id) {
        super(id);
    }

    @Override
    public void readData(LPDataInputStream data) throws IOException {
        inventorySlot = data.readInt();
        inputSlot = data.readInt();
        satelliteId = data.readInt();
        satelliteUuid = data.readUTF();
        fluidTarget = data.readBoolean();
        outputTarget = data.readBoolean();
    }

    @Override
    public void processPacket(EntityPlayer player) {
        if (inventorySlot < 0 || inventorySlot >= player.inventory.mainInventory.length) {
            return;
        }
        ItemStack pattern = player.inventory.mainInventory[inventorySlot];
        if (pattern == null || pattern.getItem() != LogisticsPipes.LogisticsPattern) {
            return;
        }
        AbstractPattern itemPattern = ItemPattern.fromStack(pattern);
        if (outputTarget && fluidTarget) {
            itemPattern.setFluidByproductSatelliteTargetForOutputSlot(inputSlot, satelliteId, satelliteUuid);
        } else if (outputTarget) {
            itemPattern.setByproductSatelliteTargetForOutputSlot(inputSlot, satelliteId, satelliteUuid);
        } else if (fluidTarget) {
            itemPattern.setFluidSatelliteTargetForInputSlot(inputSlot, satelliteId, satelliteUuid);
        } else {
            itemPattern.setSatelliteTargetForInputSlot(inputSlot, satelliteId, satelliteUuid);
        }
        player.inventory.markDirty();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    @Override
    public void writeData(LPDataOutputStream data) throws IOException {
        data.writeInt(inventorySlot);
        data.writeInt(inputSlot);
        data.writeInt(satelliteId);
        data.writeUTF(satelliteUuid == null ? "" : satelliteUuid);
        data.writeBoolean(fluidTarget);
        data.writeBoolean(outputTarget);
    }

    @Override
    public ModernPacket template() {
        return new PatternSatelliteAssignmentPacket(getId());
    }
}
