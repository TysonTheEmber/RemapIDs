package net.tysontheember.remapids.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.api.RemapType;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Parses remap JSON files, expands wildcards, flattens chains, and produces
 * a fully resolved {@link RemapConfig}.
 */
public final class RemapLoader {

    private RemapLoader() {}

    /**
     * Load remap config from a directory of JSON files.
     *
     * @param configDir  directory containing *.json remap files
     * @param knownIds   map of RemapType → set of known IDs in that registry
     * @param logger     receives info/warning/error messages
     * @return fully resolved RemapConfig
     */
    public static RemapConfig loadFromDirectory(
            Path configDir,
            Map<RemapType, Set<String>> knownIds,
            Consumer<String> logger
    ) {
        List<RemapEntry> entries = parseFromDirectory(configDir, logger);
        if (entries.isEmpty()) return RemapConfig.EMPTY;
        return resolve(entries, knownIds, logger);
    }

    /**
     * Load remap config from a list of parsed JSON objects.
     *
     * @param jsonFiles  list of parsed remap JSON objects
     * @param knownIds   map of RemapType → set of known IDs in that registry
     * @param logger     receives info/warning/error messages
     * @return fully resolved RemapConfig
     */
    public static RemapConfig load(
            List<JsonObject> jsonFiles,
            Map<RemapType, Set<String>> knownIds,
            Consumer<String> logger
    ) {
        List<RemapEntry> entries = parse(jsonFiles, logger);
        if (entries.isEmpty()) return RemapConfig.EMPTY;
        return resolve(entries, knownIds, logger);
    }

