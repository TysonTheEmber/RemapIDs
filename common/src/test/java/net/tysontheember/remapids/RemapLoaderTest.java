package net.tysontheember.remapids;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapLoader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RemapLoaderTest {

    private final List<String> logs = new ArrayList<>();

    @Test
    void testSimpleRemap() {
        String json = """
            {
              "remaps": [
                {
                  "source": "iceandfire:silver_ingot",
                  "target": "othermod:silver_ingot",
                  "types": ["item"]
                }
              ]
            }
            """;

        RemapConfig config = loadFromJson(json, knownIds("othermod:silver_ingot"));

        assertTrue(config.hasRemapFor(RemapType.ITEM, "iceandfire:silver_ingot"));
        assertEquals("othermod:silver_ingot",
                config.getTarget(RemapType.ITEM, "iceandfire:silver_ingot").orElse(null));
        assertFalse(config.hasRemapFor(RemapType.BLOCK, "iceandfire:silver_ingot"));
    }

    @Test
    void testAllTypesWhenNoTypesSpecified() {
        String json = """
            {
              "remaps": [
                {
                  "source": "modA:thing",
                  "target": "modB:thing"
                }
              ]
            }
            """;

        RemapConfig config = loadFromJson(json, knownIds("modB:thing"));

        for (RemapType type : RemapType.registryTypes()) {
            assertTrue(config.hasRemapFor(type, "modA:thing"),
                    "Expected remap for type " + type);
        }
    }

    @Test
    void testMissingSourceField() {
        String json = """
            {
              "remaps": [
                { "target": "modB:thing" }
              ]
            }
            """;

        RemapConfig config = loadFromJson(json, knownIds());
        assertTrue(config.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.contains("missing 'source'")));
    }

    @Test
    void testMissingRemapsArray() {
        String json = "{}";
        RemapConfig config = loadFromJson(json, knownIds());
        assertTrue(config.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.contains("missing 'remaps' array")));
    }

    @Test
    void testInvalidNamespace() {
        String json = """
            {
              "remaps": [
                { "source": "no_namespace", "target": "modB:thing" }
              ]
            }
            """;

        RemapConfig config = loadFromJson(json, knownIds());
        assertTrue(config.isEmpty());
        assertTrue(logs.stream().anyMatch(l -> l.contains("missing namespace")));
    }

    @Test
    void testUnknownTypeIgnored() {
        String json = """
            {
              "remaps": [
                {
                  "source": "modA:thing",
                  "target": "modB:thing",
                  "types": ["item", "unknown_type"]
                }
              ]
            }
            """;

        RemapConfig config = loadFromJson(json, knownIds("modB:thing"));
        assertTrue(config.hasRemapFor(RemapType.ITEM, "modA:thing"));
        assertTrue(logs.stream().anyMatch(l -> l.contains("Unknown remap type")));
    }

    @Test
    void testTargetNotInKnownIdsIsPreserved() {
        String json = """
            {
              "remaps": [
                {
                  "source": "modA:thing",
                  "target": "modB:nonexistent",
                  "types": ["block"]
                }
              ]
            }
            """;

        // knownIds doesn't contain the target — entry is still preserved
        // because validation is non-destructive (modded targets may not
        // be registered yet at resolve time)
        RemapConfig config = loadFromJson(json, knownIds());
        assertTrue(config.hasRemapFor(RemapType.BLOCK, "modA:thing"));
        assertEquals("modB:nonexistent",
                config.getTarget(RemapType.BLOCK, "modA:thing").orElse(null));
        assertTrue(logs.stream().anyMatch(l -> l.contains("not found")));
    }

    @Test
    void testModdedTargetSurvivesValidation() {
        String json = """
            {
              "remaps": [
                {
                  "source": "minecraft:iron_ingot",
                  "target": "othermod:silver_ingot",
                  "types": ["item"]
                }
              ]
            }
            """;

        // knownIds only has vanilla items — modded target is not present,
        // but the entry must survive so the mixin can apply it at freeze time
        RemapConfig config = loadFromJson(json, knownIds("minecraft:iron_ingot"));
        assertTrue(config.hasRemapFor(RemapType.ITEM, "minecraft:iron_ingot"));
        assertEquals("othermod:silver_ingot",
                config.getTarget(RemapType.ITEM, "minecraft:iron_ingot").orElse(null));
    }

    @Test
    void testMultipleRemapsFromMultipleFiles() {
        String json1 = """
            {
              "remaps": [
                { "source": "modA:a", "target": "modB:a", "types": ["item"] }
              ]
            }
            """;
        String json2 = """
            {
              "remaps": [
                { "source": "modA:b", "target": "modB:b", "types": ["item"] }
              ]
            }
            """;

        JsonObject j1 = JsonParser.parseString(json1).getAsJsonObject();
        JsonObject j2 = JsonParser.parseString(json2).getAsJsonObject();

        Map<RemapType, Set<String>> known = knownIds("modB:a", "modB:b");
        RemapConfig config = RemapLoader.load(List.of(j1, j2), known, logs::add);

        assertTrue(config.hasRemapFor(RemapType.ITEM, "modA:a"));
        assertTrue(config.hasRemapFor(RemapType.ITEM, "modA:b"));
    }

    @Test
    void testTwoPhaseSplitMatchesOneShot() {
        String json = """
            {
              "remaps": [
                {
                  "source": "modA:thing",
                  "target": "modB:thing",
                  "types": ["item"]
                }
              ]
            }
            """;

        Map<RemapType, Set<String>> known = knownIds("modB:thing");
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        // One-shot
        List<String> logs1 = new ArrayList<>();
        RemapConfig oneShot = RemapLoader.load(List.of(obj), known, logs1::add);

        // Two-phase
        List<String> logs2 = new ArrayList<>();
        List<RemapEntry> entries = RemapLoader.parse(List.of(obj), logs2::add);
        assertFalse(entries.isEmpty());
        RemapConfig twoPhase = RemapLoader.resolve(entries, known, logs2::add);

        assertEquals(oneShot.getTarget(RemapType.ITEM, "modA:thing"),
                twoPhase.getTarget(RemapType.ITEM, "modA:thing"));
        assertEquals(oneShot.size(), twoPhase.size());
    }

    private RemapConfig loadFromJson(String json, Map<RemapType, Set<String>> knownIds) {
        logs.clear();
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return RemapLoader.load(List.of(obj), knownIds, logs::add);
    }

    private Map<RemapType, Set<String>> knownIds(String... ids) {
        Set<String> idSet = Set.of(ids);
        Map<RemapType, Set<String>> map = new EnumMap<>(RemapType.class);
        for (RemapType type : RemapType.registryTypes()) {
            map.put(type, idSet);
        }
        return map;
    }
}
