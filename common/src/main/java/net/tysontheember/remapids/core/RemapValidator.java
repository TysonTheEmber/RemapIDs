package net.tysontheember.remapids.core;

import net.tysontheember.remapids.api.RemapType;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Validates that remap targets exist in the game registries.
 */
public final class RemapValidator {

    private RemapValidator() {}

    /**
     * Log warnings for remap targets that are not currently in the known ID set.
     * Does <b>not</b> remove entries — targets may be modded IDs that haven't
     * been registered yet at the time of validation. The actual registry check
     * happens later in the mixin when the registry is frozen.
     *
     * @param remaps      source→target map for a single type
     * @param knownIds    set of all known IDs in the registry for this type
     * @param type        the remap type being validated
     * @param warnHandler receives warning messages for missing targets
     */
    public static void warnUnresolvableTargets(
            Map<String, String> remaps,
            Set<String> knownIds,
            RemapType type,
            Consumer<String> warnHandler
    ) {
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            if (!knownIds.contains(entry.getValue())) {
                warnHandler.accept("[RemapIDs] Remap target '" + entry.getValue()
                        + "' not found in " + type.jsonKey() + " registry yet (may be a modded ID"
                        + " not registered at this point), remap from '" + entry.getKey()
                        + "' will be attempted at freeze time");
            }
        }
    }
}
