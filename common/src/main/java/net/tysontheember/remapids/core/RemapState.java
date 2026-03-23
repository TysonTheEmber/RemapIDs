package net.tysontheember.remapids.core;

import net.tysontheember.remapids.api.RemapConfig;

/**
 * Holds the currently active {@link RemapConfig}. Set during mod initialization,
 * read by mixins at runtime. Thread-safe via volatile reference to immutable config.
 */
public final class RemapState {
    private static volatile RemapConfig active = RemapConfig.EMPTY;

    public static RemapConfig get() {
        return active;
    }

    public static void set(RemapConfig config) {
        active = config;
    }

    private RemapState() {}
}
