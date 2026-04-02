package net.tysontheember.remapids.neoforge.mixin;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.neoforge.NeoForgePlatformHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    private static final Logger REMAPIDS_LOGGER = LoggerFactory.getLogger("RemapIDs");

    @Shadow @Final private Map<ResourceLocation, Holder.Reference<T>> byLocation;
    @Shadow @Final private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow public abstract ResourceKey<? extends Registry<T>> key();

    @Inject(method = "freeze", at = @At("HEAD"))
    private void remapids$beforeFreeze(CallbackInfoReturnable<Registry<T>> cir) {
        if (!RemapState.hasPending()) return;
        RemapState.finalizeIfPending(NeoForgePlatformHelper.getAllRegistryIds());
        remapids$injectAllRegistryAliases();
    }

    @Unique
    private static void remapids$injectAllRegistryAliases() {
        remapids$injectAliasesInto(BuiltInRegistries.BLOCK, RemapType.BLOCK);
        remapids$injectAliasesInto(BuiltInRegistries.ITEM, RemapType.ITEM);
        remapids$injectAliasesInto(BuiltInRegistries.FLUID, RemapType.FLUID);
        remapids$injectAliasesInto(BuiltInRegistries.ENTITY_TYPE, RemapType.ENTITY_TYPE);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <R> void remapids$injectAliasesInto(Registry<R> registry, RemapType type) {
        Map<String, String> remaps = RemapState.get().getAllForType(type);
        if (remaps.isEmpty()) return;

        MappedRegistryMixin<R> mixin = (MappedRegistryMixin<R>)(Object) registry;
        ResourceKey<? extends Registry<R>> registryKey = registry.key();
        List<Map.Entry<Block, Block>> blockRemaps = type == RemapType.BLOCK ? new ArrayList<>() : null;

        int injected = 0;
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            ResourceLocation sourceRL = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(entry.getValue());
            if (sourceRL == null || targetRL == null) continue;

            Holder.Reference<R> targetHolder = mixin.byLocation.get(targetRL);
            if (targetHolder == null) {
                REMAPIDS_LOGGER.warn("[RemapIDs] Cannot inject alias {} -> {}: target not found in {} registry",
                        entry.getKey(), entry.getValue(), type.jsonKey());
                continue;
            }

            if (blockRemaps != null) {
                Holder.Reference<R> sourceHolder = mixin.byLocation.get(sourceRL);
                if (sourceHolder != null) {
                    blockRemaps.add(new AbstractMap.SimpleEntry<>(
                            (Block)(Object) sourceHolder.value(), (Block)(Object) targetHolder.value()));
                }
            }

            mixin.byLocation.put(sourceRL, targetHolder);
            ResourceKey<R> sourceKey = ResourceKey.create(registryKey, sourceRL);
            mixin.byKey.put(sourceKey, targetHolder);
            injected++;
        }

        if (blockRemaps != null && !blockRemaps.isEmpty()) {
            remapids$remapBlockStateIds(blockRemaps);
        }
        if (injected > 0) {
            REMAPIDS_LOGGER.info("[RemapIDs] Injected {} {} registry aliases", injected, type.jsonKey());
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static void remapids$remapBlockStateIds(List<Map.Entry<Block, Block>> blockRemaps) {
        Reference2IntMap<BlockState> stateIds =
                ((IdMapperAccessor<BlockState>)(Object) Block.BLOCK_STATE_REGISTRY).remapids$getTToId();
        int remapped = 0;
        for (Map.Entry<Block, Block> pair : blockRemaps) {
            for (BlockState sourceState : pair.getKey().getStateDefinition().getPossibleStates()) {
                BlockState targetState = remapids$findMatchingState(sourceState, pair.getValue());
                int targetStateId = Block.BLOCK_STATE_REGISTRY.getId(targetState);
                if (targetStateId >= 0) { stateIds.put(sourceState, targetStateId); remapped++; }
            }
        }
        if (remapped > 0) {
            REMAPIDS_LOGGER.info("[RemapIDs] Remapped {} block state IDs for {} block aliases", remapped, blockRemaps.size());
        }
    }

    @Unique
    private static BlockState remapids$findMatchingState(BlockState sourceState, Block targetBlock) {
        BlockState targetState = targetBlock.defaultBlockState();
        StateDefinition<Block, BlockState> targetDef = targetBlock.getStateDefinition();
        for (Property<?> sourceProp : sourceState.getProperties()) {
            Property<?> targetProp = targetDef.getProperty(sourceProp.getName());
            if (targetProp != null) { targetState = remapids$copyProperty(targetState, sourceState, sourceProp, targetProp); }
        }
        return targetState;
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <V extends Comparable<V>> BlockState remapids$copyProperty(
            BlockState target, BlockState source, Property<?> sourceProp, Property<?> targetProp) {
        try {
            Property<V> typed = (Property<V>) sourceProp;
            Property<V> typedTarget = (Property<V>) targetProp;
            V value = source.getValue(typed);
            if (typedTarget.getPossibleValues().contains(value)) { return target.setValue(typedTarget, value); }
        } catch (ClassCastException ignored) {}
        return target;
    }
}
