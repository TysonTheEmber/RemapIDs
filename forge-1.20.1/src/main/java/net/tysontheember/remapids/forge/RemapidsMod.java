package net.tysontheember.remapids.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.core.NumericalIdResolver;
import net.tysontheember.remapids.core.RemapLoader;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.forge.command.IdentifyCommand;
import net.tysontheember.remapids.forge.event.ForgeRegistryEvents;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

@Mod(RemapConstants.MOD_ID)
public class RemapidsMod {

    private static final Logger LOGGER = LogUtils.getLogger();

    public RemapidsMod() {
        LOGGER.info("[RemapIDs] Initializing on Forge 1.20.1");

        // Load remap config from global config directory
        Path configDir = FMLPaths.CONFIGDIR.get()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        // Load custom numerical ID mappings (modded IDs from pre-1.13)
        NumericalIdResolver.loadCustomMappings(
                configDir.getParent().resolve(RemapConstants.CUSTOM_NUMERICAL_IDS_FILE),
                LOGGER::info);

        List<RemapEntry> entries = RemapLoader.parseFromDirectory(configDir, LOGGER::info);
        RemapState.setPending(entries, LOGGER::info);

        // Finalize remap config and inject registry aliases after all mod loading
        // is complete. FMLLoadCompleteEvent is the last lifecycle event — by this
        // point all RegisterEvent handlers have run, GameData.freezeData() has
        // synced ForgeRegistry entries to vanilla MappedRegistry, and all registries
        // are fully populated. We can't use MappedRegistry.freeze() on Forge because
        // bootstrap freeze() runs on the Render thread concurrently with mod loading.
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLLoadCompleteEvent event) ->
                event.enqueueWork(() -> {
                    RemapState.finalizeIfPending(ForgePlatformHelper.getAllRegistryIds());
                    RegistryAliasInjector.injectAll();
                }));

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(ForgeRegistryEvents.class);
        MinecraftForge.EVENT_BUS.addListener(IdentifyCommand::register);
    }
}
