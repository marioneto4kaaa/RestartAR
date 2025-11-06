package me.marioneto4ka.restartar.Utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Locale;

public class LangManager {

    private final JavaPlugin plugin;
    private FileConfiguration langConfig;

    public LangManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguage(plugin.getConfig().getString("language", "en"));
    }

    public void loadLanguage(String lang) {
        File langFile = new File(plugin.getDataFolder(), "lang/" + lang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String path) {
        return langConfig.getString(path, "§c[Error] Message is missing in the config!");
    }

    public String getMessage(String path, int time) {
        return getMessage(path).replace("%time%", String.valueOf(time));
    }

    public String getMessage(String path, int time, String executor) {
        return getMessage(path, time).replace("%executor%", executor);
    }
}
