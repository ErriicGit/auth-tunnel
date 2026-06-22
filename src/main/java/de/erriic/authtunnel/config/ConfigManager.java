package de.erriic.authtunnel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("auth-tunnel.json");
    private static final Logger LOGGER = LogUtils.getLogger();

    private static AuthTunnelConfig CONFIG;

    public static AuthTunnelConfig get() {
        if (CONFIG == null) {
            load();
        }
        return CONFIG;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            CONFIG = new AuthTunnelConfig();
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            CONFIG = GSON.fromJson(reader, AuthTunnelConfig.class);

            if (CONFIG == null) {
                CONFIG = new AuthTunnelConfig();
            }
        } catch (IOException e) {
            CONFIG = new AuthTunnelConfig();
            LOGGER.error("[AuthTunnel] Failed to load config", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(CONFIG, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[AuthTunnel] Failed to save config", e);
        }
    }
}
