package net.tysontheember.remapids.fabric.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.IdMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IdMapper.class)
public interface IdMapperAccessor<T> {
    @Accessor("tToId")
    Object2IntMap<T> remapids$getTToId();
}
