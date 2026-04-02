package net.tysontheember.remapids.forge;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.forge.mixin.IdMapperAccessor;
import net.tysontheember.remapids.forge.mixin.MappedRegistryAccessor;
import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Injects registry aliases after all mod loading is complete.
 *
 * <p>On Forge 1.20.1, modded entries live in {@link ForgeRegistries} (ForgeRegistry),
 * NOT in vanilla {@link BuiltInRegistries} (MappedRegistry). Target lookups must go
 * through ForgeRegistries. Aliases are injected into the vanilla MappedRegistry's
 * internal maps so that vanilla code paths (codecs, etc.) also resolve them.</p>
 */
public final class RegistryAliasInjector {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RegistryAliasInjector() {}

    public static void injectAll() {
        if (RemapState.get().isEmpty()) return;

        injectBlockAliases();
        injectItemAliases();
        injectSimpleAliases(BuiltInRegistries.FLUID, ForgeRegistries.FLUIDS, RemapType.FLUID);
        injectSimpleAliases(BuiltInRegistries.ENTITY_TYPE, ForgeRegistries.ENTITY_TYPES, RemapType.ENTITY_TYPE);
    }

    @SuppressWarnings("unchecked")
    private static void injectBlockAliases() {
        Map<String, String> remaps = RemapState.get().getAllForType(RemapType.BLOCK);
        if (remaps.isEmpty()) return;

        IForgeRegistry<Block> forgeRegistry = ForgeRegistries.BLOCKS;
        MappedRegistryAccessor<Block> vanilla =
                (MappedRegistryAccessor<Block>)(Object) BuiltInRegistries.BLOCK;
        Map<ResourceLocation, Holder.Reference<Block>> byLocation = vanilla.remapids$getByLocation();
        Map<ResourceKey<Block>, Holder.Reference<Block>> byKey = vanilla.remapids$getByKey();

        List<Map.Entry<Block, Block>> blockStateRemaps = new ArrayList<>();

        int injected = 0;
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            ResourceLocation sourceRL = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(entry.getValue());
            if (sourceRL == null || targetRL == null) continue;

            // Look up target via ForgeRegistries (canonical on Forge)
            Block targetBlock = forgeRegistry.getValue(targetRL);
            if (targetBlock == null || targetRL.equals(new ResourceLocation("minecraft:air"))) {
                LOGGER.warn("[RemapIDs] Cannot inject block alias {} -> {}: target not found",
                        entry.getKey(), entry.getValue());
                continue;
            }

            // Save source block for block state remapping (if source exists)
            Block sourceBlock = forgeRegistry.getValue(sourceRL);
            if (sourceBlock != null && !sourceRL.equals(new ResourceLocation("minecraft:air"))) {
                blockStateRemaps.add(new AbstractMap.SimpleEntry<>(sourceBlock, targetBlock));
            }

            // Inject into vanilla MappedRegistry for codec/vanilla code paths
            Holder.Reference<Block> targetHolder = byLocation.get(targetRL);
            if (targetHolder != null) {
                byLocation.put(sourceRL, targetHolder);
                ResourceKey<Block> sourceKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), sourceRL);
                byKey.put(sourceKey, targetHolder);
                injected++;
            } else {
                LOGGER.debug("[RemapIDs] Block target {} not in vanilla MappedRegistry" +
                        " (ForgeRegistry redirect will handle lookups)", entry.getValue());
                injected++; // Still counts — ForgeRegistryMixin handles lookups
            }
        }

        if (!blockStateRemaps.isEmpty()) {
            remapBlockStateIds(blockStateRemaps);
        }

        if (injected > 0) {
            LOGGER.info("[RemapIDs] Injected {} block registry aliases", injected);
        }
    }

    @SuppressWarnings("unchecked")
    private static void injectItemAliases() {
        Map<String, String> remaps = RemapState.get().getAllForType(RemapType.ITEM);
        if (remaps.isEmpty()) return;

        MappedRegistryAccessor<net.minecraft.world.item.Item> vanilla =
                (MappedRegistryAccessor<net.minecraft.world.item.Item>)(Object) BuiltInRegistries.ITEM;
        Map<ResourceLocation, Holder.Reference<net.minecraft.world.item.Item>> byLocation = vanilla.remapids$getByLocation();
        Map<ResourceKey<net.minecraft.world.item.Item>, Holder.Reference<net.minecraft.world.item.Item>> byKey = vanilla.remapids$getByKey();

        int injected = 0;
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            ResourceLocation sourceRL = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(entry.getValue());
            if (sourceRL == null || targetRL == null) continue;

            // Verify target exists in ForgeRegistries
            if (!ForgeRegistries.ITEMS.containsKey(targetRL)) {
                LOGGER.warn("[RemapIDs] Cannot inject item alias {} -> {}: target not found",
                        entry.getKey(), entry.getValue());
                continue;
            }

            // Inject into vanilla MappedRegistry if holder is available
            Holder.Reference<net.minecraft.world.item.Item> targetHolder = byLocation.get(targetRL);
            if (targetHolder != null) {
                byLocation.put(sourceRL, targetHolder);
                ResourceKey<net.minecraft.world.item.Item> sourceKey =
                        ResourceKey.create(BuiltInRegistries.ITEM.key(), sourceRL);
                byKey.put(sourceKey, targetHolder);
            }
            injected++;
        }

        if (injected > 0) {
            LOGGER.info("[RemapIDs] Injected {} item registry aliases", injected);
        }
    }

    @SuppressWarnings("unchecked")
    private static <R> void injectSimpleAliases(
            Registry<R> registry, IForgeRegistry<R> forgeRegistry, RemapType type) {
        Map<String, String> remaps = RemapState.get().getAllForType(type);
        if (remaps.isEmpty()) return;

        MappedRegistryAccessor<R> vanilla = (MappedRegistryAccessor<R>)(Object) registry;
        Map<ResourceLocation, Holder.Reference<R>> byLocation = vanilla.remapids$getByLocation();
        Map<ResourceKey<R>, Holder.Reference<R>> byKey = vanilla.remapids$getByKey();

        int injected = 0;
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            ResourceLocation sourceRL = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(entry.getValue());
            if (sourceRL == null || targetRL == null) continue;

            if (!forgeRegistry.containsKey(targetRL)) {
                LOGGER.warn("[RemapIDs] Cannot inject {} alias {} -> {}: target not found",
                        type.jsonKey(), entry.getKey(), entry.getValue());
                continue;
            }

            Holder.Reference<R> targetHolder = byLocation.get(targetRL);
            if (targetHolder != null) {
                byLocation.put(sourceRL, targetHolder);
                ResourceKey<R> sourceKey = ResourceKey.create(registry.key(), sourceRL);
                byKey.put(sourceKey, targetHolder);
            }
            injected++;
        }

        if (injected > 0) {
            LOGGER.info("[RemapIDs] Injected {} {} registry aliases", injected, type.jsonKey());
        }
    }

    @SuppressWarnings("unchecked")
    private static void remapBlockStateIds(List<Map.Entry<Block, Block>> blockRemaps) {
        Object2IntMap<BlockState> stateIds =
                ((IdMapperAccessor<BlockState>)(Object) Block.BLOCK_STATE_REGISTRY).remapids$getTToId();

        int remapped = 0;
        for (Map.Entry<Block, Block> pair : blockRemaps) {
            Block sourceBlock = pair.getKey();
            Block targetBlock = pair.getValue();

            for (BlockState sourceState : sourceBlock.getStateDefinition().getPossibleStates()) {
                BlockState targetState = findMatchingState(sourceState, targetBlock);
                int targetStateId = Block.BLOCK_STATE_REGISTRY.getId(targetState);
                if (targetStateId >= 0) {
                    stateIds.put(sourceState, targetStateId);
                    remapped++;
                }
            }
        }

        if (remapped > 0) {
            LOGGER.info("[RemapIDs] Remapped {} block state IDs for {} block aliases",
                    remapped, blockRemaps.size());
        }
    }

    private static BlockState findMatchingState(BlockState sourceState, Block targetBlock) {
        BlockState targetState = targetBlock.defaultBlockState();
        StateDefinition<Block, BlockState> targetDef = targetBlock.getStateDefinition();

        for (Property<?> sourceProp : sourceState.getProperties()) {
            Property<?> targetProp = targetDef.getProperty(sourceProp.getName());
            if (targetProp != null) {
                targetState = copyProperty(targetState, sourceState, sourceProp, targetProp);
            }
        }

        return targetState;
    }

    @SuppressWarnings("unchecked")
    private static <V extends Comparable<V>> BlockState copyProperty(
            BlockState target, BlockState source, Property<?> sourceProp, Property<?> targetProp) {
        try {
            Property<V> typed = (Property<V>) sourceProp;
            Property<V> typedTarget = (Property<V>) targetProp;
            V value = source.getValue(typed);
            if (typedTarget.getPossibleValues().contains(value)) {
                return target.setValue(typedTarget, value);
            }
        } catch (ClassCastException ignored) {}
        return target;
    }
}
