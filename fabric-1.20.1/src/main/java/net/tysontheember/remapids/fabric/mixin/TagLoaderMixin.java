package net.tysontheember.remapids.fabric.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intercepts tag loading to apply tag remaps.
 *
 * <p>Pass 1 handles tag ID remapping: merging all entries from a source tag into
 * a target tag (for {@code #}-prefixed remap sources).</p>
 *
 * <p>Pass 2 handles element-within-tag replacement: replacing individual item/block
 * entries inside tag definitions with their remap targets (for non-{@code #}-prefixed
 * sources). This complements the registry aliases injected by {@link MappedRegistryMixin}.</p>
 */
@Mixin(TagLoader.class)
public class TagLoaderMixin {

    private static final Logger REMAPIDS_LOGGER = LoggerFactory.getLogger("RemapIDs");

    @Inject(method = "loadAndBuild", at = @At("RETURN"), cancellable = true)
    private void remapids$modifyTags(
            ResourceManager resourceManager,
            CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir
    ) {
        Map<String, String> tagRemaps = RemapState.get().getAllForType(RemapType.TAG);
        if (tagRemaps.isEmpty()) return;

        Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags = new HashMap<>(cir.getReturnValue());
        boolean modified = false;

        // Pass 1: Tag-to-tag merging (for #-prefixed sources)
        for (Map.Entry<String, String> remap : tagRemaps.entrySet()) {
            if (!remap.getKey().startsWith("#")) continue;

            String sourceStr = remap.getKey().substring(1);
            String targetStr = remap.getValue().startsWith("#") ? remap.getValue().substring(1) : remap.getValue();
            ResourceLocation sourceRL = ResourceLocation.tryParse(sourceStr);
            ResourceLocation targetRL = ResourceLocation.tryParse(targetStr);
            if (sourceRL == null || targetRL == null) continue;

            List<TagLoader.EntryWithSource> sourceEntries = tags.get(sourceRL);
            if (sourceEntries != null && !sourceEntries.isEmpty()) {
                List<TagLoader.EntryWithSource> targetEntries = tags.computeIfAbsent(targetRL, k -> new ArrayList<>());
                targetEntries.addAll(sourceEntries);
                modified = true;
                REMAPIDS_LOGGER.debug("[RemapIDs] Merged tag #{} ({} entries) into #{}",
                        sourceStr, sourceEntries.size(), targetStr);
            }
        }

        // Pass 2: Element-within-tag replacement (for non-#-prefixed sources)
        Map<ResourceLocation, ResourceLocation> entryRemaps = new HashMap<>();
        for (Map.Entry<String, String> remap : tagRemaps.entrySet()) {
            if (remap.getKey().startsWith("#")) continue;
            ResourceLocation sourceRL = ResourceLocation.tryParse(remap.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(remap.getValue());
            if (sourceRL != null && targetRL != null) {
                entryRemaps.put(sourceRL, targetRL);
            }
        }

        if (!entryRemaps.isEmpty()) {
            for (Map.Entry<ResourceLocation, List<TagLoader.EntryWithSource>> tagEntry : tags.entrySet()) {
                List<TagLoader.EntryWithSource> entries = tagEntry.getValue();
                for (int i = 0; i < entries.size(); i++) {
                    TagLoader.EntryWithSource ews = entries.get(i);
                    TagEntryAccessor accessor = (TagEntryAccessor) (Object) ews.entry();

                    if (!accessor.remapids$isTag()) {
                        ResourceLocation target = entryRemaps.get(accessor.remapids$getId());
                        if (target != null) {
                            TagEntry replacement = accessor.remapids$isRequired()
                                    ? TagEntry.element(target)
                                    : TagEntry.optionalElement(target);
                            entries.set(i, new TagLoader.EntryWithSource(replacement, ews.source()));
                            modified = true;
                            REMAPIDS_LOGGER.debug("[RemapIDs] Replaced entry {} with {} in tag #{}",
                                    accessor.remapids$getId(), target, tagEntry.getKey());
                        }
                    }
                }
            }
        }

        if (modified) {
            cir.setReturnValue(tags);
        }
    }
}
