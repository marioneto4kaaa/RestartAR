package me.marioneto4ka.restartar.Discord;

import me.marioneto4ka.restartar.Utils.LangManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebhookSender implements DiscordMessageSender {

    private final Logger logger;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final String footerText;
    private final String footerIconUrl;
    private final String thumbnailUrl;
    private final LangManager langManager;

    public WebhookSender(Logger logger, String webhookUrl, String username, String avatarUrl,
                         String footerText, String footerIconUrl, String thumbnailUrl,
                         LangManager langManager) {
        this.logger = logger;
        this.webhookUrl = webhookUrl;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.footerText = footerText;
        this.footerIconUrl = footerIconUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.langManager = langManager;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public void sendMessage(String restartTime) {
        try {
            String title = escapeJson(langManager.getMessage("messages.discord-webhook-title"));
            String description = escapeJson(langManager.getMessage("messages.discord-webhook-description").replace("%time%", restartTime));
            String footer = escapeJson(langManager.getMessage("messages.discord-webhook-footer"));

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{"
                    + "\"username\":\"" + escapeJson(username) + "\","
                    + "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\","
                    + "\"embeds\":[{"
                    + "    \"title\":\"" + title + "\","
                    + "    \"description\":\"" + description + "\","
                    + "    \"color\":16711680,"
                    + "    \"thumbnail\":{\"url\":\"" + escapeJson(thumbnailUrl) + "\"},"
                    + "    \"footer\":{\"text\":\"" + footer + "\",\"icon_url\":\"" + escapeJson(footerIconUrl) + "\"}"
                    + "}]"
                    + "}";

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                logger.warning("Failed to send webhook message! Response code: " + responseCode);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending webhook message!", e);
        }
    }
}
