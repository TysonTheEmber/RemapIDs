package net.tysontheember.remapids.api;

import java.util.Objects;
import java.util.Set;

/**
 * A single remap definition: source identifier -> target identifier,
 * optionally filtered to specific types.
 *
 * <p>Source and target are full namespaced identifiers as strings
 * (e.g., "iceandfire:silver_ingot"). Tag remaps use "#" prefix
 * (e.g., "#forge:ores/silver").</p>
 *
 * <p>After wildcard expansion, entries contain no wildcards.</p>
 */
public final class RemapEntry {
    private final String source;
    private final String target;
    private final Set<RemapType> types;

    public RemapEntry(String source, String target, Set<RemapType> types) {
        this.source = Objects.requireNonNull(source);
        this.target = Objects.requireNonNull(target);
        this.types = Objects.requireNonNull(types);
    }

    public String source() {
        return source;
    }

    public String target() {
        return target;
    }

    public Set<RemapType> types() {
        return types;
    }

    public boolean appliesTo(RemapType type) {
        return types.isEmpty() || types.contains(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemapEntry that)) return false;
        return source.equals(that.source) && target.equals(that.target) && types.equals(that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target, types);
    }

    @Override
    public String toString() {
        return source + " -> " + target + " " + types;
    }
}
