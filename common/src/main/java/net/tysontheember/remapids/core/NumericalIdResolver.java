package net.tysontheember.remapids.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Resolves pre-1.13 numerical block/item IDs to their post-flattening
 * namespaced string IDs using a built-in lookup table.
 */
public final class NumericalIdResolver {

    private static final Pattern NUMERIC_ONLY = Pattern.compile("^\\d+$");
    private static final Pattern NUMERIC_META = Pattern.compile("^\\d+:\\d+$");

    private static volatile FlatteningTable table;

    private NumericalIdResolver() {}

    /**
     * Returns true if the source string looks like a pre-1.13 numerical ID.
     * Matches pure numbers ("1", "35") or number:metadata ("1:3", "35:14").
     */
    public static boolean isNumericalId(String source) {
        if (source == null || source.isEmpty()) return false;
        return NUMERIC_ONLY.matcher(source).matches()
                || NUMERIC_META.matcher(source).matches();
    }

    /**
     * Resolve a numerical ID to its post-flattening string ID.
     *
     * @param numericalId the numerical ID string (e.g. "1", "35:14")
     * @param types       the remap types specified for this entry (used to pick blocks vs items table)
     * @param logger      receives warning messages
     * @return the resolved string ID, or null if not found in the flattening table
     */
    public static String resolve(String numericalId, Set<RemapType> types, Consumer<String> logger) {
        FlatteningTable t = getTable(logger);
        if (t == null) return null;

        boolean wantBlock = types.isEmpty() || types.contains(RemapType.BLOCK);
        boolean wantItem = types.isEmpty() || types.contains(RemapType.ITEM);

        // Try exact match first
        String result = null;
        if (wantBlock) {
            result = t.blocks.get(numericalId);
        }
        if (result == null && wantItem) {
            result = t.items.get(numericalId);
        }

        // If bare number with no metadata, try appending :0
        if (result == null && NUMERIC_ONLY.matcher(numericalId).matches()) {
            String withZero = numericalId + ":0";
            if (wantBlock) {
                result = t.blocks.get(withZero);
            }
            if (result == null && wantItem) {
                result = t.items.get(withZero);
            }
        }

        if (result == null) {
            logger.accept("[RemapIDs] Unknown numerical ID: '" + numericalId
                    + "' (not found in flattening table — may be a modded ID)");
        } else {
            logger.accept("[RemapIDs] Resolved numerical ID '" + numericalId + "' -> '" + result + "'");
        }

        return result;
    }

    /**
     * Load custom numerical ID mappings from a user-provided JSON file.
     * These are merged on top of the built-in vanilla table, so custom
     * entries override built-in ones. Call this before any remaps are loaded.
     *
     * @param customTablePath path to a JSON file with the same format as the built-in table
     * @param logger          receives info/warning messages
     */
    public static void loadCustomMappings(Path customTablePath, Consumer<String> logger) {
        if (!Files.isRegularFile(customTablePath)) return;

        // Ensure built-in table is loaded first
        FlatteningTable t = getTable(logger);

        try (Reader reader = Files.newBufferedReader(customTablePath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Map<String, String> customBlocks = parseSection(root, "blocks");
            Map<String, String> customItems = parseSection(root, "items");

            if (customBlocks.isEmpty() && customItems.isEmpty()) return;

            // Merge custom entries on top of built-in table
            Map<String, String> mergedBlocks = new HashMap<>(t != null ? t.blocks : Map.of());
            Map<String, String> mergedItems = new HashMap<>(t != null ? t.items : Map.of());
            mergedBlocks.putAll(customBlocks);
            mergedItems.putAll(customItems);
            table = new FlatteningTable(mergedBlocks, mergedItems);

            logger.accept("[RemapIDs] Loaded custom numerical IDs: "
                    + customBlocks.size() + " blocks, " + customItems.size() + " items from "
                    + customTablePath.getFileName());
        } catch (IOException e) {
            logger.accept("[RemapIDs] Failed to load custom numerical IDs from "
                    + customTablePath + ": " + e.getMessage());
        }
    }

    private static FlatteningTable getTable(Consumer<String> logger) {
        if (table != null) return table;
        synchronized (NumericalIdResolver.class) {
            if (table != null) return table;
            table = loadBuiltinTable(logger);
            return table;
        }
    }

    private static FlatteningTable loadBuiltinTable(Consumer<String> logger) {
        try (InputStream is = NumericalIdResolver.class.getResourceAsStream(
                RemapConstants.FLATTENING_TABLE_RESOURCE)) {
            if (is == null) {
                logger.accept("[RemapIDs] Flattening table resource not found: "
                        + RemapConstants.FLATTENING_TABLE_RESOURCE);
                return new FlatteningTable(new HashMap<>(), new HashMap<>());
            }

            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            Map<String, String> blocks = parseSection(root, "blocks");
            Map<String, String> items = parseSection(root, "items");

            logger.accept("[RemapIDs] Loaded flattening table: " + blocks.size()
                    + " blocks, " + items.size() + " items");
            return new FlatteningTable(blocks, items);
        } catch (IOException e) {
            logger.accept("[RemapIDs] Failed to load flattening table: " + e.getMessage());
            return new FlatteningTable(new HashMap<>(), new HashMap<>());
        }
    }

    private static Map<String, String> parseSection(JsonObject root, String key) {
        Map<String, String> map = new HashMap<>();
        if (root.has(key) && root.get(key).isJsonObject()) {
            JsonObject section = root.getAsJsonObject(key);
            for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return map;
    }

    /** Visible for testing. */
    public static void resetTable() {
        table = null;
    }

    private record FlatteningTable(Map<String, String> blocks, Map<String, String> items) {}
}
