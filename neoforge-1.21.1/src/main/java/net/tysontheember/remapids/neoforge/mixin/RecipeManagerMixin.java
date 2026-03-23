package net.tysontheember.remapids.neoforge.mixin;

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
        if (config.isEmpty()) return;

        Map<String, String> recipeRemaps = config.getAllForType(RemapType.RECIPE);
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

        // 2. Rewrite item/tag references in recipe JSON (also apply RECIPE remaps to ingredients)
        for (JsonElement element : recipes.values()) {
            JsonRemapper.remapJson(element, config, EnumSet.of(RemapType.RECIPE));
        }

        if (rekeyed > 0) {
            REMAPIDS_LOGGER.debug("[RemapIDs] Re-keyed {} recipe IDs", rekeyed);
        }
    }
}
