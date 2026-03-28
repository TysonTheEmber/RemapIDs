package net.tysontheember.remapids.fabric21;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.core.RemapLoader;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.fabric21.command.IdentifyCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class RemapidsMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemapConstants.MOD_NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("[RemapIDs] Initializing on Fabric 1.21.1");

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        RemapConfig config = RemapLoader.loadFromDirectory(configDir, LOGGER::info);
        RemapState.set(config);

        if (!config.isEmpty()) {
            LOGGER.info("[RemapIDs] Active remap config: {}", config);
        }

        CommandRegistrationCallback.EVENT.register(IdentifyCommand::register);
    }
}
