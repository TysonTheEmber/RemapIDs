package net.tysontheember.remapids;

import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemapConfigTest {

    @Test
    void testEmptyConfig() {
        RemapConfig config = RemapConfig.EMPTY;

        assertTrue(config.isEmpty());
        assertEquals(0, config.size());
        assertFalse(config.hasRemapFor(RemapType.BLOCK, "any:thing"));
        assertTrue(config.getTarget(RemapType.ITEM, "any:thing").isEmpty());
        assertTrue(config.getAllForType(RemapType.BLOCK).isEmpty());
    }

    @Test
    void testLookupHit() {
        Map<String, String> itemRemaps = new HashMap<>();
        itemRemaps.put("modA:silver_ingot", "modB:silver_ingot");

        EnumMap<RemapType, Map<String, String>> map = new EnumMap<>(RemapType.class);
        map.put(RemapType.ITEM, itemRemaps);

        RemapConfig config = new RemapConfig(map);

        assertTrue(config.hasRemapFor(RemapType.ITEM, "modA:silver_ingot"));
        assertEquals("modB:silver_ingot",
                config.getTarget(RemapType.ITEM, "modA:silver_ingot").orElse(null));
        assertEquals(1, config.size());
    }

    @Test
    void testLookupMiss() {
        Map<String, String> itemRemaps = new HashMap<>();
        itemRemaps.put("modA:silver_ingot", "modB:silver_ingot");

        EnumMap<RemapType, Map<String, String>> map = new EnumMap<>(RemapType.class);
        map.put(RemapType.ITEM, itemRemaps);

        RemapConfig config = new RemapConfig(map);

        assertFalse(config.hasRemapFor(RemapType.ITEM, "modA:gold_ingot"));
        assertFalse(config.hasRemapFor(RemapType.BLOCK, "modA:silver_ingot"));
        assertTrue(config.getTarget(RemapType.BLOCK, "modA:silver_ingot").isEmpty());
    }

    @Test
    void testImmutability() {
        Map<String, String> itemRemaps = new HashMap<>();
        itemRemaps.put("a:x", "b:x");

        EnumMap<RemapType, Map<String, String>> map = new EnumMap<>(RemapType.class);
        map.put(RemapType.ITEM, itemRemaps);

        RemapConfig config = new RemapConfig(map);

        // Modifying the original map should not affect the config
        itemRemaps.put("a:y", "b:y");
        assertFalse(config.hasRemapFor(RemapType.ITEM, "a:y"));

        // getAllForType returns unmodifiable map
        assertThrows(UnsupportedOperationException.class, () ->
                config.getAllForType(RemapType.ITEM).put("c:x", "d:x"));
    }

    @Test
    void testMultipleTypes() {
        Map<String, String> blockRemaps = new HashMap<>();
        blockRemaps.put("a:ore", "b:ore");

        Map<String, String> itemRemaps = new HashMap<>();
        itemRemaps.put("a:ingot", "b:ingot");
        itemRemaps.put("a:nugget", "b:nugget");

        EnumMap<RemapType, Map<String, String>> map = new EnumMap<>(RemapType.class);
        map.put(RemapType.BLOCK, blockRemaps);
        map.put(RemapType.ITEM, itemRemaps);

        RemapConfig config = new RemapConfig(map);

        assertEquals(3, config.size());
        assertFalse(config.isEmpty());
        assertTrue(config.hasRemapFor(RemapType.BLOCK, "a:ore"));
        assertTrue(config.hasRemapFor(RemapType.ITEM, "a:ingot"));
        assertTrue(config.hasRemapFor(RemapType.ITEM, "a:nugget"));
    }
}
