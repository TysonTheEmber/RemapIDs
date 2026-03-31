package net.tysontheember.remapids.core;

import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.api.RemapType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Holds the currently active {@link RemapConfig}. Supports two-phase initialization:
 * raw entries are stored as pending during mod construction, then resolved against
 * complete registry data at freeze time.
 *
 * <p>Thread-safe via volatile references and synchronized finalization.</p>
 */
public final class RemapState {
    private static volatile RemapConfig active = RemapConfig.EMPTY;
    private static volatile List<RemapEntry> pendingEntries;
    private static volatile Consumer<String> pendingLogger;

    public static RemapConfig get() {
        return active;
    }

    public static void set(RemapConfig config) {
        active = config;
    }

    /**
     * Store raw parsed entries for deferred resolution. Call this during mod
     * construction; the entries will be resolved when {@link #finalizeIfPending}
     * is called at registry freeze time.
     */
    public static void setPending(List<RemapEntry> entries, Consumer<String> logger) {
        pendingEntries = entries;
        pendingLogger = logger;
    }

    /**
     * Returns true if there are pending entries awaiting resolution.
     */
    public static boolean hasPending() {
        return pendingEntries != null;
    }

    /**
     * Resolve pending entries against the given registry IDs and set the active config.
     * Safe to call multiple times — only the first call performs resolution.
     *
     * @param knownIds map of RemapType → set of known IDs (should include all mod entries)
     */
    public static synchronized void finalizeIfPending(Map<RemapType, Set<String>> knownIds) {
        List<RemapEntry> entries = pendingEntries;
        Consumer<String> logger = pendingLogger;
        if (entries == null) return;

        RemapConfig config = RemapLoader.resolve(entries, knownIds, logger);
        active = config;
        pendingEntries = null;
        pendingLogger = null;

        if (!config.isEmpty()) {
            logger.accept("[RemapIDs] Active remap config: " + config);
        }
    }

    private RemapState() {}
}
