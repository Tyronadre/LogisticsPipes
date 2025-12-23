package logisticspipes.interfaces.routing;

import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipes.PipeLogisticsChassi;
import net.minecraft.nbt.NBTTagCompound;

public interface IAdditionalTargetInformation {
    static IAdditionalTargetInformation createFromNBT(NBTTagCompound nbt) {
        if (!nbt.hasKey("ai_type")) return null;
        switch (Type.ofOrdinal(nbt.getInteger("ai_type"))) {
            case ChassiTargetInformation:
                if (!nbt.hasKey("ai_moduleSlot")) return null;
                return new PipeLogisticsChassi.ChassiTargetInformation(nbt.getInteger("ai_moduleSlot"));
            case CraftingChassieInformation:
                if (!nbt.hasKey("ai_moduleSlot")) return null;
                if (!nbt.hasKey("ai_craftingSlot")) return null;
                return new ModuleCrafter.CraftingChassieInformation(nbt.getInteger("ai_craftingSlot"), nbt.getInteger("ai_moduleSlot"));
            default: return null;
        }
    }

    void writeToNBT(NBTTagCompound nbtTagCompound);

    enum Type {
        ChassiTargetInformation,
        CraftingChassieInformation,
        PatternSupplierTargetInformation,
        SupplierTargetInformation;

        public static Type ofOrdinal(int typeID) {
            return Type.values()[typeID];
        }
    }
}
