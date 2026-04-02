package net.tysontheember.remapids.fabric21.mixin;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.IdMapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IdMapper.class)
public interface IdMapperAccessor<T> {
    @Accessor("tToId")
    Reference2IntMap<T> remapids$getTToId();
}
