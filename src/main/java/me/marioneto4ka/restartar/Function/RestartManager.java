package me.marioneto4ka.restartar.Function;

import me.marioneto4ka.restartar.Discord.DiscordNotifier;
import me.marioneto4ka.restartar.RestartAR;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

public class RestartManager {


    private final RestartAR plugin;
    private final DiscordNotifier discordNotifier;

    public RestartManager(RestartAR plugin, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.discordNotifier = discordNotifier;
    }

    public void cancelRestart(Player sender) {
        if (plugin.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(plugin.getTaskId());
            plugin.setTaskId(-1);

            BossBar bossBar = plugin.getBossBar();
            if (bossBar != null) {
                bossBar.removeAll();
                plugin.setBossBar(null);
            }

            sender.sendMessage(plugin.getMessage("messages.restart-cancelled"));
        } else {
            sender.sendMessage(plugin.getMessage("messages.no-active-restart"));
        }
    }

    public void toggleFeedback(Player player) {
        if (!player.hasPermission("restartar.admin")) {
            player.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }

        boolean feedbackNotification = plugin.getConfig().getBoolean("admin-feedback-notification", true);

        if (!feedbackNotification) {
            player.sendMessage("§cAdmin feedback notification is already disabled.");
            return;
        }

        plugin.getConfig().set("admin-feedback-notification", false);
        plugin.saveConfig();

        player.sendMessage("§aAdmin feedback notification has been disabled.");
    }

    public void restartNow(Player player) {
        if (!player.hasPermission("restartar.admin")) {
            player.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }

        String message = plugin.getMessage("messages.restart-now", 0, player.getName());
        player.sendMessage(message);

        // Используем discordNotifier напрямую
        String discordMessage = plugin.getMessage("messages.discord-restart-now", 0, player.getName());
        if (discordNotifier != null) {
            discordNotifier.sendDiscordMessage(discordMessage);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
            Bukkit.shutdown();
        });
    }
}
