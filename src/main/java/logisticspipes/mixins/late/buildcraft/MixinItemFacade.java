package logisticspipes.mixins.late.buildcraft;

import buildcraft.core.proxy.CoreProxy;
import buildcraft.transport.ItemFacade;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps old malformed facade variants searchable when their underlying item cannot supply a display name. */
@Mixin(value = ItemFacade.class, remap = false)
public class MixinItemFacade {

    @WrapOperation(
        method = "getFacadeStateDisplayName",
        at = @At(
            value = "INVOKE",
            target = "Lbuildcraft/core/proxy/CoreProxy;getItemDisplayName(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;",
            remap = false),
        remap = false)
    private static String LogisticsPipes$safeFacadeName(CoreProxy proxy, ItemStack stack, Operation<String> original) {
        try {
            return original.call(proxy, stack);
        } catch (RuntimeException invalidVariant) {
            // Lost high metadata bits cannot be recovered from old byte tags. Avoid another display-name lookup.
            String name = Item.itemRegistry.getNameForObject(stack.getItem());
            return (name == null ? "Unknown block" : name) + "@" + stack.getItemDamage();
        }
    }
}
