package me.marioneto4ka.restartar.Function;

import org.bukkit.Bukkit;

import me.marioneto4ka.restartar.RestartAR;

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
            String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            int countdownTime = plugin.getConfig().getInt("default-restart-time", 60);

            for (String restartDate : restartDates) {
                if (restartDate.contains("-")) {
                    try {
                        LocalDateTime scheduledDateTime = LocalDateTime.parse(restartDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        LocalDateTime countdownStart = scheduledDateTime.minusSeconds(countdownTime);

                        if (currentDateTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))) {
                            Bukkit.broadcastMessage(plugin.getMessage("messages.scheduled-restart"));
                            startCountdown(countdownTime);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid scheduled date format: " + restartDate);
                    }
                } else {
                    try {
                        LocalTime scheduledTime = LocalTime.parse(restartDate, DateTimeFormatter.ofPattern("HH:mm:ss"));
                        LocalTime countdownStart = scheduledTime.minusSeconds(countdownTime);

                        if (currentTime.equals(countdownStart.format(DateTimeFormatter.ofPattern("HH:mm:ss")))) {
                            Bukkit.broadcastMessage(plugin.getMessage("messages.scheduled-restart"));
                            startCountdown(countdownTime);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid scheduled time format: " + restartDate);
                    }
                }
            }
        }, 20L, 20L);
    }

    private void startCountdown(int countdownTime) {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int timeLeft = countdownTime;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage(plugin.getMessage("messages.restart-started", timeLeft));
                    Bukkit.getServer().shutdown();
                } else {
                    Bukkit.broadcastMessage(plugin.getMessage("messages.restart-message", timeLeft));
                    timeLeft--;
                }
            }
        }, 0L, 20L);
    }
}
