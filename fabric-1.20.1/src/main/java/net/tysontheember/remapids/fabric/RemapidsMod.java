package net.tysontheember.remapids.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapEntry;
import net.tysontheember.remapids.core.NumericalIdResolver;
import net.tysontheember.remapids.core.RemapLoader;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.fabric.command.IdentifyCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

public class RemapidsMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemapConstants.MOD_NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("[RemapIDs] Initializing on Fabric 1.20.1");

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        NumericalIdResolver.loadCustomMappings(
                configDir.getParent().resolve(RemapConstants.CUSTOM_NUMERICAL_IDS_FILE),
                LOGGER::info);

        List<RemapEntry> entries = RemapLoader.parseFromDirectory(configDir, LOGGER::info);
        RemapState.setPending(entries, LOGGER::info);

        CommandRegistrationCallback.EVENT.register(IdentifyCommand::register);
    }
}
