package me.marioneto4ka.restartar.Function;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordNotifier {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final FileConfiguration config;
    private JDA jda;
    private String discordChannelId;

    public DiscordNotifier(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.getConfig();
    }

    public void setupBot() {
        String mode = config.getString("discord-mode", "none").toLowerCase();
        if (!"bot".equals(mode)) {
            logger.info("Discord bot is not enabled (mode: " + mode + ").");
            return;
        }

        String token = config.getString("discord-bot-token");
        discordChannelId = config.getString("discord-channel-id");

        if (token == null || discordChannelId == null) {
            logger.warning("Discord bot token or channel ID is missing in config.yml!");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token).build();
            logger.info("Discord bot connected successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect Discord bot!", e);
        }
    }

    private void sendBotMessage(String message) {
        if (jda == null) {
            logger.warning("JDA is not initialized!");
            return;
        }
        TextChannel channel = jda.getTextChannelById(discordChannelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            logger.warning("Discord channel not found!");
        }
    }

    private void sendWebhookMessage(String restartTime) {
        String webhookUrl = config.getString("discord-webhook-url");
        String avatarUrl = config.getString("discord-avatar-url", "https://example.com/avatar.png");
        String thumbnailUrl = config.getString("discord-thumbnail-url", avatarUrl);
        String footerIconUrl = config.getString("discord-footer-icon-url", avatarUrl);
        String username = config.getString("discord-username", "RestartAR");
        String footerText = config.getString("discord-footer-text", "RestartAR");

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            logger.warning("Webhook URL is missing in config.yml!");
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
                    restartTime + "\\n\\nPlease save your progress.\"," +
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
                logger.warning("Failed to send webhook message! Response code: " + responseCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending webhook message!", e);
        }
    }

    public void sendDiscordMessage(String messageOrTime) {
        String mode = config.getString("discord-mode", "none").toLowerCase();

        switch (mode) {
            case "webhook":
                sendWebhookMessage(messageOrTime);
                break;
            case "bot":
                sendBotMessage(messageOrTime);
                break;
            case "none":
                logger.info("Discord messages are disabled (mode: none). No message sent.");
                break;
            default:
                logger.warning("Invalid 'discord-mode' in config.yml! Use 'bot', 'webhook', or 'none'.");
        }
    }
}
