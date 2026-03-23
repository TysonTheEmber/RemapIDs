package net.tysontheember.remapids.core;

import net.tysontheember.remapids.api.RemapType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Validates that remap targets exist in the game registries.
 */
public final class RemapValidator {

    private RemapValidator() {}

    /**
     * Validate that all targets in the remap map exist in the known ID set.
     * Returns a list of warnings for missing targets.
     *
     * @param remaps      source→target map for a single type
     * @param knownIds    set of all known IDs in the registry for this type
     * @param type        the remap type being validated
     * @param warnHandler receives warning messages for missing targets
     * @return the input map with invalid entries removed
     */
    public static Map<String, String> validateAndFilter(
            Map<String, String> remaps,
            Set<String> knownIds,
            RemapType type,
            Consumer<String> warnHandler
    ) {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            if (!knownIds.contains(entry.getValue())) {
                warnHandler.accept("[RemapIDs] Remap target '" + entry.getValue()
                        + "' not found in " + type.jsonKey() + " registry, skipping remap from '"
                        + entry.getKey() + "'");
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            remaps.remove(key);
        }

        return remaps;
    }
}
