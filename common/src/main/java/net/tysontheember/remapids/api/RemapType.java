package net.tysontheember.remapids.api;

import java.util.EnumSet;
import java.util.Set;

public enum RemapType {
    BLOCK("block"),
    ITEM("item"),
    FLUID("fluid"),
    ENTITY_TYPE("entity_type"),
    TAG("tag"),
    RECIPE("recipe"),
    LOOT_TABLE("loot_table");

    private final String jsonKey;

    RemapType(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    public String jsonKey() {
        return jsonKey;
    }

    /** Types that correspond to game registries (applied at freeze time). */
    public static Set<RemapType> registryTypes() {
        return EnumSet.of(BLOCK, ITEM, FLUID, ENTITY_TYPE);
    }

    /** Types that are reloadable via /reload (applied on datapack load). */
    public static Set<RemapType> reloadableTypes() {
        return EnumSet.of(TAG, RECIPE, LOOT_TABLE);
    }

    /** All types. */
    public static Set<RemapType> all() {
        return EnumSet.allOf(RemapType.class);
    }

    public static RemapType fromJsonKey(String key) {
        for (RemapType type : values()) {
            if (type.jsonKey.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
