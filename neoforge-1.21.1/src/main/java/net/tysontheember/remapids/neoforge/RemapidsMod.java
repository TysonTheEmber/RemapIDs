package net.tysontheember.remapids.neoforge;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
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

@Mod(RemapConstants.MOD_ID)
public class RemapidsMod {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemapConstants.MOD_NAME);

    public RemapidsMod() {
        LOGGER.info("[RemapIDs] Initializing on NeoForge 1.21.1");

        Path configDir = FMLPaths.CONFIGDIR.get()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        Map<RemapType, Set<String>> knownIds = NeoForgePlatformHelper.getAllRegistryIds();

        RemapConfig config = RemapLoader.loadFromDirectory(configDir, knownIds, LOGGER::info);
        RemapState.set(config);

        if (!config.isEmpty()) {
            LOGGER.info("[RemapIDs] Active remap config: {}", config);
        }
    }
}
