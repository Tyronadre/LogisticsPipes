package logisticspipes.mixins.late.buildcraft;

import buildcraft.transport.ItemFacade;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves full block metadata for facades of GregTech frames and other extended-metadata blocks. */
@Mixin(value = ItemFacade.FacadeState.class, remap = false)
public class MixinFacadeState {

    @Shadow
    @Final
    @Mutable
    public int metadata;

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"), remap = false)
    private void LogisticsPipes$readFullMetadata(NBTTagCompound nbt, CallbackInfo ci) {
        // getInteger also reads legacy byte tags without changing their value.
        metadata = nbt.getInteger("metadata");
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"), remap = false)
    private void LogisticsPipes$writeFullMetadata(NBTTagCompound nbt, CallbackInfo ci) {
        // Keep existing NBT (and recipe matching) for ordinary byte-sized variants.
        if (metadata < Byte.MIN_VALUE || metadata > Byte.MAX_VALUE) {
            nbt.setInteger("metadata", metadata);
        }
    }
}
