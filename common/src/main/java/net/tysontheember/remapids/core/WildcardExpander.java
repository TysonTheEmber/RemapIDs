package net.tysontheember.remapids.core;

import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.api.RemapType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Expands wildcard patterns in remap entries against known registry IDs.
 *
 * <p>Only single {@code *} wildcards are supported. The {@code *} matches one or more
 * characters in the path portion of a namespaced ID.</p>
 */
public final class WildcardExpander {

    private WildcardExpander() {}

    /**
     * Returns true if the source pattern contains a wildcard.
     */
    public static boolean isWildcard(String id) {
        return id.contains(RemapConstants.WILDCARD);
    }

    /**
     * Expand a wildcard remap entry against a set of known IDs.
     *
     * @param source  the source pattern (e.g., "iceandfire:silver_*")
     * @param target  the target pattern (e.g., "othermod:silver_*")
     * @param types   the remap types to apply
     * @param knownIds set of all known IDs for the applicable registries
     * @return list of concrete (non-wildcard) remap entries
     */
    public static List<RemapEntry> expand(String source, String target, Set<RemapType> types, Set<String> knownIds) {
        if (!isWildcard(source)) {
            return List.of(new RemapEntry(source, target, types));
        }

        String regex = buildRegex(source);
        Pattern pattern = Pattern.compile(regex);
        List<RemapEntry> results = new ArrayList<>();

        for (String id : knownIds) {
            var matcher = pattern.matcher(id);
            if (matcher.matches()) {
                String captured = matcher.group(1);
                String concreteTarget = target.replace(RemapConstants.WILDCARD, captured);
                results.add(new RemapEntry(id, concreteTarget, types));
            }
        }

        return results;
    }

    /**
     * Convert a glob pattern like "iceandfire:silver_*" into a regex
     * where * becomes (.+) and everything else is quoted.
     */
    public static String buildRegex(String globPattern) {
        String[] parts = globPattern.split("\\*", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(Pattern.quote(parts[i]));
            if (i < parts.length - 1) {
                sb.append("(.+)");
            }
        }
        return sb.toString();
    }
}
