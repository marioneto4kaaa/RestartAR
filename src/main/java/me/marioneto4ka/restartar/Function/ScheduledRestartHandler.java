package me.marioneto4ka.restartar.Function;

import org.bukkit.Bukkit;

import me.marioneto4ka.restartar.RestartAR;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ScheduledRestartHandler {
    private final RestartAR plugin;

    public ScheduledRestartHandler(RestartAR plugin) {
        this.plugin = plugin;
    }
    public void handleScheduledRestarts(List<String> restartDates) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();
            String currentDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentTime = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            DayOfWeek currentDay = now.getDayOfWeek();
            int countdownTime = plugin.getConfig().getInt("default-restart-time", 60);

            for (String restartDate : restartDates) {
                try {
                    if (restartDate.contains("-")) {
                        LocalDateTime scheduledDateTime = LocalDateTime.parse(restartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        LocalDateTime countdownStart = scheduledDateTime.minusSeconds(countdownTime);

                        if (currentDateTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
                            String scheduledMessage = plugin.getMessage("messages.scheduled-restart", countdownTime);
                            Bukkit.broadcastMessage(scheduledMessage);
                            startCountdown(countdownTime);
                        }
                    } else if (restartDate.matches("(?i)(mon|tue|wed|thu|fri|sat|sun)\\s+\\d{2}:\\d{2}:\\d{2}")) {
                        String[] parts = restartDate.split("\\s+");
                        String dayPart = parts[0].toUpperCase();
                        String timePart = parts[1];

                        DayOfWeek scheduledDay = parseDayOfWeek(dayPart);
                        if (scheduledDay == null) {
                            plugin.getLogger().warning("Invalid day of week in scheduled restart: " + dayPart);
                            continue;
                        }

                        LocalTime scheduledTime = LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentDay == scheduledDay && currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            String scheduledMessage = plugin.getMessage("messages.scheduled-restart", countdownTime);
                            Bukkit.broadcastMessage(scheduledMessage);
                            startCountdown(countdownTime);
                        }
                    } else {
                        LocalTime scheduledTime = LocalTime.parse(restartDate, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            String scheduledMessage = plugin.getMessage("messages.scheduled-restart", countdownTime);
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
                    String startedMessage = plugin.getMessage("messages.restart-started", 0);
                    Bukkit.broadcastMessage(startedMessage);
                    plugin.triggerRestart();
                } else {
                    String message = plugin.getMessage("messages.restart-message", timeLeft);
                    Bukkit.broadcastMessage(message);
                    timeLeft--;
                }
            }
        }, 0L, 20L);
    }
}
