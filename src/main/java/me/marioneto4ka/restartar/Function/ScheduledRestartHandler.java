package me.marioneto4ka.restartar.Function;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class ScheduledRestartHandler {

    private final JavaPlugin plugin;
    private final Function<String, String> getMessage;
    private final ZoneId zoneId;

    public ScheduledRestartHandler(JavaPlugin plugin, Function<String, String> getMessage, ZoneId zoneId) {
        this.plugin = plugin;
        this.getMessage = getMessage;
        this.zoneId = zoneId;
    }

    public void handleScheduledRestarts(List<String> restartDates) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ZonedDateTime now = ZonedDateTime.now(zoneId);
            String currentDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentTime = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            DayOfWeek currentDay = now.getDayOfWeek();
            int countdownTime = plugin.getConfig().getInt("default-restart-time", 60);

            for (String restartDate : restartDates) {
                try {
                    if (restartDate.contains("-")) {
                        LocalDateTime scheduledDateTime = LocalDateTime.parse(restartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        ZonedDateTime zonedScheduled = scheduledDateTime.atZone(zoneId);
                        ZonedDateTime countdownStart = zonedScheduled.minusSeconds(countdownTime);

                        if (currentDateTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
                            String scheduledTemplate = getMessage.apply("messages.scheduled-restart");
                            String scheduledMessage = scheduledTemplate.replace("%time%", String.valueOf(countdownTime));
                            Bukkit.broadcastMessage(scheduledMessage);
                            startCountdown(countdownTime);
                        }
                    } else if (restartDate.matches("(?i)(mon|tue|wed|thu|fri|sat|sun)\\s+\\d{2}:\\d{2}:\\d{2}")) {
                        String[] parts = restartDate.split("\\s+");
                        String dayPart = parts[0].toUpperCase();
                        String timePart = parts[1];

                        DayOfWeek scheduledDay = parseDayOfWeek(dayPart);
                        if (scheduledDay == null) continue;

                        LocalTime scheduledTime = LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentDay == scheduledDay && currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            String scheduledTemplate = getMessage.apply("messages.scheduled-restart");
                            String scheduledMessage = scheduledTemplate.replace("%time%", String.valueOf(countdownTime));
                            Bukkit.broadcastMessage(scheduledMessage);
                            startCountdown(countdownTime);
                        }
                    } else {
                        LocalTime scheduledTime = LocalTime.parse(restartDate, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            String scheduledTemplate = getMessage.apply("messages.scheduled-restart");
                            String scheduledMessage = scheduledTemplate.replace("%time%", String.valueOf(countdownTime));
                            Bukkit.broadcastMessage(scheduledMessage);
                            startCountdown(countdownTime);
                        }
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

    private void startCountdown(int countdownTime) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    String startedMessage = getMessage.apply("messages.restart-started").replace("%time%", String.valueOf(0));
                    Bukkit.broadcastMessage(startedMessage);
                    Bukkit.getServer().shutdown();
                } else {
                    String messageTemplate = getMessage.apply("messages.restart-message");
                    String message = messageTemplate.replace("%time%", String.valueOf(timeLeft));
                    Bukkit.broadcastMessage(message);
                    timeLeft--;
                }
            }
        }, 0L, 20L);
    }
}