    /**
     * Parse remap JSON files from a directory into raw entries (phase 1).
     * Does not require registry data — can run at any time.
     *
     * @param configDir  directory containing *.json remap files
     * @param logger     receives info/warning/error messages
     * @return list of raw remap entries (may contain wildcards)
     */
    public static List<RemapEntry> parseFromDirectory(Path configDir, Consumer<String> logger) {
        if (!Files.isDirectory(configDir)) {
            logger.accept("[RemapIDs] Config directory not found: " + configDir);
            return List.of();
        }

        List<JsonObject> jsonFiles = new ArrayList<>();
        try (Stream<Path> files = Files.list(configDir)) {
            files.filter(p -> p.toString().endsWith(".json"))
                 .sorted()
                 .forEach(path -> {
                     try (Reader reader = Files.newBufferedReader(path)) {
                         JsonElement element = JsonParser.parseReader(reader);
                         if (element.isJsonObject()) {
                             jsonFiles.add(element.getAsJsonObject());
                         } else {
                             logger.accept("[RemapIDs] Skipping non-object JSON file: " + path);
                         }
                     } catch (IOException e) {
                         logger.accept("[RemapIDs] Failed to read " + path + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            logger.accept("[RemapIDs] Failed to list config directory: " + e.getMessage());
            return List.of();
        }

        return parse(jsonFiles, logger);
    }

    /**
     * Parse a list of JSON objects into raw remap entries (phase 1).
     *
     * @param jsonFiles  list of parsed remap JSON objects
     * @param logger     receives info/warning/error messages
     * @return list of raw remap entries (may contain wildcards)
     */
    public static List<RemapEntry> parse(List<JsonObject> jsonFiles, Consumer<String> logger) {
        List<RemapEntry> rawEntries = new ArrayList<>();
        for (JsonObject json : jsonFiles) {
            rawEntries.addAll(parseJson(json, logger));
        }
        return rawEntries;
    }

    /**
     * Resolve raw remap entries against known registry IDs (phase 2).
     * Expands wildcards, groups by type, flattens chains, and validates targets.
     *
     * @param rawEntries  raw entries from {@link #parse} or {@link #parseFromDirectory}
     * @param knownIds    map of RemapType → set of known IDs in that registry
     * @param logger      receives info/warning/error messages
     * @return fully resolved RemapConfig
     */
    public static RemapConfig resolve(
            List<RemapEntry> rawEntries,
            Map<RemapType, Set<String>> knownIds,
            Consumer<String> logger
    ) {
        if (rawEntries.isEmpty()) {
            return RemapConfig.EMPTY;
        }

        // 1. Collect all known IDs into a flat set for wildcard expansion
        Set<String> allKnownIds = new HashSet<>();
        for (Set<String> ids : knownIds.values()) {
            allKnownIds.addAll(ids);
        }

        // 2. Expand wildcards
        List<RemapEntry> expandedEntries = new ArrayList<>();
        for (RemapEntry entry : rawEntries) {
            if (WildcardExpander.isWildcard(entry.source())) {
                List<RemapEntry> expanded = WildcardExpander.expand(
                        entry.source(), entry.target(), entry.types(), allKnownIds);
                if (expanded.isEmpty()) {
                    logger.accept("[RemapIDs] Wildcard pattern '" + entry.source()
                            + "' matched no known IDs");
                }
                expandedEntries.addAll(expanded);
            } else {
                expandedEntries.add(entry);
            }
        }

        // 3. Group by type and build per-type source→target maps
        EnumMap<RemapType, Map<String, String>> remapsByType = new EnumMap<>(RemapType.class);
        for (RemapEntry entry : expandedEntries) {
            for (RemapType type : RemapType.values()) {
                if (entry.appliesTo(type)) {
                    remapsByType.computeIfAbsent(type, k -> new HashMap<>())
                                .put(entry.source(), entry.target());
                }
            }
        }

        // 4. Flatten chains per type
        for (Map.Entry<RemapType, Map<String, String>> typeEntry : remapsByType.entrySet()) {
            Map<String, String> flattened = ChainFlattener.flatten(typeEntry.getValue(), logger);
            typeEntry.setValue(flattened);
        }

        // 5. Warn about targets not currently in known IDs — they may be modded
        //    items not yet registered at this point. We don't remove them because
        //    the mixin validates against the actual registry at freeze time.
        for (RemapType type : RemapType.registryTypes()) {
            Map<String, String> typeRemaps = remapsByType.get(type);
            if (typeRemaps != null) {
                Set<String> typeKnownIds = knownIds.getOrDefault(type, Set.of());
                RemapValidator.warnUnresolvableTargets(typeRemaps, typeKnownIds, type, logger);
            }
        }

        RemapConfig config = new RemapConfig(remapsByType);
        logger.accept("[RemapIDs] Loaded " + config.size() + " remaps across " + remapsByType.size() + " types");
        return config;
    }

    /**
     * Parse a single JSON remap file into a list of raw entries.
     */
    static List<RemapEntry> parseJson(JsonObject json, Consumer<String> logger) {
        List<RemapEntry> entries = new ArrayList<>();

        if (!json.has("remaps") || !json.get("remaps").isJsonArray()) {
            logger.accept("[RemapIDs] JSON missing 'remaps' array");
            return entries;
        }

        JsonArray remaps = json.getAsJsonArray("remaps");
        for (JsonElement element : remaps) {
            if (!element.isJsonObject()) {
                logger.accept("[RemapIDs] Skipping non-object remap entry");
                continue;
            }

            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("source") || !obj.has("target")) {
                logger.accept("[RemapIDs] Remap entry missing 'source' or 'target'");
                continue;
            }

            String source = obj.get("source").getAsString();
            String target = obj.get("target").getAsString();

            // Parse type filter early — needed for numerical ID resolution
            Set<RemapType> types = EnumSet.noneOf(RemapType.class);
            if (obj.has("types") && obj.get("types").isJsonArray()) {
                for (JsonElement typeElement : obj.getAsJsonArray("types")) {
                    String typeKey = typeElement.getAsString();
                    RemapType type = RemapType.fromJsonKey(typeKey);
                    if (type != null) {
                        types.add(type);
                    } else {
                        logger.accept("[RemapIDs] Unknown remap type: '" + typeKey + "'");
                    }
                }
            }

            // Resolve pre-1.13 numerical IDs via flattening table
            if (NumericalIdResolver.isNumericalId(source)) {
                String resolved = NumericalIdResolver.resolve(source, types, logger);
                if (resolved == null) {
                    continue;
                }
                source = resolved;
            }

            // Validate namespaced ID format (must contain colon)
            if (!source.contains(":") && !source.startsWith(RemapConstants.TAG_PREFIX)) {
                logger.accept("[RemapIDs] Invalid source ID (missing namespace): '" + source + "'");
                continue;
            }
            if (!target.contains(":") && !target.startsWith(RemapConstants.TAG_PREFIX)) {
                logger.accept("[RemapIDs] Invalid target ID (missing namespace): '" + target + "'");
                continue;
            }

            // Validate wildcard consistency
            boolean sourceWild = WildcardExpander.isWildcard(source);
            boolean targetWild = WildcardExpander.isWildcard(target);
            if (sourceWild != targetWild) {
                logger.accept("[RemapIDs] Wildcard mismatch: source and target must both "
                        + "contain '*' or neither. source='" + source + "' target='" + target + "'");
                continue;
            }

            entries.add(new RemapEntry(source, target, types));
        }

        return entries;
    }
}
