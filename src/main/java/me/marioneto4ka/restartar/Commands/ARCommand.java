package me.marioneto4ka.restartar.Commands;

import me.marioneto4ka.restartar.Function.RestartManager;
import me.marioneto4ka.restartar.RestartAR;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ARCommand implements CommandExecutor {

    private final RestartAR plugin;

    private final RestartManager manager;

    public ARCommand(RestartAR plugin) {
        this.plugin = plugin;
        this.manager = new RestartManager(plugin, plugin.getDiscordNotifier());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("messages.usage-ar"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "restart":
                if (!sender.hasPermission("restartar.admin")) {
                    sender.sendMessage(plugin.getMessage("messages.no-permission"));
                    return true;
                }

                int time = plugin.getConfig().getInt("default-restart-time", 60);
                if (args.length > 1) {
                    try {
                        time = parseTimeArgument(args[1]);
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(plugin.getMessage("messages.invalid-time"));
                        return true;
                    }
                }

                plugin.startCountdown(time);
                sender.sendMessage(plugin.getMessage("messages.restart-started", time));
                plugin.sendToDiscord(plugin.getMessage("messages.discord-restart-message", time));
                break;

            case "cancel":
                if (sender instanceof Player) {
                    manager.cancelRestart((Player) sender);
                }
                break;

            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(plugin.getMessage("messages.config-reloaded"));
                break;

            case "help":
                plugin.checkForUpdates(sender);
                break;


            case "disablefeedback":
                if (sender instanceof Player) {
                    manager.toggleFeedback((Player) sender);
                }
                break;

            case "scheduled":
                plugin.showScheduledRestarts(sender);
                break;

            case "now":
                if (sender instanceof Player) {
                    manager.restartNow((Player) sender);
                }
                break;

            default:
                sender.sendMessage(plugin.getMessage("messages.usage-ar"));
                break;
        }
        return true;
    }

    private int parseTimeArgument(String input) {
        if (input.matches("\\d+[smhd]?")) {
            int value = Integer.parseInt(input.replaceAll("[^0-9]", ""));
            char unit = input.charAt(input.length() - 1);

            switch (unit) {
                case 's': return value;
                case 'm': return value * 60;
                case 'h': return value * 3600;
                case 'd': return value * 86400;
                default: return value;
            }
        } else {
            throw new IllegalArgumentException("Invalid time format.");
        }
    }
}
