package me.marioneto4ka.restartar.Discord;

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

    public WebhookSender(Logger logger, String webhookUrl, String username, String avatarUrl, String footerText, String footerIconUrl, String thumbnailUrl) {
        this.logger = logger;
        this.webhookUrl = webhookUrl;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.footerText = footerText;
        this.footerIconUrl = footerIconUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    @Override
    public void sendMessage(String restartTime) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{"
                    + "\"username\":\"" + username + "\","
                    + "\"avatar_url\":\"" + avatarUrl + "\","
                    + "\"embeds\":[{"
                    + "    \"title\":\"Warning!\","
                    + "    \"description\":\"🔄 **The server will restart at**\\n" + restartTime + "\\n\\nPlease save your progress.\","
                    + "    \"color\":16711680,"
                    + "    \"thumbnail\":{\"url\":\"" + thumbnailUrl + "\"},"
                    + "    \"footer\":{\"text\":\"" + footerText + "\",\"icon_url\":\"" + footerIconUrl + "\"}"
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
