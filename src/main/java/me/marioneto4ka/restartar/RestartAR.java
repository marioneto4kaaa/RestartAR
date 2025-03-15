package me.marioneto4ka.restartar;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InteractionFailureException;
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

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class RestartAR extends JavaPlugin {
    private int taskId = -1;
    private FileConfiguration langConfig;
    private BossBar bossBar;
    private JDA jda;
    private String discordChannelId;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLanguage();
        setupDiscordBot();

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

    private void setupDiscordBot() {
        String mode = getConfig().getString("discord-mode", "none").toLowerCase();
        if (!"bot".equals(mode)) {
            getLogger().info("Discord bot is not enabled (mode: " + mode + ").");
            return;
        }

        String token = getConfig().getString("discord-bot-token");
        discordChannelId = getConfig().getString("discord-channel-id");

        if (token == null || discordChannelId == null) {
            getLogger().warning("Discord bot token or channel ID is missing in config.yml!");
            return;
        }

        jda = JDABuilder.createDefault(token).build();
        getLogger().info("Discord bot connected successfully.");
    }

    private void sendDiscordBotMessage(String message) {
        if (jda == null) {
            getLogger().warning("JDA is not initialized!");
            return;
        }
        TextChannel channel = jda.getTextChannelById(discordChannelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            getLogger().warning("Discord channel not found!");
        }
    }


    public void sendDiscordMessage(String message) {
        String mode = getConfig().getString("discord-mode", "none").toLowerCase();

        switch (mode) {
            case "webhook":
                sendDiscordWebhookMessage(message);
                break;
            case "bot":
                sendDiscordBotMessage(message);
                break;
            case "none":
                getLogger().info("Discord messages are disabled (mode: none). No message sent.");
                break;
            default:
                getLogger().warning("Invalid 'discord-mode' in config.yml! Use 'bot', 'webhook', or 'none'.");
        }
    }

    private void sendDiscordWebhookMessage(String restartTime) {
        String webhookUrl = getConfig().getString("discord-webhook-url");
        String avatarUrl = getConfig().getString("discord-avatar-url", "default_avatar_url");
        String thumbnailUrl = getConfig().getString("discord-thumbnail-url", avatarUrl);
        String footerIconUrl = getConfig().getString("discord-footer-icon-url", avatarUrl);
        String username = getConfig().getString("discord-username", "ExampleName");
        String footerText = getConfig().getString("discord-footer-text", "ExampleName.net");

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URL is missing in config.yml!");
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{" +
                    "\"username\": \"" + username + "\"," +
                    "\"avatar_url\": \"" + avatarUrl + "\"," +
                    "\"embeds\": [{" +
                    "    \"title\": \"Warning!\"," +
                    "    \"description\": \"🔄 **The server will restart at**\\n" +
                    "    " + restartTime + "\\n\\n" +
                    "    Please save your progress.\"," +
                    "    \"color\": 16711680," +
                    "    \"thumbnail\": { \"url\": \"" + thumbnailUrl + "\" }," +
                    "    \"footer\": { \"text\": \"" + footerText + "\", \"icon_url\": \"" + footerIconUrl + "\" }" +
                    "}]" +
                    "}";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                getLogger().log(Level.WARNING, "Failed to send webhook message! Response code: " + responseCode);
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error sending webhook message!", e);
        }
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
                String timeArg = args[0].toLowerCase();
                try {
                    time = parseTimeArgument(timeArg);
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(getMessage("messages.invalid-time"));
                    return true;
                }
            }

            startCountdown(time);
            sender.sendMessage(getMessage("messages.restart-started", time));
            sendDiscordMessage(getMessage("messages.discord-restart-message", time));
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
                    sendDiscordMessage(getMessage("messages.discord-restart-done"));
                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }
                    Bukkit.shutdown();
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