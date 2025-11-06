package me.marioneto4ka.restartar.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ConfigUpdater {

    private final JavaPlugin plugin;

    public ConfigUpdater(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateConfig() {
        FileConfiguration config = plugin.getConfig();
        File defaultConfigFile = new File(plugin.getDataFolder(), "config.yml");
        if (!defaultConfigFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        FileConfiguration defaultConfig = plugin.getConfig();
        plugin.saveDefaultConfig();

        addMissingKeys(defaultConfig, config);

        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save updated config.yml: " + e.getMessage());
        }
    }

    private void addMissingKeys(FileConfiguration source, FileConfiguration target) {
        Set<String> keys = source.getKeys(true);
        for (String key : keys) {
            if (!target.contains(key)) {
                target.set(key, source.get(key));
                plugin.getLogger().info("Added missing config key: " + key);
            }
        }
    }
}
