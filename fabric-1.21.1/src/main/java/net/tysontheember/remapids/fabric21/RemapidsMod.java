package net.tysontheember.remapids.fabric21;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapLoader;
import net.tysontheember.remapids.core.RemapState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class RemapidsMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemapConstants.MOD_NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("[RemapIDs] Initializing on Fabric 1.21.1");

        Path configDir = FabricLoader.getInstance().getConfigDir()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        Map<RemapType, Set<String>> knownIds = FabricPlatformHelper.getAllRegistryIds();

        RemapConfig config = RemapLoader.loadFromDirectory(configDir, knownIds, LOGGER::info);
        RemapState.set(config);

        if (!config.isEmpty()) {
            LOGGER.info("[RemapIDs] Active remap config: {}", config);
        }
    }
}
