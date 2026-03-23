package net.tysontheember.remapids;

import net.tysontheember.remapids.core.ChainFlattener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChainFlattenerTest {

    private final List<String> errors = new ArrayList<>();

    @Test
    void testSimpleNoChain() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("modA:a", "modB:a");
        remaps.put("modA:b", "modB:b");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertEquals("modB:a", result.get("modA:a"));
        assertEquals("modB:b", result.get("modA:b"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void testChainAtoB_BtoC() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("modA:thing", "modB:thing");
        remaps.put("modB:thing", "minecraft:thing");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertEquals("minecraft:thing", result.get("modA:thing"));
        assertEquals("minecraft:thing", result.get("modB:thing"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void testLongerChain() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("a:x", "b:x");
        remaps.put("b:x", "c:x");
        remaps.put("c:x", "d:x");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertEquals("d:x", result.get("a:x"));
        assertEquals("d:x", result.get("b:x"));
        assertEquals("d:x", result.get("c:x"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void testCycleDetected() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("a:x", "b:x");
        remaps.put("b:x", "a:x");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertTrue(result.isEmpty(), "Cycle should result in empty map");
        assertFalse(errors.isEmpty(), "Should have logged cycle error");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Circular")));
    }

    @Test
    void testSelfRemapDropped() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("a:x", "a:x");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertTrue(result.isEmpty());
        assertTrue(errors.isEmpty(), "Self-remap should be silently dropped");
    }

    @Test
    void testDiamond() {
        // A->C, B->C — no chain, both just point to C
        Map<String, String> remaps = new HashMap<>();
        remaps.put("a:x", "c:x");
        remaps.put("b:x", "c:x");

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertEquals("c:x", result.get("a:x"));
        assertEquals("c:x", result.get("b:x"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void testMixedChainAndDirect() {
        Map<String, String> remaps = new HashMap<>();
        remaps.put("a:x", "b:x");
        remaps.put("b:x", "c:x");
        remaps.put("d:y", "e:y");  // independent direct remap

        Map<String, String> result = ChainFlattener.flatten(remaps, errors::add);

        assertEquals("c:x", result.get("a:x"));
        assertEquals("c:x", result.get("b:x"));
        assertEquals("e:y", result.get("d:y"));
        assertTrue(errors.isEmpty());
    }
}
