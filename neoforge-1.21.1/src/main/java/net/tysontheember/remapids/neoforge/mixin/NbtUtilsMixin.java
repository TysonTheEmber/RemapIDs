package net.tysontheember.remapids.neoforge.mixin;

import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts block state deserialization from NBT to remap source block IDs.
 * NeoForge 1.21.1 has no MissingMappingsEvent, so this mixin handles
 * world migration for blocks.
 */
@Mixin(NbtUtils.class)
public class NbtUtilsMixin {

    @Inject(method = "readBlockState", at = @At("HEAD"))
    private static void remapids$remapBlockState(
            HolderGetter<Block> holderGetter,
            CompoundTag tag,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (tag.contains("Name", 8)) {
            String name = tag.getString("Name");
            RemapState.get().getTarget(RemapType.BLOCK, name).ifPresent(target ->
                    tag.putString("Name", target)
            );
        }
    }
}
