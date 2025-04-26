package me.marioneto4ka.restartar.Function;



import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class HelpMessageSender {

    private final JavaPlugin plugin;

    public HelpMessageSender(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void send(CommandSender sender, String latestVersion) {
        String currentVersion = plugin.getDescription().getVersion();
        String lang = plugin.getConfig().getString("language", "en").toLowerCase();

        String line1, line2, versionText;
        String restartCmd, cancelCmd, reloadCmd, helpCmd, listCmd, nowCmd, disableFeedbackCmd;

        if (lang.equals("ru")) {
            line1 = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            line2 = "Доступные команды:";
            versionText = "Версия: ";
            restartCmd = "/ar restart [время] - Запускает обратный отсчёт до перезагрузки";
            cancelCmd = "/ar cancel - Отменяет запланированную перезагрузку";
            reloadCmd = "/ar reload - Перезагружает конфигурацию";
            helpCmd = "/ar help - Отображает это меню помощи";
            listCmd = "/ar scheduled - Показывает все запланированные рестарты";
            nowCmd = "/ar now - Немедленно перезапускает сервер";
            disableFeedbackCmd = "/ar disablefeedback - Отключает уведомления для админов";
        } else {
            line1 = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            line2 = "Available Commands:";
            versionText = "Version: ";
            restartCmd = "/ar restart [time] - Starts a countdown to restart";
            cancelCmd = "/ar cancel - Cancels the scheduled restart";
            reloadCmd = "/ar reload - Reloads the configuration";
            helpCmd = "/ar help - Displays this help menu";
            listCmd = "/ar scheduled - Shows all scheduled restarts";
            nowCmd = "/ar now - Immediately restarts the server";
            disableFeedbackCmd = "/ar disablefeedback - Disables admin feedback notifications";
        }

        sender.sendMessage(Component.text(line1).color(TextColor.fromHexString("#555555")));
        sender.sendMessage(Component.text("  ")
                .append(Component.text("R").color(TextColor.fromHexString("#00FFFF")))
                .append(Component.text("e").color(TextColor.fromHexString("#1EE9FF")))
                .append(Component.text("s").color(TextColor.fromHexString("#3DD4FF")))
                .append(Component.text("t").color(TextColor.fromHexString("#5BBFFF")))
                .append(Component.text("a").color(TextColor.fromHexString("#7AAAFF")))
                .append(Component.text("r").color(TextColor.fromHexString("#9994FF")))
                .append(Component.text("t").color(TextColor.fromHexString("#B77FFF")))
                .append(Component.text("A").color(TextColor.fromHexString("#D66AFF")))
                .append(Component.text("R ").color(TextColor.fromHexString("#F455FF")))
                .append(Component.text("| " + versionText).color(TextColor.fromHexString("#AAAAAA")))
                .append(Component.text(currentVersion).color(TextColor.fromHexString(
                        currentVersion.equals(latestVersion) ? "#55FF55" : "#FF5555")))
                .append(Component.text(currentVersion.equals(latestVersion) ? " ✔" : " | Latest: " + latestVersion + " ❌")
                        .color(TextColor.fromHexString(currentVersion.equals(latestVersion) ? "#55FF55" : "#FF5555"))));

        sender.sendMessage(Component.text(" "));
        sender.sendMessage(Component.text("  ").append(Component.text(line2).color(TextColor.fromHexString("#FFFF55"))));
        sender.sendMessage(Component.text(" "));

        sender.sendMessage(Component.text("  " + restartCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + cancelCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + reloadCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + helpCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + listCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + nowCmd).color(TextColor.fromHexString("#AAAAAA")));
        sender.sendMessage(Component.text("  " + disableFeedbackCmd).color(TextColor.fromHexString("#AAAAAA")));

        sender.sendMessage(Component.text(line1).color(TextColor.fromHexString("#555555")));
    }
}
