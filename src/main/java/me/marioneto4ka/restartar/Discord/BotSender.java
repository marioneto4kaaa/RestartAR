package me.marioneto4ka.restartar.Discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BotSender implements DiscordMessageSender {

    private final Logger logger;
    private final String token;
    private final String channelId;
    private JDA jda;

    public BotSender(Logger logger, String token, String channelId) {
        this.logger = logger;
        this.token = token;
        this.channelId = channelId;
        setupBot();
    }

    private void setupBot() {
        try {
            jda = JDABuilder.createDefault(token).build();
            logger.info("Discord bot connected successfully.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect Discord bot!", e);
        }
    }

    @Override
    public void sendMessage(String message) {
        if (jda == null) {
            logger.warning("JDA is not initialized!");
            return;
        }
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            logger.warning("Discord channel not found!");
        }
    }
}
