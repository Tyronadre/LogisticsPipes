package logisticspipes.network;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.crafting.requesttable.RequestTableContainer;
import logisticspipes.crafting.requesttable.RequestTablePipe;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;

/** Direction, reach, security and open-container validation for request-table packets. */
public final class PacketGuards {

    private static final double MAX_INTERACT_DISTANCE_SQ = 64.0D;

    private PacketGuards() {}

    public static boolean isOnClient(EntityPlayer player) {
        return player != null && MainProxy.isClient(player.worldObj);
    }

    public static boolean canConfigurePipe(EntityPlayer player, CoreRoutedPipe pipe) {
        if (player == null || pipe == null
                || pipe.container == null
                || pipe.container.isInvalid()
                || pipe.getWorld() != player.worldObj) {
            return false;
        }
        if (player.getDistanceSq(pipe.getX() + 0.5D, pipe.getY() + 0.5D, pipe.getZ() + 0.5D)
                > MAX_INTERACT_DISTANCE_SQ) {
            return false;
        }
        LogisticsSecurityTileEntity station = SimpleServiceLocator.securityStationManager
                .getStation(pipe.getOriginalUpgradeManager().getSecurityID());
        if (station == null) {
            return true;
        }
        SecuritySettings settings = station.getSecuritySettingsForPlayer(player, true);
        return settings == null || settings.openGui;
    }

    public static RequestTablePipe getOpenRequestTable(EntityPlayer player) {
        if (player == null || !(player.openContainer instanceof RequestTableContainer container)) {
            return null;
        }
        RequestTablePipe table = container.getTable();
        if (table == null || table.container == null || table.container.isInvalid()) {
            return null;
        }
        return table;
    }
}
