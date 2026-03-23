package net.tysontheember.remapids.forge.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.JsonRemapper;
import net.tysontheember.remapids.core.RemapState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Intercepts recipe loading to rewrite item/tag references in raw JSON
 * before deserialization, and to re-key recipe IDs.
 */
@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    private static final Logger REMAPIDS_LOGGER = LoggerFactory.getLogger("RemapIDs");

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void remapids$beforeApply(
            Map<ResourceLocation, JsonElement> recipes,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci
    ) {
        RemapConfig config = RemapState.get();
        REMAPIDS_LOGGER.info("[RemapIDs] RecipeManager.apply() intercepted with {} recipes, config empty={}",
                recipes.size(), config.isEmpty());
        if (config.isEmpty()) return;

        Map<String, String> recipeRemaps = config.getAllForType(RemapType.RECIPE);
        REMAPIDS_LOGGER.info("[RemapIDs] RECIPE remaps: {}", recipeRemaps);
        int rewrittenContent = 0;
        int rekeyed = 0;

        // 1. Re-key recipe IDs
        if (!recipeRemaps.isEmpty()) {
            Map<ResourceLocation, JsonElement> toAdd = new HashMap<>();
            Iterator<Map.Entry<ResourceLocation, JsonElement>> iter = recipes.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<ResourceLocation, JsonElement> entry = iter.next();
                String target = recipeRemaps.get(entry.getKey().toString());
                if (target != null) {
                    ResourceLocation targetRL = ResourceLocation.tryParse(target);
                    if (targetRL != null) {
                        iter.remove();
                        toAdd.put(targetRL, entry.getValue());
                        rekeyed++;
                    }
                }
            }
            recipes.putAll(toAdd);
        }

        // Log golden apple recipe before rewrite for debugging
        ResourceLocation goldenAppleRL = ResourceLocation.tryParse("minecraft:golden_apple");
        if (goldenAppleRL != null && recipes.containsKey(goldenAppleRL)) {
            REMAPIDS_LOGGER.info("[RemapIDs] golden_apple recipe BEFORE: {}", recipes.get(goldenAppleRL));
        }

        // 2. Rewrite item/tag references in recipe JSON (also apply RECIPE remaps to ingredients)
        JsonRemapper.drainRewriteCount(); // reset counter
        for (JsonElement element : recipes.values()) {
            JsonRemapper.remapJson(element, config, EnumSet.of(RemapType.RECIPE));
            rewrittenContent++;
        }
        int rewrites = JsonRemapper.drainRewriteCount();

        // Log golden apple recipe after rewrite
        if (goldenAppleRL != null && recipes.containsKey(goldenAppleRL)) {
            REMAPIDS_LOGGER.info("[RemapIDs] golden_apple recipe AFTER: {}", recipes.get(goldenAppleRL));
        }

        REMAPIDS_LOGGER.info("[RemapIDs] Processed {} recipes ({} re-keyed, {} field rewrites)",
                rewrittenContent, rekeyed, rewrites);
    }
}
