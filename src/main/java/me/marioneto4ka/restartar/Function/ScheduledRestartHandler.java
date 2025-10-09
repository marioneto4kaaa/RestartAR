package me.marioneto4ka.restartar.Function;

import me.marioneto4ka.restartar.Discord.DiscordNotifier;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class ScheduledRestartHandler {

    private final JavaPlugin plugin;
    private final Function<String, String> getMessage;
    private final ZoneId zoneId;
    private BossBar bossBar;
    private final DiscordNotifier discordNotifier;

    public ScheduledRestartHandler(JavaPlugin plugin, Function<String, String> getMessage, ZoneId zoneId, DiscordNotifier discordNotifier) {
        this.plugin = plugin;
        this.getMessage = getMessage;
        this.zoneId = zoneId;
        this.discordNotifier = discordNotifier;
    }

    public void handleScheduledRestarts(List<String> restartDates) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            int countdownTime = plugin.getConfig().getInt("default-restart-time", 60);

            for (String restartDate : restartDates) {
                try {
                    restartDate = restartDate.trim();

                    if (restartDate.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                        LocalDateTime scheduled = LocalDateTime.parse(restartDate,
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        ZonedDateTime scheduledZoned = scheduled.atZone(zoneId)
                                .minusSeconds(countdownTime);

                        if (!now.isBefore(scheduledZoned) && now.isBefore(scheduledZoned.plusSeconds(1))) {
                            startFullCountdown(countdownTime);
                        }

                    } else if (restartDate.matches("(?i)(MON|TUE|WED|THU|FRI|SAT|SUN) \\d{2}:\\d{2}:\\d{2}")) {
                        String[] parts = restartDate.split("\\s+");
                        DayOfWeek scheduledDay = parseDayOfWeek(parts[0]);
                        LocalTime scheduledTime = LocalTime.parse(parts[1],
                                DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (now.getDayOfWeek() == scheduledDay &&
                                now.toLocalTime().getHour() == countdownStart.getHour() &&
                                now.toLocalTime().getMinute() == countdownStart.getMinute() &&
                                now.toLocalTime().getSecond() == countdownStart.getSecond()) {
                            startFullCountdown(countdownTime);
                        }

                    } else if (restartDate.matches("\\d{2}:\\d{2}:\\d{2}")) {
                        LocalTime scheduledTime = LocalTime.parse(restartDate,
                                DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (now.toLocalTime().getHour() == countdownStart.getHour() &&
                                now.toLocalTime().getMinute() == countdownStart.getMinute() &&
                                now.toLocalTime().getSecond() == countdownStart.getSecond()) {
                            startFullCountdown(countdownTime);
                        }

                    } else {
                        plugin.getLogger().warning("Invalid scheduled restart format: " + restartDate);
                    }

                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid scheduled restart format: " + restartDate);
                }
            }
        }, 20L, 20L);
    }

    private DayOfWeek parseDayOfWeek(String day) {
        switch (day.toUpperCase()) {
            case "MON":
            case "MONDAY": return DayOfWeek.MONDAY;
            case "TUE":
            case "TUESDAY": return DayOfWeek.TUESDAY;
            case "WED":
            case "WEDNESDAY": return DayOfWeek.WEDNESDAY;
            case "THU":
            case "THURSDAY": return DayOfWeek.THURSDAY;
            case "FRI":
            case "FRIDAY": return DayOfWeek.FRIDAY;
            case "SAT":
            case "SATURDAY": return DayOfWeek.SATURDAY;
            case "SUN":
            case "SUNDAY": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private void startFullCountdown(int seconds) {
        List<Integer> countdownTimes = plugin.getConfig().getIntegerList("countdown-announcements");
        List<String> notificationTypes = plugin.getConfig().getStringList("notification-type");

        String colorName = plugin.getConfig().getString("bossbar-color", "RED").toUpperCase(Locale.ROOT);
        BarColor bossBarColor;
        try {
            bossBarColor = BarColor.valueOf(colorName);
        } catch (IllegalArgumentException e) {
            bossBarColor = BarColor.RED;
        }

        if (notificationTypes.contains("bossbar")) {
            bossBar = Bukkit.createBossBar(getMessage.apply("messages.bossbar-restart-message").replace("%time%", String.valueOf(seconds)),
                    bossBarColor, BarStyle.SEGMENTED_10, BarFlag.DARKEN_SKY);
            bossBar.setProgress(1.0);
            for (Player player : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(player);
            }
        }

        discordNotifier.sendDiscordMessage(getMessage.apply("messages.restart-message").replace("%time%", String.valueOf(seconds)));

        new BukkitRunnable() {
            int timeLeft = seconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage(getMessage.apply("messages.restart-done"));
                    discordNotifier.sendDiscordMessage(getMessage.apply("messages.restart-done"));

                    if (bossBar != null) {
                        bossBar.removeAll();
                        bossBar = null;
                    }

                    String lastRestartTime = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    plugin.getConfig().set("last-restart-time", lastRestartTime);
                    plugin.saveConfig();

                    Bukkit.shutdown();
                    cancel();
                    return;
                }

                if (notificationTypes.contains("chat") && countdownTimes.contains(timeLeft)) {
                    String msg = getMessage.apply("messages.restart-message").replace("%time%", String.valueOf(timeLeft));
                    Bukkit.broadcastMessage(msg);
                    discordNotifier.sendDiscordMessage(msg);
                }

                if (notificationTypes.contains("actionbar")) {
                    String actionBarMessage = getMessage.apply("messages.actionbar-restart-message").replace("%time%", String.valueOf(timeLeft));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionBarMessage));
                    }
                }

                List<Integer> titleCountdownTimes = plugin.getConfig().getIntegerList("title-countdown-announcements");
                boolean titleEverySecond = plugin.getConfig().getBoolean("title-update-every-second", false);

                if (notificationTypes.contains("title") && (titleEverySecond || titleCountdownTimes.contains(timeLeft))) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String titleMessage = getMessage.apply("messages.title-restart-message").replace("%time%", String.valueOf(timeLeft));
                        String subtitleMessage = getMessage.apply("messages.subtitle-restart-message").replace("%time%", String.valueOf(timeLeft));
                        player.sendTitle(titleMessage, subtitleMessage, 10, 40, 10);
                    }
                }

                if (notificationTypes.contains("bossbar") && bossBar != null) {
                    bossBar.setTitle(getMessage.apply("messages.bossbar-restart-message").replace("%time%", String.valueOf(timeLeft)));
                    bossBar.setProgress((double) timeLeft / seconds);
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
