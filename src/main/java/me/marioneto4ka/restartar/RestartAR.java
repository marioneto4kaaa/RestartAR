package me.marioneto4ka.restartar;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import me.marioneto4ka.restartar.Commands.ARCommand;
import me.marioneto4ka.restartar.Commands.ARTabCompleter;
import me.marioneto4ka.restartar.Commands.HelpMessage;
import me.marioneto4ka.restartar.Discord.DiscordNotifier;
import me.marioneto4ka.restartar.Function.*;
import me.marioneto4ka.restartar.Notifications.AdminFeedback;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bstats.bukkit.Metrics;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

public final class RestartAR extends JavaPlugin implements Listener {
    private int taskId = -1;
    private FileConfiguration langConfig;
    private BossBar bossBar;
    private DiscordNotifier discordNotifier;
    private static final String SPIGOT_RESOURCE_ID = "122574";
    private AdminFeedback feedbackNotifier;
    public int getTaskId() { return taskId; }
    public void setTaskId(int id) { taskId = id; }
    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bar) { bossBar = bar; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLanguage();
        this.discordNotifier = new DiscordNotifier(this);
        this.feedbackNotifier = new AdminFeedback(getConfig());

        boolean scheduledRestartsEnabled = getConfig().getBoolean("enable-scheduled-restarts", false);
        if (scheduledRestartsEnabled) {
            List<String> restartDates = getConfig().getStringList("restart-dates");
            handleScheduledRestarts(restartDates);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        ARCommand arCommand = new ARCommand(this);
        getCommand("ar").setExecutor(arCommand);
        getCommand("ar").setTabCompleter(new ARTabCompleter());
        int pluginId = 25011;
        Metrics metrics = new Metrics(this, pluginId);

        UpdateChecker updateChecker = new UpdateChecker(this, UpdateCheckSource.SPIGOT, SPIGOT_RESOURCE_ID);

        updateChecker.onSuccess((senders, latestVersion) -> {
            getLogger().info("The latest version of the plugin is: " + latestVersion);
        }).onFail((senders, exception) -> {
            getLogger().warning("Failed to fetch the latest plugin version: " + exception.getMessage());
        });

        updateChecker.checkNow();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not work.");
        }
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            feedbackNotifier.send(player);
        }, 60L);
    }

    private void loadLanguage() {
        String lang = getConfig().getString("language", "en");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");

        if (!langFile.exists()) {
            saveResource("lang/" + lang + ".yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void checkForUpdates(CommandSender sender) {
        UpdateChecker updateChecker = new UpdateChecker(this, UpdateCheckSource.SPIGOT, SPIGOT_RESOURCE_ID);

        updateChecker.onSuccess((senders, latestVersion) -> {
            new HelpMessage(this).send(sender, latestVersion);
        }).onFail((senders, exception) -> {
            sender.sendMessage(getMessage("messages.update-check-failed") + exception.getMessage());
        });

        updateChecker.checkNow();
    }

    private ZoneId getPluginZoneId() {
        String timezone = getConfig().getString("timezone", "").trim();
        if (timezone.isEmpty()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            getLogger().warning("Invalid timezone in config: " + timezone + ". Using server default.");
            return ZoneId.systemDefault();
        }
    }

    private void handleScheduledRestarts(List<String> restartDates) {
        DiscordNotifier discordNotifier = new DiscordNotifier(this);

        ZoneId zoneId = getPluginZoneId();

        new ScheduledRestartHandler(this, zoneId, discordNotifier)
                .handleScheduledRestarts(restartDates);
    }

    // Выше не трогай
    public void sendToDiscord(String msg) {
        discordNotifier.sendDiscordMessage(msg);
    }

    public void showScheduledRestarts(CommandSender sender) {
        List<String> restartDates = getConfig().getStringList("restart-dates");

        List<String> datedRestarts = new ArrayList<>();
        List<String> dailyRestarts = new ArrayList<>();

        for (String entry : restartDates) {
            if (entry.contains("-")) {
                datedRestarts.add(entry);
            } else {
                dailyRestarts.add(entry);
            }
        }

        if (datedRestarts.isEmpty() && dailyRestarts.isEmpty()) {
            sender.sendMessage("§cNo scheduled restarts at the moment.");
            return;
        }

        sender.sendMessage("§e§lScheduled Restarts:");

        if (!datedRestarts.isEmpty()) {
            sender.sendMessage("§6One-time restarts:");
            for (String restart : datedRestarts) {
                sender.sendMessage("  §7- §f" + restart);
            }
        }

        if (!dailyRestarts.isEmpty()) {
            sender.sendMessage("§6Daily restarts:");
            for (String restart : dailyRestarts) {

                sender.sendMessage("  §7- §f" + restart);
            }
        }
    }

    public void startCountdown(int seconds) {
        List<Integer> countdownTimes = getConfig().getIntegerList("countdown-announcements");
        List<String> notificationTypes = getConfig().getStringList("notification-type");

        String colorName = getConfig().getString("bossbar-color", "RED").toUpperCase(Locale.ROOT);
        BarColor bossBarColor;
        try {
            bossBarColor = BarColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            bossBarColor = BarColor.RED;
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
                    discordNotifier.sendDiscordMessage(getMessage("messages.discord-restart-done"));
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    triggerRestart();
                    cancel();
                    return;
                }

                int preRestartExecuteTime = getConfig().getInt("pre-restart-execute-time", 0);

                if (timeLeft <= preRestartExecuteTime && timeLeft > 0) {
                    Bukkit.broadcastMessage(getMessage("messages.restart-done"));

                    boolean executePreRestartCommands = getConfig().getBoolean("execute-pre-restart-commands", true);

                    if (executePreRestartCommands) {
                        List<String> preRestartCommands = getConfig().getStringList("pre-restart-commands");
                        for (String command : preRestartCommands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        }
                    }

                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    triggerRestart();
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

                List<Integer> titleCountdownTimes = getConfig().getIntegerList("title-countdown-announcements");
                boolean titleEverySecond = getConfig().getBoolean("title-update-every-second", false);

                if (notificationTypes.contains("title") && (titleEverySecond || titleCountdownTimes.contains(timeLeft))) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String titleMessage = getMessage("messages.title-restart-message", timeLeft);
                        String subtitleMessage = getMessage("messages.subtitle-restart-message", timeLeft);
                        player.sendTitle(titleMessage, subtitleMessage, 10, 40, 10);
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

    public String getMessage(String path) {
        return langConfig.getString(path, "§c[Error] Message is missing in the config!");
    }

    public String getMessage(String path, int time) {
        return getMessage(path).replace("%time%", String.valueOf(time));
    }

    public String getMessage(String path, int time, String executor) {
        return getMessage(path, time).replace("%executor%", executor);
    }

    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }
}
