package me.marioneto4ka.restartar.Discord;

import me.marioneto4ka.restartar.RestartAR;
import java.util.logging.Logger;

public class DiscordNotifier {
    private final RestartAR plugin;
    private final Logger logger;
    private DiscordMessageSender sender;

    public DiscordNotifier(RestartAR plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        setupSender();
    }

    private void setupSender() {
        String mode = plugin.getConfig().getString("discord-mode", "none").toLowerCase();
        switch (mode) {
            case "bot":
                sender = new BotSender(
                        logger,
                        plugin.getConfig().getString("discord-bot-token"),
                        plugin.getConfig().getString("discord-channel-id")
                );
                break;

            case "webhook":
                sender = new WebhookSender(
                        logger,
                        plugin.getConfig().getString("discord-webhook-url", ""),
                        plugin.getConfig().getString("discord-username", "RestartAR"),
                        plugin.getConfig().getString("discord-avatar-url", "https://example.com/avatar.png"),
                        plugin.getConfig().getString("discord-footer-text", "⚙ Restart by RestartAR"),
                        plugin.getConfig().getString("discord-footer-icon-url", "https://example.com/footer-icon.png"),
                        plugin.getConfig().getString("discord-thumbnail-url", "https://example.com/thumbnail.png"),
                        plugin.getLangManager()
                );
                break;

            case "none":
            default:
                sender = null;
                logger.info("Discord messages are disabled.");
        }
    }

    public void sendDiscordMessage(String message) {
        if (sender != null) {
            sender.sendMessage(stripMinecraftColors(message));
        } else {
            logger.info("No Discord sender configured.");
        }
    }

    private String stripMinecraftColors(String message) {
        if (message == null) return "";
        return message.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }
}