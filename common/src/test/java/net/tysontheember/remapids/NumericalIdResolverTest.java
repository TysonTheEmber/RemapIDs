package net.tysontheember.remapids;

import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.NumericalIdResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NumericalIdResolverTest {

    private final List<String> logs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        logs.clear();
        NumericalIdResolver.resetTable();
    }

    @Test
    void testIsNumericalId_plainNumber() {
        assertTrue(NumericalIdResolver.isNumericalId("1"));
        assertTrue(NumericalIdResolver.isNumericalId("35"));
        assertTrue(NumericalIdResolver.isNumericalId("256"));
    }

    @Test
    void testIsNumericalId_withMetadata() {
        assertTrue(NumericalIdResolver.isNumericalId("1:3"));
        assertTrue(NumericalIdResolver.isNumericalId("35:14"));
        assertTrue(NumericalIdResolver.isNumericalId("263:1"));
    }

    @Test
    void testIsNumericalId_notNumerical() {
        assertFalse(NumericalIdResolver.isNumericalId("minecraft:stone"));
        assertFalse(NumericalIdResolver.isNumericalId("modname:item"));
        assertFalse(NumericalIdResolver.isNumericalId("#forge:ores"));
        assertFalse(NumericalIdResolver.isNumericalId(""));
        assertFalse(NumericalIdResolver.isNumericalId(null));
        assertFalse(NumericalIdResolver.isNumericalId("abc"));
        assertFalse(NumericalIdResolver.isNumericalId("1:abc"));
        assertFalse(NumericalIdResolver.isNumericalId("abc:1"));
    }

    @Test
    void testResolve_stone() {
        String result = NumericalIdResolver.resolve("1", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:stone", result);
    }

    @Test
    void testResolve_stoneWithMetadata() {
        String granite = NumericalIdResolver.resolve("1:1", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:granite", granite);

        String diorite = NumericalIdResolver.resolve("1:3", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:diorite", diorite);
    }

    @Test
    void testResolve_wool() {
        String white = NumericalIdResolver.resolve("35", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:white_wool", white);

        String red = NumericalIdResolver.resolve("35:14", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:red_wool", red);
    }

    @Test
    void testResolve_itemOnly() {
        String charcoal = NumericalIdResolver.resolve("263:1", EnumSet.of(RemapType.ITEM), logs::add);
        assertEquals("minecraft:charcoal", charcoal);
    }

    @Test
    void testResolve_emptyTypes_fallsBackToBlock() {
        // Empty types = all types, should check blocks first
        String result = NumericalIdResolver.resolve("1", Set.of(), logs::add);
        assertEquals("minecraft:stone", result);
    }

    @Test
    void testResolve_unknownId_returnsNull() {
        String result = NumericalIdResolver.resolve("9999", EnumSet.of(RemapType.BLOCK), logs::add);
        assertNull(result);
        assertTrue(logs.stream().anyMatch(l -> l.contains("Unknown numerical ID")));
    }

    @Test
    void testResolve_dirt() {
        String dirt = NumericalIdResolver.resolve("3", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:dirt", dirt);

        String coarse = NumericalIdResolver.resolve("3:1", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:coarse_dirt", coarse);

        String podzol = NumericalIdResolver.resolve("3:2", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:podzol", podzol);
    }

    @Test
    void testResolve_cobblestone() {
        String result = NumericalIdResolver.resolve("4", EnumSet.of(RemapType.BLOCK), logs::add);
        assertEquals("minecraft:cobblestone", result);
    }
}
