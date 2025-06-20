package me.marioneto4ka.restartar;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import me.marioneto4ka.restartar.Function.*;
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
import org.bukkit.command.TabCompleter;
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
import java.util.*;

public final class RestartAR extends JavaPlugin implements Listener {
    private int taskId = -1;
    private FileConfiguration langConfig;
    private BossBar bossBar;
    private DiscordNotifier discordNotifier;
    private static final String SPIGOT_RESOURCE_ID = "122574";
    private AdminFeedbackNotifier feedbackNotifier;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLanguage();
        discordNotifier = new DiscordNotifier(this);
        discordNotifier.setupBot();
        this.feedbackNotifier = new AdminFeedbackNotifier(getConfig());

        boolean scheduledRestartsEnabled = getConfig().getBoolean("enable-scheduled-restarts", false);
        if (scheduledRestartsEnabled) {
            List<String> restartDates = getConfig().getStringList("restart-dates");
            handleScheduledRestarts(restartDates);
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        ARCommand arCommand = new ARCommand();
        getCommand("ar").setExecutor(new ARCommand());
        getCommand("ar").setTabCompleter(arCommand);
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

    private void checkForUpdates(CommandSender sender) {
        UpdateChecker updateChecker = new UpdateChecker(this, UpdateCheckSource.SPIGOT, SPIGOT_RESOURCE_ID);

        updateChecker.onSuccess((senders, latestVersion) -> {
            new HelpMessageSender(this).send(sender, latestVersion);
        }).onFail((senders, exception) -> {
            sender.sendMessage(getMessage("messages.update-check-failed") + exception.getMessage());
        });

        updateChecker.checkNow();
    }

    private void handleScheduledRestarts(List<String> restartDates) {
        new ScheduledRestartHandler(this).handleScheduledRestarts(restartDates);
    }
// Выше не трогай
    public void sendToDiscord(String msg) {
        discordNotifier.sendDiscordMessage(msg);
    }

    public void triggerRestart() {
        String lastRestartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        getConfig().set("last-restart-time", lastRestartTime);
        saveConfig();

        if (getConfig().getBoolean("restart-instead-of-stop", false)) {
             Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        } else {
            Bukkit.shutdown();
        }
    }

    private class ARCommand implements CommandExecutor, TabCompleter {

        private static final int SPIGOT_RESOURCE_ID = 122574;

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                List<String> subCommands = Arrays.asList("restart", "cancel", "reload", "help", "disablefeedback", "scheduled", "now");
                for (String sub : subCommands) {
                    if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                        completions.add(sub);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("restart")) {
                List<String> timeOptions = Arrays.asList("60", "30s", "5m", "1h");
                for (String option : timeOptions) {
                    if (option.startsWith(args[1].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }

            return completions;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(getMessage("messages.usage-ar"));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "restart":
                    if (!sender.hasPermission("restartar.admin")) {
                        sender.sendMessage(getMessage("messages.no-permission"));
                        return true;
                    }

                    int time = getConfig().getInt("default-restart-time", 60);
                    if (args.length > 1) {
                        try {
                            time = parseTimeArgument(args[1]);
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(getMessage("messages.invalid-time"));
                            return true;
                        }
                    }

                    startCountdown(time);
                    sender.sendMessage(getMessage("messages.restart-started", time));
                    discordNotifier.sendDiscordMessage(getMessage("messages.discord-restart-message", time));
                    break;

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

                case "help":
                    checkForUpdates(sender);
                    break;

                case "disablefeedback":
                    if (sender instanceof Player && sender.hasPermission("restartar.admin")) {
                        boolean feedbackNotification = getConfig().getBoolean("admin-feedback-notification", true);

                        String lang = getConfig().getString("language", "en").toLowerCase();
                        boolean isRu = lang.equals("ru");

                        if (!feedbackNotification) {
                            String message = isRu
                                    ? "§cУведомления для администраторов уже отключены."
                                    : "§cAdmin feedback notification is already disabled.";
                            sender.sendMessage(message);
                            return true;
                        }

                        RestartAR plugin = RestartAR.this;
                        plugin.getConfig().set("admin-feedback-notification", false);
                        plugin.saveConfig();

                        String successMessage = isRu
                                ? "§aУведомления для администраторов были отключены."
                                : "§aAdmin feedback notification has been disabled.";
                        sender.sendMessage(successMessage);
                    } else {
                        String lang = getConfig().getString("language", "en").toLowerCase();
                        boolean isRu = lang.equals("ru");

                        String noPermissionMessage = isRu
                                ? "§cУ вас нет прав для выполнения этой команды."
                                : "§cYou don't have permission to do that.";
                        sender.sendMessage(noPermissionMessage);
                    }
                    return true;

                case "scheduled":
                    showScheduledRestarts(sender);
                    break;

                case "now":
                    if (!sender.hasPermission("restartar.admin")) {
                        sender.sendMessage(getMessage("messages.no-permission"));
                        return true;
                    }

                    String message = getMessage("messages.restart-now", 0, sender.getName());
                    sender.sendMessage(message);

                    String discordMessage = getMessage("messages.discord-restart-now", 0, sender.getName());
                    discordNotifier.sendDiscordMessage(discordMessage);

                    Bukkit.getScheduler().runTask(RestartAR.this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
                        triggerRestart();
                    });
                    break;


                default:
                    sender.sendMessage(getMessage("messages.usage-ar"));
                    break;
            }
            return true;
        }

        private int parseTimeArgument(String input) {
            if (input.matches("\\d+[smhd]?")) {
                int value = Integer.parseInt(input.replaceAll("[^0-9]", ""));
                char unit = input.charAt(input.length() - 1);

                switch (unit) {
                    case 's':
                        return value;
                    case 'm':
                        return value * 60;
                    case 'h':
                        return value * 3600;
                    case 'd':
                        return value * 86400;
                    default:
                        return value;
                }
            } else {
                throw new IllegalArgumentException("Invalid time format.");
            }
        }
    }

    private void showScheduledRestarts(CommandSender sender) {
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

    private void startCountdown(int seconds) {
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
}
