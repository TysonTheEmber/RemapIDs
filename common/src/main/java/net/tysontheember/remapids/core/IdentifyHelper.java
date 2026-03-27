package net.tysontheember.remapids.core;

import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;

import java.util.List;
import java.util.Optional;

/**
 * Shared formatting logic for the /remapids id command.
 * Takes a registry ID string and produces a formatted message
 * including any active remap information.
 */
public final class IdentifyHelper {

    private IdentifyHelper() {}

    public static String formatBlockInfo(String registryId, RemapConfig config) {
        return formatInfo("Block", registryId, RemapType.BLOCK, config);
    }

    public static String formatItemInfo(String registryId, RemapConfig config) {
        return formatInfo("Item", registryId, RemapType.ITEM, config);
    }

    private static String formatInfo(String label, String registryId, RemapType type, RemapConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(": ").append(registryId);

        // Check if this ID is a source that remaps to something else
        Optional<String> target = config.getTarget(type, registryId);
        if (target.isPresent()) {
            sb.append(" (Remaps to: ").append(target.get()).append(")");
        }

        // Check if this ID is the target of other remaps
        List<String> sources = config.getSourcesForTarget(type, registryId);
        if (!sources.isEmpty()) {
            sb.append(" (Remapped from: ").append(String.join(", ", sources)).append(")");
        }

        return sb.toString();
    }
}
