package logisticspipes.crafting;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Objects;

/**
 * Identifies the satellite assigned to one pattern output slot.
 */
public final class PatternByproductTarget {

    private static final String OUTPUT_SLOT_SUFFIX = "OutputSlot";
    private static final String SATELLITE_ID_SUFFIX = "SatelliteId";
    private static final String SATELLITE_UUID_SUFFIX = "SatelliteUuid";
    private static final String FLUID_SUFFIX = "Fluid";

    private final int outputSlot;
    private final int satelliteId;
    private final String satelliteUuid;
    private final boolean fluid;

    public PatternByproductTarget(int outputSlot, int satelliteId, String satelliteUuid, boolean fluid) {
        this.outputSlot = Math.max(0, outputSlot);
        this.satelliteId = Math.max(0, satelliteId);
        this.satelliteUuid = satelliteUuid == null ? "" : satelliteUuid;
        this.fluid = fluid;
    }

    static PatternByproductTarget readFromNBT(NBTTagCompound tag, String prefix) {
        int satelliteId = tag.getInteger(prefix + SATELLITE_ID_SUFFIX);
        String satelliteUuid = tag.getString(prefix + SATELLITE_UUID_SUFFIX);
        if (satelliteId <= 0 && satelliteUuid.isEmpty()) {
            return null;
        }
        return new PatternByproductTarget(
            tag.getInteger(prefix + OUTPUT_SLOT_SUFFIX),
            satelliteId,
            satelliteUuid,
            tag.getBoolean(prefix + FLUID_SUFFIX));
    }

    public int getOutputSlot() {
        return outputSlot;
    }

    public int getSatelliteId() {
        return satelliteId;
    }

    public String getSatelliteUuid() {
        return satelliteUuid;
    }

    public boolean isFluid() {
        return fluid;
    }

    public boolean isConfigured() {
        return satelliteId > 0 || !satelliteUuid.isEmpty();
    }

    void writeToNBT(NBTTagCompound tag, String prefix) {
        tag.setInteger(prefix + OUTPUT_SLOT_SUFFIX, outputSlot);
        tag.setInteger(prefix + SATELLITE_ID_SUFFIX, satelliteId);
        tag.setString(prefix + SATELLITE_UUID_SUFFIX, satelliteUuid);
        tag.setBoolean(prefix + FLUID_SUFFIX, fluid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PatternByproductTarget other)) {
            return false;
        }
        return outputSlot == other.outputSlot
            && satelliteId == other.satelliteId
            && fluid == other.fluid
            && satelliteUuid.equals(other.satelliteUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputSlot, satelliteId, satelliteUuid, fluid);
    }
}
