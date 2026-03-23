package net.tysontheember.remapids.fabric21.mixin;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.tysontheember.remapids.core.JsonRemapper;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Rewrites item/block references in loot table JSON before codec deserialization.
 * This ensures loot drops respect item remaps (e.g., coal -> diamond).
 */
@Mixin(LootDataType.class)
public class LootDataTypeMixin<T> {

    @Inject(method = "deserialize", at = @At("HEAD"))
    private void remapids$rewriteLootJson(ResourceLocation id, DynamicOps<?> ops, Object data, CallbackInfoReturnable<Optional<T>> cir) {
        if (data instanceof JsonElement json) {
            JsonRemapper.remapJson(json, RemapState.get());
        }
    }
}
