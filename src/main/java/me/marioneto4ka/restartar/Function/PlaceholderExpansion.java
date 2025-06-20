package me.marioneto4ka.restartar.Function;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final Plugin plugin;

    public PlaceholderExpansion(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "restartar";
    }

    @Override
    public String getAuthor() {
        return "marioneto4ka";
    }

    @Override
    public String getVersion() {
        return "1.9.1";
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String language = plugin.getConfig().getString("language", "en");
        String hourWord = language.equals("ru") ? "час" : "hour";
        String minuteWord = language.equals("ru") ? "минута" : "minute";
        String secondWord = language.equals("ru") ? "секунда" : "second";

        if (params.equalsIgnoreCase("last_seconds")) {
            int elapsedSeconds = calculateElapsedSecondsSinceLastRestart();
            return String.valueOf(elapsedSeconds);
        }

        if (params.equalsIgnoreCase("last_hhmmss")) {
            int elapsedSeconds = calculateElapsedSecondsSinceLastRestart();
            return formatTime(elapsedSeconds, hourWord, minuteWord, secondWord);
        }

        if (params.equalsIgnoreCase("last_formatted")) {
            int elapsedSeconds = calculateElapsedSecondsSinceLastRestart();
            return formatTimeWithText(elapsedSeconds, language);
        }

        return null;
    }

    private int calculateElapsedSecondsSinceLastRestart() {
        String lastRestartTimeString = plugin.getConfig().getString("last-restart-time");
        if (lastRestartTimeString == null) {
            return 0;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date lastRestartDate = dateFormat.parse(lastRestartTimeString);
            long elapsedMillis = System.currentTimeMillis() - lastRestartDate.getTime();
            return (int) (elapsedMillis / 1000);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String formatTime(int seconds, String hourWord, String minuteWord, String secondWord) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;

        StringBuilder timeString = new StringBuilder();

        if (hours > 0) {
            timeString.append(String.format("%02d", hours)).append(":");
        } else {
            timeString.append("00:");
        }

        if (minutes > 0) {
            timeString.append(String.format("%02d", minutes)).append(":");
        } else {
            timeString.append("00:");
        }

        timeString.append(String.format("%02d", remainingSeconds));
        return timeString.toString();
    }

    private String formatTimeWithText(int seconds, String language) {
        if (seconds == 0) return language.equals("ru") ? "0 секунд" : "0 seconds";

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int remainingSeconds = seconds % 60;

        StringBuilder timeString = new StringBuilder();

        if (hours > 0) {
            timeString.append(hours).append(" ");
            timeString.append(language.equals("ru") ? (hours == 1 ? "час" : hours <= 4 ? "часа" : "часов") : "hour");
            timeString.append(" ");
        }

        if (minutes > 0) {
            timeString.append(minutes).append(" ");
            timeString.append(language.equals("ru") ? (minutes == 1 ? "минута" : minutes <= 4 ? "минуты" : "минут") : "minute");
            timeString.append(" ");
        }

        if (remainingSeconds > 0) {
            timeString.append(remainingSeconds).append(" ");
            timeString.append(language.equals("ru") ? (remainingSeconds == 1 ? "секунда" : remainingSeconds <= 4 ? "секунды" : "секунд") : "second");
        }

        return timeString.toString().trim();
    }
}