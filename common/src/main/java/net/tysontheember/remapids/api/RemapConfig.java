package net.tysontheember.remapids.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable, fully resolved remap configuration. All wildcards have been expanded,
 * chains have been flattened, and conflicts resolved.
 *
 * <p>Thread-safe: once built, this object is read-only.</p>
 */
public final class RemapConfig {

    public static final RemapConfig EMPTY = new RemapConfig(new EnumMap<>(RemapType.class));

    private final Map<RemapType, Map<String, String>> remapsByType;

    public RemapConfig(Map<RemapType, Map<String, String>> remapsByType) {
        // Deep defensive copy
        EnumMap<RemapType, Map<String, String>> copy = new EnumMap<>(RemapType.class);
        for (Map.Entry<RemapType, Map<String, String>> entry : remapsByType.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.remapsByType = Collections.unmodifiableMap(copy);
    }

    /**
     * Get the remap target for a source ID within a given type.
     */
    public Optional<String> getTarget(RemapType type, String sourceId) {
        Map<String, String> map = remapsByType.get(type);
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(sourceId));
    }

    /**
     * Get all source->target mappings for a given type.
     */
    public Map<String, String> getAllForType(RemapType type) {
        Map<String, String> map = remapsByType.get(type);
        return map != null ? map : Collections.emptyMap();
    }

    /**
     * Check if a remap exists for the given type and source ID.
     */
    public boolean hasRemapFor(RemapType type, String sourceId) {
        Map<String, String> map = remapsByType.get(type);
        return map != null && map.containsKey(sourceId);
    }

    /**
     * Reverse lookup: find which source ID(s) remap to the given target.
     */
    public List<String> getSourcesForTarget(RemapType type, String targetId) {
        Map<String, String> map = remapsByType.get(type);
        if (map == null) return List.of();
        List<String> sources = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getValue().equals(targetId)) {
                sources.add(entry.getKey());
            }
        }
        return sources;
    }

    /**
     * Total number of remaps across all types.
     */
    public int size() {
        int count = 0;
        for (Map<String, String> map : remapsByType.values()) {
            count += map.size();
        }
        return count;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public String toString() {
        return "RemapConfig{size=" + size() + ", types=" + remapsByType.keySet() + "}";
    }
}
