package net.tysontheember.remapids.forge.mixin;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * On Forge 1.20.1, registry alias injection is NOT done here. Bootstrap freeze()
 * runs on the Render thread concurrently with mod loading on worker threads,
 * causing a race condition where modded content isn't available yet.
 *
 * <p>Instead, finalization and alias injection are triggered from
 * {@link net.tysontheember.remapids.forge.RemapidsMod} via {@code FMLLoadCompleteEvent},
 * which is guaranteed to fire after all {@code RegisterEvent} handlers.</p>
 *
 * <p>This mixin is kept as a no-op placeholder so the mixin config is valid.</p>
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> {

    @Inject(method = "freeze", at = @At("HEAD"))
    private void remapids$beforeFreeze(CallbackInfoReturnable<Registry<T>> cir) {
        // Intentionally empty on Forge — see class javadoc
    }
}
