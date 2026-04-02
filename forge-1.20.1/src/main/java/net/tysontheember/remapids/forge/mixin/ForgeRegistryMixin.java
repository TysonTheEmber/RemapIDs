package net.tysontheember.remapids.forge.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts {@link ForgeRegistry#getValue(ResourceLocation)} to redirect
 * lookups for remapped source IDs to their targets.
 *
 * <p>This is needed because ForgeRegistry maintains its own internal lookup maps
 * separate from vanilla's {@code MappedRegistry.byLocation}. Mods using
 * {@code ForgeRegistries.BLOCKS.getValue(rl)} go through ForgeRegistry, not vanilla.</p>
 *
 * <p>The redirect is disabled during {@code validateContent()} to avoid breaking
 * Forge's internal consistency checks.</p>
 */
@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class ForgeRegistryMixin<V> {

    @Shadow
    @Final
    private ResourceKey<?> key;

    /**
     * Flag to disable redirect during internal validation.
     * Only accessed from the main thread during startup, so no synchronization needed.
     */
    @Unique
    private static boolean remapids$inValidation = false;

    @Inject(method = "validateContent", at = @At("HEAD"))
    private void remapids$enterValidation(ResourceLocation registryName, CallbackInfo ci) {
        remapids$inValidation = true;
    }

    @Inject(method = "validateContent", at = @At("TAIL"))
    private void remapids$exitValidation(ResourceLocation registryName, CallbackInfo ci) {
        remapids$inValidation = false;
    }

    @ModifyVariable(method = "getValue", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation remapids$redirectGetValue(ResourceLocation key) {
        if (remapids$inValidation || key == null) return key;

        RemapType type = remapids$registryKeyToRemapType();
        if (type == null) return key;

        var target = RemapState.get().getTarget(type, key.toString());
        return target.map(ResourceLocation::tryParse).orElse(key);
    }

    @Unique
    private RemapType remapids$registryKeyToRemapType() {
        String path = this.key.location().getPath();
        return switch (path) {
            case "block" -> RemapType.BLOCK;
            case "item" -> RemapType.ITEM;
            case "fluid" -> RemapType.FLUID;
            case "entity_type" -> RemapType.ENTITY_TYPE;
            default -> null;
        };
    }
}
