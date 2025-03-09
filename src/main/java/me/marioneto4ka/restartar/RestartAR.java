package me.marioneto4ka.restartar;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bstats.bukkit.Metrics;

import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class RestartAR extends JavaPlugin {
    private int taskId = -1;
    private FileConfiguration langConfig;
    private BossBar bossBar;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLanguage();

        boolean scheduledRestartsEnabled = getConfig().getBoolean("enable-scheduled-restarts", false);
        if (scheduledRestartsEnabled) {
            List<String> restartDates = getConfig().getStringList("restart-dates");
            handleScheduledRestarts(restartDates);
        }

        getCommand("autorestart").setExecutor(new RestartCommand());
        getCommand("ar").setExecutor(new ARCommand());
        int pluginId = 25011;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void loadLanguage() {
        String lang = getConfig().getString("language", "en");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");

        if (!langFile.exists()) {
            saveResource("lang/" + lang + ".yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private class RestartCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("timereload.restart")) {
                sender.sendMessage(getMessage("messages.no-permission"));
                return true;
            }

            int time = getConfig().getInt("default-restart-time", 60);
            if (args.length > 0) {
                try {
                    time = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(getMessage("messages.invalid-time"));
                    return true;
                }
            }

            startCountdown(time);
            sender.sendMessage(getMessage("messages.restart-started", time));
            return true;
        }
    }

    private class ARCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(getMessage("messages.usage-ar"));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "cancel":
                    if (taskId != -1) {
                        Bukkit.getScheduler().cancelTask(taskId);
                        taskId = -1;

                        if (bossBar != null) {
                            bossBar.removeAll();
                            bossBar = null;
                        }

                        sender.sendMessage(getMessage("messages.restart-cancelled"));
                    } else {
                        sender.sendMessage(getMessage("messages.no-active-restart"));
                    }
                    break;

                case "reload":
                    reloadConfig();
                    sender.sendMessage(getMessage("messages.config-reloaded"));
                    break;
                default:
                    sender.sendMessage(getMessage("messages.usage-ar"));
                    break;
            }
            return true;
        }
    }

    private void startCountdown(int seconds) {
        List<Integer> countdownTimes = getConfig().getIntegerList("countdown-announcements");
        List<String> notificationTypes = getConfig().getStringList("notification-type");

        String colorName = getConfig().getString("bossbar-color", "RED").toUpperCase(Locale.ROOT);
        BarColor bossBarColor;
        try {
            bossBarColor = BarColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            bossBarColor = BarColor.RED;
            getLogger().warning("Invalid BossBar color in config.yml! Using RED as default.");
        }

        if (notificationTypes.contains("bossbar")) {
            bossBar = Bukkit.createBossBar(getMessage("messages.bossbar-restart-message", seconds),
                    bossBarColor, BarStyle.SEGMENTED_10, BarFlag.DARKEN_SKY);
            bossBar.setProgress(1.0);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        }

        taskId = new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage(getMessage("messages.restart-done"));
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    Bukkit.shutdown();
                    cancel();
                    return;
                }

                if (notificationTypes.contains("chat") && countdownTimes.contains(timeLeft)) {
                    Bukkit.broadcastMessage(getMessage("messages.restart-message", timeLeft));
                }

                if (notificationTypes.contains("actionbar")) {
                    String actionBarMessage = getMessage("messages.actionbar-restart-message", timeLeft);
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
                    }
                }

                if (notificationTypes.contains("bossbar") && bossBar != null) {
                    bossBar.setTitle(getMessage("messages.bossbar-restart-message", timeLeft));
                    bossBar.setProgress((double) timeLeft / seconds);
                }

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L).getTaskId();
    }

    private void handleScheduledRestarts(List<String> restartDates) {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            int countdownTime = getConfig().getInt("default-restart-time", 60);

            for (String restartDate : restartDates) {
                if (restartDate.contains("-")) {
                    try {
                        LocalDateTime scheduledDateTime = LocalDateTime.parse(restartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        LocalDateTime countdownStart = scheduledDateTime.minusSeconds(countdownTime);

                        if (currentDateTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
                            Bukkit.broadcastMessage(getMessage("messages.scheduled-restart", countdownTime));
                            startCountdown(countdownTime);
                        }
                    } catch (Exception e) {
                        getLogger().warning("Invalid scheduled date format: " + restartDate);
                    }
                } else {
                    try {
                        LocalTime scheduledTime = LocalTime.parse(restartDate, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            Bukkit.broadcastMessage(getMessage("messages.scheduled-restart", countdownTime));
                            startCountdown(countdownTime);
                        }
                    } catch (Exception e) {
                        getLogger().warning("Invalid scheduled time format: " + restartDate);
                    }
                }
            }
        }, 20L, 20L);
    }


    public String getMessage(String path) {
        return langConfig.getString(path, "§c[Error] Message is missing in the config!");
    }

    public String getMessage(String path, int time) {
        return getMessage(path).replace("%time%", String.valueOf(time));
    }
}
