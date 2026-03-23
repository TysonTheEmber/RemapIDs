package net.tysontheember.remapids.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Recursively rewrites item/tag references in recipe and loot table JSON.
 * Used by RecipeManagerMixin and LootDataTypeMixin on all loaders.
 */
public final class JsonRemapper {

    private static int rewriteCount = 0;

    private JsonRemapper() {}

    /** Returns and resets the rewrite counter (for diagnostics). */
    public static int drainRewriteCount() {
        int count = rewriteCount;
        rewriteCount = 0;
        return count;
    }

    /**
     * Recursively walk a JSON element, replacing item and tag ID references
     * using standard ITEM/BLOCK/TAG remaps only.
     */
    public static void remapJson(JsonElement element, RemapConfig config) {
        remapJson(element, config, EnumSet.noneOf(RemapType.class));
    }

    /**
     * Recursively walk a JSON element, replacing item and tag ID references.
     *
     * <p>{@code extraItemTypes} specifies additional remap types to check when
     * processing "item" and "name" fields. For example, passing {@code RECIPE}
     * allows recipe-scoped remaps to rewrite ingredient references.</p>
     */
    public static void remapJson(JsonElement element, RemapConfig config, Set<RemapType> extraItemTypes) {
        if (element.isJsonObject()) {
            remapObject(element.getAsJsonObject(), config, extraItemTypes);
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                remapJson(child, config, extraItemTypes);
            }
        }
    }

    private static void remapObject(JsonObject obj, RemapConfig config, Set<RemapType> extraItemTypes) {
        // Remap "item" fields (recipe ingredients, results)
        remapStringField(obj, "item", RemapType.ITEM, config, extraItemTypes);

        // Remap "tag" fields (ingredient tag references)
        remapStringField(obj, "tag", RemapType.TAG, config, EnumSet.noneOf(RemapType.class));

        // Remap "name" fields (loot table item entries use "name" for item ID)
        remapStringField(obj, "name", RemapType.ITEM, config, extraItemTypes);

        // Remap "block" fields (some recipes/loot reference blocks)
        remapStringField(obj, "block", RemapType.BLOCK, config, EnumSet.noneOf(RemapType.class));

        // Replace tag ingredients with item ingredients via extra types (e.g., RECIPE remaps)
        // Allows configs like: { "source": "#forge:ingots/gold", "target": "minecraft:diamond", "types": ["recipe"] }
        remapTagToItem(obj, config, extraItemTypes);

        // Recurse into all values
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            remapJson(entry.getValue(), config, extraItemTypes);
        }
    }

    private static void remapStringField(JsonObject obj, String key, RemapType primaryType,
                                         RemapConfig config, Set<RemapType> extraTypes) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) return;

        JsonPrimitive prim = obj.getAsJsonPrimitive(key);
        if (!prim.isString()) return;

        String value = prim.getAsString();

        // Handle tag prefix for TAG type lookups
        String lookupValue = value;
        if (primaryType == RemapType.TAG && lookupValue.startsWith("#")) {
            lookupValue = lookupValue.substring(1);
        }

        // Check primary type first
        Optional<String> target = config.getTarget(primaryType, lookupValue);

        // Check extra types if primary didn't match
        if (target.isEmpty()) {
            for (RemapType extra : extraTypes) {
                target = config.getTarget(extra, value);
                if (target.isPresent()) break;
            }
        }

        target.ifPresent(t -> {
            String newValue = (primaryType == RemapType.TAG && value.startsWith("#")) ? "#" + t : t;
            rewriteCount++;
            obj.addProperty(key, newValue);
        });
    }

    /**
     * Check if a "tag" field can be replaced with an "item" field via extra remap types.
     * Supports recipe remaps like {@code #forge:ingots/gold -> minecraft:diamond},
     * which replaces {@code {"tag":"forge:ingots/gold"}} with {@code {"item":"minecraft:diamond"}}.
     */
    private static void remapTagToItem(JsonObject obj, RemapConfig config, Set<RemapType> extraTypes) {
        if (extraTypes.isEmpty()) return;
        if (!obj.has("tag") || !obj.get("tag").isJsonPrimitive()) return;

        JsonPrimitive prim = obj.getAsJsonPrimitive("tag");
        if (!prim.isString()) return;

        String tagValue = prim.getAsString();

        // Look up "#tagValue" in extra types (e.g., RECIPE remaps)
        for (RemapType extra : extraTypes) {
            Optional<String> target = config.getTarget(extra, "#" + tagValue);
            if (target.isPresent()) {
                String targetValue = target.get();
                obj.remove("tag");
                if (targetValue.startsWith("#")) {
                    // Target is also a tag: #forge:gems -> keep as tag
                    obj.addProperty("tag", targetValue.substring(1));
                } else {
                    // Target is an item: replace tag with item
                    obj.addProperty("item", targetValue);
                }
                rewriteCount++;
                return;
            }
        }
    }
}
