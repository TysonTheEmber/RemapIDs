package net.tysontheember.remapids.fabric21.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
 * Intercepts tag loading to apply tag ID remaps.
 *
 * <p>In 1.21.1, {@code loadAndBuild()} returns resolved objects, so we intercept
 * {@code load()} which still returns raw {@code EntryWithSource} entries.</p>
 */
@Mixin(TagLoader.class)
public class TagLoaderMixin {

    private static final Logger REMAPIDS_LOGGER = LoggerFactory.getLogger("RemapIDs");

    @Inject(method = "load", at = @At("RETURN"), cancellable = true)
    private void remapids$modifyTags(
            ResourceManager resourceManager,
            CallbackInfoReturnable<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> cir
    ) {
        Map<String, String> tagRemaps = RemapState.get().getAllForType(RemapType.TAG);
        if (tagRemaps.isEmpty()) return;

        Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags = new HashMap<>(cir.getReturnValue());
        boolean modified = false;

        for (Map.Entry<String, String> remap : tagRemaps.entrySet()) {
            String sourceStr = remap.getKey().startsWith("#") ? remap.getKey().substring(1) : remap.getKey();
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

        if (modified) {
            cir.setReturnValue(tags);
        }
    }
}
