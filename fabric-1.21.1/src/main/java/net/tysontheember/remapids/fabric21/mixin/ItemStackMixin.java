package net.tysontheember.remapids.fabric21.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts item stack deserialization to remap source item IDs.
 * In 1.21.1, {@code ItemStack.of(CompoundTag)} was replaced by
 * {@code ItemStack.parseOptional(HolderLookup.Provider, CompoundTag)}.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "parseOptional", at = @At("HEAD"))
    private static void remapids$remapItemStack(
            HolderLookup.Provider provider,
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
