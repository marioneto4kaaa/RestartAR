package me.marioneto4ka.restartar.Utils;

import me.marioneto4ka.restartar.RestartAR;
import me.marioneto4ka.restartar.Discord.DiscordNotifier;
import me.marioneto4ka.restartar.Function.ScheduledRestartHandler;

import java.time.ZoneId;
import java.util.List;

public class ScheduledRestartUtils {

    private final RestartAR plugin;
    private final DiscordNotifier discordNotifier;

    public ScheduledRestartUtils(RestartAR plugin, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.discordNotifier = discordNotifier;
    }

    public void handleScheduledRestarts(List<String> restartDates) {
        ZoneId zoneId = getPluginZoneId();
        new ScheduledRestartHandler(plugin, plugin.getLangManager()::getMessage, zoneId, discordNotifier)
                .handleScheduledRestarts(restartDates);
    }

    private ZoneId getPluginZoneId() {
        String timezone = plugin.getConfig().getString("timezone", "").trim();
        if (timezone.isEmpty()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid timezone in config: " + timezone + ". Using server default.");
            return ZoneId.systemDefault();
        }
    }
}
