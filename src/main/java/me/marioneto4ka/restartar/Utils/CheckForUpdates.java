package me.marioneto4ka.restartar.Utils;

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import me.marioneto4ka.restartar.Commands.HelpMessage;
import me.marioneto4ka.restartar.RestartAR;
import org.bukkit.command.CommandSender;

public class CheckForUpdates {

    private final RestartAR plugin;
    private final LangManager langManager;
    private static final String SPIGOT_RESOURCE_ID = "122574";

    public CheckForUpdates(RestartAR plugin, LangManager langManager) {
        this.plugin = plugin;
        this.langManager = langManager;
    }

    public void performUpdateCheck(CommandSender sender) {
        UpdateChecker updateChecker = new UpdateChecker(plugin, UpdateCheckSource.SPIGOT, SPIGOT_RESOURCE_ID);

        updateChecker.onSuccess((senders, latestVersion) -> {
            new HelpMessage(plugin).send(sender, latestVersion);
        }).onFail((senders, exception) -> {
            sender.sendMessage(langManager.getMessage("messages.update-check-failed") + exception.getMessage());
        });

        updateChecker.checkNow();
    }
}