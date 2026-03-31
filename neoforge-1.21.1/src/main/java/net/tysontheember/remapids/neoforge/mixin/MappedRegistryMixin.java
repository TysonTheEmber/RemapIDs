package net.tysontheember.remapids.neoforge.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.neoforge.NeoForgePlatformHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    private static final Logger REMAPIDS_LOGGER = LoggerFactory.getLogger("RemapIDs");

    @Shadow
    @Final
    private Map<ResourceLocation, Holder.Reference<T>> byLocation;

    @Shadow
    @Final
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;

    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @Inject(method = "freeze", at = @At("HEAD"))
    private void remapids$beforeFreeze(CallbackInfoReturnable<Registry<T>> cir) {
        if (RemapState.hasPending()) {
            RemapState.finalizeIfPending(NeoForgePlatformHelper.getAllRegistryIds());
        }

        RemapType type = registryKeyToRemapType(this.key());
        if (type == null) return;

        Map<String, String> remaps = RemapState.get().getAllForType(type);
        if (remaps.isEmpty()) return;

        int injected = 0;
        for (Map.Entry<String, String> entry : remaps.entrySet()) {
            ResourceLocation sourceRL = ResourceLocation.tryParse(entry.getKey());
            ResourceLocation targetRL = ResourceLocation.tryParse(entry.getValue());

            Holder.Reference<T> targetHolder = this.byLocation.get(targetRL);
            if (targetHolder != null) {
                this.byLocation.put(sourceRL, targetHolder);
                ResourceKey<T> sourceKey = ResourceKey.create(this.key(), sourceRL);
                this.byKey.put(sourceKey, targetHolder);
                injected++;
            } else {
                REMAPIDS_LOGGER.warn("[RemapIDs] Cannot inject alias {} -> {}: target not found in {} registry",
                        entry.getKey(), entry.getValue(), type.jsonKey());
            }
        }

        if (injected > 0) {
            REMAPIDS_LOGGER.info("[RemapIDs] Injected {} {} registry aliases", injected, type.jsonKey());
        }
    }

    private static RemapType registryKeyToRemapType(ResourceKey<? extends Registry<?>> key) {
        String path = key.location().getPath();
        return switch (path) {
            case "block" -> RemapType.BLOCK;
            case "item" -> RemapType.ITEM;
            case "fluid" -> RemapType.FLUID;
            case "entity_type" -> RemapType.ENTITY_TYPE;
            default -> null;
        };
    }
}
