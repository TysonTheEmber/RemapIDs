package net.tysontheember.remapids.core;

import net.tysontheember.remapids.RemapConstants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Flattens remap chains so every source points directly to its terminal target.
 *
 * <p>Given A→B and B→C, produces A→C and B→C. Detects and rejects cycles.</p>
 */
public final class ChainFlattener {

    private ChainFlattener() {}

    /**
     * Flatten a map of source→target remaps, resolving chains to terminal targets.
     *
     * @param rawRemaps source→target map (may contain chains)
     * @param errorHandler receives error messages for cycles or excessive chain depth
     * @return flattened source→target map with no chains
     */
    public static Map<String, String> flatten(Map<String, String> rawRemaps, Consumer<String> errorHandler) {
        Map<String, String> result = new HashMap<>();
        Set<String> cycleMembers = new HashSet<>();

        for (Map.Entry<String, String> entry : rawRemaps.entrySet()) {
            String source = entry.getKey();

            if (cycleMembers.contains(source)) {
                continue;
            }
            if (source.equals(entry.getValue())) {
                // Self-remap — silently drop
                continue;
            }

            String terminal = resolveTerminal(source, rawRemaps, cycleMembers, errorHandler);
            if (terminal != null && !terminal.equals(source)) {
                result.put(source, terminal);
            }
        }

        return result;
    }

    /**
     * Follow a chain from source to its terminal target.
     * Returns null if a cycle is detected.
     */
    private static String resolveTerminal(
            String source,
            Map<String, String> remaps,
            Set<String> cycleMembers,
            Consumer<String> errorHandler
    ) {
        Set<String> visited = new HashSet<>();
        visited.add(source);
        String current = remaps.get(source);
        int depth = 0;

        while (current != null && remaps.containsKey(current)) {
            if (visited.contains(current)) {
                // Cycle detected
                errorHandler.accept("Circular remap detected involving: " + visited);
                cycleMembers.addAll(visited);
                return null;
            }
            visited.add(current);
            current = remaps.get(current);
            depth++;

            if (depth > RemapConstants.MAX_CHAIN_DEPTH) {
                errorHandler.accept("Remap chain exceeds max depth (" + RemapConstants.MAX_CHAIN_DEPTH
                        + ") starting from '" + source + "', likely a cycle");
                cycleMembers.addAll(visited);
                return null;
            }
        }

        return current != null ? current : remaps.get(source);
    }
}
