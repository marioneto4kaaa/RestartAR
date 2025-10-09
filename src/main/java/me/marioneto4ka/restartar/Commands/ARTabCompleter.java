package me.marioneto4ka.restartar.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ARTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "restart", "cancel", "reload", "help", "disablefeedback", "scheduled", "now"
            );
            for (String sub : subCommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("restart")) {
            List<String> timeOptions = Arrays.asList("60", "30s", "5m", "1h");
            for (String option : timeOptions) {
                if (option.startsWith(args[1].toLowerCase())) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}
