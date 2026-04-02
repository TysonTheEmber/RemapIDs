package net.tysontheember.remapids.forge.event;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles world migration: when a world is loaded containing registry IDs that
 * no longer exist (e.g., the source mod was removed), this remaps them to the
 * configured targets.
 */
public class ForgeRegistryEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        handleMappings(event, ForgeRegistries.BLOCKS.getRegistryKey(), RemapType.BLOCK);
        handleMappings(event, ForgeRegistries.ITEMS.getRegistryKey(), RemapType.ITEM);
        handleMappings(event, ForgeRegistries.FLUIDS.getRegistryKey(), RemapType.FLUID);
        handleMappings(event, ForgeRegistries.ENTITY_TYPES.getRegistryKey(), RemapType.ENTITY_TYPE);
    }

    @SuppressWarnings("unchecked")
    private static <T> void handleMappings(
            MissingMappingsEvent event,
            net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey,
            RemapType type
    ) {
        Map<String, String> remaps = RemapState.get().getAllForType(type);
        if (remaps.isEmpty()) return;

        // Collect all unique source namespaces from the remap config
        Set<String> sourceNamespaces = new HashSet<>();
        for (String sourceId : remaps.keySet()) {
            int colon = sourceId.indexOf(':');
            if (colon > 0) {
                sourceNamespaces.add(sourceId.substring(0, colon));
            }
        }

        for (String namespace : sourceNamespaces) {
            for (MissingMappingsEvent.Mapping<T> mapping : event.getMappings(registryKey, namespace)) {
                String sourceId = mapping.getKey().toString();
                String targetId = remaps.get(sourceId);

                if (targetId != null) {
                    ResourceLocation targetRL = ResourceLocation.tryParse(targetId);
                    T targetValue = (T) lookupInRegistry(type, targetRL);

                    if (targetValue != null) {
                        mapping.remap(targetValue);
                        LOGGER.info("[RemapIDs] Remapped missing {} '{}' -> '{}'",
                                type.jsonKey(), sourceId, targetId);
                    } else {
                        LOGGER.warn("[RemapIDs] Cannot remap missing {} '{}': target '{}' not found",
                                type.jsonKey(), sourceId, targetId);
                    }
                }
            }
        }
    }

    private static Object lookupInRegistry(RemapType type, ResourceLocation rl) {
        return switch (type) {
            case BLOCK -> ForgeRegistries.BLOCKS.getValue(rl);
            case ITEM -> ForgeRegistries.ITEMS.getValue(rl);
            case FLUID -> ForgeRegistries.FLUIDS.getValue(rl);
            case ENTITY_TYPE -> ForgeRegistries.ENTITY_TYPES.getValue(rl);
            default -> null;
        };
    }
}
