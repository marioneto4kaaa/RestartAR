package me.marioneto4ka.restartar.Function;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.BiFunction;


public class ScheduledRestartHandler {

    private final JavaPlugin plugin;
    private final Function<String, String> getMessage;

    public ScheduledRestartHandler(JavaPlugin plugin, BiFunction<String, Integer, String> getMessage) {
        this.plugin = plugin;
        this.getMessage = getMessage;
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
                            Bukkit.broadcastMessage(getMessage.apply("messages.scheduled-restart"));
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
                            Bukkit.broadcastMessage(getMessage.apply("messages.scheduled-restart"));
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
                    Bukkit.broadcastMessage(getMessage.apply("messages.restart-started"));
                    Bukkit.getServer().shutdown();
                } else {
                    Bukkit.broadcastMessage(getMessage.apply("messages.restart-message", timeLeft));
                    timeLeft--;
                }
            }
        }, 0L, 20L);
    }
}
