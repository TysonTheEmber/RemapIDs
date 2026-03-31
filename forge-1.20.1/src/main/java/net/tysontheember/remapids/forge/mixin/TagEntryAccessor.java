package net.tysontheember.remapids.forge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TagEntry.class)
public interface TagEntryAccessor {

    @Accessor("id")
    ResourceLocation remapids$getId();

    @Accessor("tag")
    boolean remapids$isTag();

    @Accessor("required")
    boolean remapids$isRequired();
}
