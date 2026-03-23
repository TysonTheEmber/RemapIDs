package net.tysontheember.remapids.neoforge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.tysontheember.remapids.api.RemapType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class NeoForgePlatformHelper {

    private NeoForgePlatformHelper() {}

    public static Map<RemapType, Set<String>> getAllRegistryIds() {
        Map<RemapType, Set<String>> result = new EnumMap<>(RemapType.class);

        result.put(RemapType.BLOCK, collectIds(BuiltInRegistries.BLOCK));
        result.put(RemapType.ITEM, collectIds(BuiltInRegistries.ITEM));
        result.put(RemapType.FLUID, collectIds(BuiltInRegistries.FLUID));
        result.put(RemapType.ENTITY_TYPE, collectIds(BuiltInRegistries.ENTITY_TYPE));

        return result;
    }

    private static Set<String> collectIds(net.minecraft.core.Registry<?> registry) {
        return registry.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toSet());
    }
}
