package net.tysontheember.remapids.forge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.tysontheember.remapids.RemapConstants;
import net.tysontheember.remapids.api.RemapConfig;
import net.tysontheember.remapids.api.RemapType;
import net.tysontheember.remapids.core.RemapLoader;
import net.tysontheember.remapids.core.RemapState;
import net.tysontheember.remapids.forge.event.ForgeRegistryEvents;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@Mod(RemapConstants.MOD_ID)
public class RemapidsMod {

    private static final Logger LOGGER = LogUtils.getLogger();

    public RemapidsMod() {
        LOGGER.info("[RemapIDs] Initializing on Forge 1.20.1");

        // Load remap config from global config directory
        Path configDir = FMLPaths.CONFIGDIR.get()
                .resolve(RemapConstants.CONFIG_DIR_NAME)
                .resolve(RemapConstants.REMAPS_SUBDIR);

        // Query all known registry IDs
        Map<RemapType, Set<String>> knownIds = ForgePlatformHelper.getAllRegistryIds();

        // Load and resolve remaps
        RemapConfig config = RemapLoader.loadFromDirectory(configDir, knownIds, LOGGER::info);
        RemapState.set(config);

        if (!config.isEmpty()) {
            LOGGER.info("[RemapIDs] Active remap config: {}", config);
        }

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(ForgeRegistryEvents.class);
    }
}
