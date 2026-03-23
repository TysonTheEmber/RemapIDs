package net.tysontheember.remapids.fabric.mixin;

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
 * Intercepts block state deserialization from NBT to remap source block IDs
 * to their targets. This handles world migration on Fabric, which has no
 * MissingMappingsEvent.
 *
 * <p>The chunk palette deserializes block states through this method, so
 * this covers placed blocks in saved chunks.</p>
 */
@Mixin(NbtUtils.class)
public class NbtUtilsMixin {

    @Inject(method = "readBlockState", at = @At("HEAD"))
    private static void remapids$remapBlockState(
            HolderGetter<Block> holderGetter,
            CompoundTag tag,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (tag.contains("Name", 8)) { // 8 = TAG_String
            String name = tag.getString("Name");
            RemapState.get().getTarget(RemapType.BLOCK, name).ifPresent(target ->
                    tag.putString("Name", target)
            );
        }
    }
}
