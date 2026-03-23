package net.tysontheember.remapids.forge.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Redirects loot table lookups so that source loot table IDs resolve to target tables.
 *
 * <p>Intercepts {@code getElement(LootDataId)} and, if the ID is a loot table
 * with a known remap, substitutes the target's loot table.</p>
 */
@Mixin(LootDataManager.class)
public class LootDataManagerMixin {

    @Shadow
    private Map<LootDataId<?>, ?> elements;

    @Inject(method = "getElement", at = @At("HEAD"), cancellable = true)
    private <T> void remapids$redirectLootTableLookup(LootDataId<T> id, CallbackInfoReturnable<T> cir) {
        if (id.type() != LootDataType.TABLE) return;

        String target = RemapState.get().getTarget(RemapType.LOOT_TABLE, id.location().toString()).orElse(null);
        if (target == null) return;

        ResourceLocation targetRL = ResourceLocation.tryParse(target);
        if (targetRL == null) return;

        @SuppressWarnings("unchecked")
        T result = (T) this.elements.get(new LootDataId<>(id.type(), targetRL));
        if (result != null) {
            cir.setReturnValue(result);
        }
    }
}
