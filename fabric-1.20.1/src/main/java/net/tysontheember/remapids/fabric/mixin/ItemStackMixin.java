package net.tysontheember.remapids.fabric.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts item stack deserialization from NBT to remap source item IDs
 * to their targets. This handles world migration for items in inventories,
 * containers, and entity data on Fabric.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "of", at = @At("HEAD"))
    private static void remapids$remapItemStack(
            CompoundTag tag,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (tag.contains("id", 8)) { // 8 = TAG_String
            String id = tag.getString("id");
            RemapState.get().getTarget(RemapType.ITEM, id).ifPresent(target ->
                    tag.putString("id", target)
            );
        }
    }
}
