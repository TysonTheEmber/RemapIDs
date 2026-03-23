package net.tysontheember.remapids.fabric.mixin;

import com.google.gson.JsonElement;
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
 * Rewrites item/block references in loot table JSON before Gson deserialization.
 * This ensures loot drops respect item remaps (e.g., coal -> diamond).
 */
@Mixin(LootDataType.class)
public class LootDataTypeMixin<T> {

    @Inject(method = "deserialize(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonElement;)Ljava/util/Optional;", at = @At("HEAD"))
    private void remapids$rewriteLootJson(ResourceLocation id, JsonElement json, CallbackInfoReturnable<Optional<T>> cir) {
        JsonRemapper.remapJson(json, RemapState.get());
    }
}
