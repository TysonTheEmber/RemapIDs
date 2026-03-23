package net.tysontheember.remapids;

import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.WildcardExpander;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WildcardExpanderTest {

    private final Set<RemapType> itemOnly = EnumSet.of(RemapType.ITEM);

    @Test
    void testIsWildcard() {
        assertTrue(WildcardExpander.isWildcard("mod:silver_*"));
        assertFalse(WildcardExpander.isWildcard("mod:silver_ingot"));
    }

    @Test
    void testSimpleExpansion() {
        Set<String> knownIds = Set.of(
                "iceandfire:silver_ingot",
                "iceandfire:silver_nugget",
                "iceandfire:silver_block",
                "iceandfire:gold_ingot"  // should not match
        );

        List<RemapEntry> results = WildcardExpander.expand(
                "iceandfire:silver_*", "othermod:silver_*", itemOnly, knownIds);

        assertEquals(3, results.size());
        assertTrue(results.stream().anyMatch(e ->
                e.source().equals("iceandfire:silver_ingot") && e.target().equals("othermod:silver_ingot")));
        assertTrue(results.stream().anyMatch(e ->
                e.source().equals("iceandfire:silver_nugget") && e.target().equals("othermod:silver_nugget")));
        assertTrue(results.stream().anyMatch(e ->
                e.source().equals("iceandfire:silver_block") && e.target().equals("othermod:silver_block")));
    }

    @Test
    void testNoMatches() {
        Set<String> knownIds = Set.of("othermod:copper_ingot");

        List<RemapEntry> results = WildcardExpander.expand(
                "iceandfire:silver_*", "othermod:silver_*", itemOnly, knownIds);

        assertTrue(results.isEmpty());
    }

    @Test
    void testNonWildcardPassthrough() {
        Set<String> knownIds = Set.of("iceandfire:silver_ingot");

        List<RemapEntry> results = WildcardExpander.expand(
                "iceandfire:silver_ingot", "othermod:silver_ingot", itemOnly, knownIds);

        assertEquals(1, results.size());
        assertEquals("iceandfire:silver_ingot", results.get(0).source());
        assertEquals("othermod:silver_ingot", results.get(0).target());
    }

    @Test
    void testWildcardAtStart() {
        Set<String> knownIds = Set.of(
                "mod:raw_silver",
                "mod:processed_silver",
                "mod:raw_gold"
        );

        List<RemapEntry> results = WildcardExpander.expand(
                "mod:*_silver", "other:*_silver", itemOnly, knownIds);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(e ->
                e.source().equals("mod:raw_silver") && e.target().equals("other:raw_silver")));
        assertTrue(results.stream().anyMatch(e ->
                e.source().equals("mod:processed_silver") && e.target().equals("other:processed_silver")));
    }

    @Test
    void testWildcardInMiddle() {
        Set<String> knownIds = Set.of(
                "mod:silver_raw_ore",
                "mod:silver_deepslate_ore",
                "mod:copper_raw_ore"
        );

        List<RemapEntry> results = WildcardExpander.expand(
                "mod:silver_*_ore", "other:silver_*_ore", itemOnly, knownIds);

        assertEquals(2, results.size());
    }

    @Test
    void testBuildRegex() {
        // Trailing \Q\E is harmless — split("\\*", -1) produces empty trailing element
        assertEquals("\\Qmod:silver_\\E(.+)\\Q\\E", WildcardExpander.buildRegex("mod:silver_*"));
        assertEquals("\\Qmod:\\E(.+)\\Q_ore\\E", WildcardExpander.buildRegex("mod:*_ore"));
    }
}
