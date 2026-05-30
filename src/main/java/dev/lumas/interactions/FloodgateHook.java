package dev.lumas.interactions;

import org.bukkit.Bukkit;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

/**
 * Floodgate integration
 * @author Mitality
 */
public class FloodgateHook {

    private static boolean initialized = false;
    private static boolean enabled = false;

    public static void init() {
        if (initialized) return;
        enabled = Bukkit.getPluginManager().isPluginEnabled("floodgate");
        initialized = true;
    }

    public static boolean isBedrockPlayer(UUID uuid) {
        if (!initialized) init();
        if (!enabled) return false;
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    public static void reset() {
        initialized = false;
    }

}