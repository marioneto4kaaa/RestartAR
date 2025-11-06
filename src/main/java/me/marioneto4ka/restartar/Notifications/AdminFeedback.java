package me.marioneto4ka.restartar.Notifications;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdminFeedback {

    private final FileConfiguration config;
    private final JavaPlugin plugin;


    public AdminFeedback(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public void send(Player player) {
        if (!player.hasPermission("restartar.admin")) return;
        if (!config.getBoolean("admin-feedback-notification", true)) return;

        String lang = config.getString("language", "en").toLowerCase();

        String line1, line2, line3, line4, disableText, disableButton, donateText, donateButton;
        String workLine1, workLine2;

        if (lang.equals("ru")) {
            line1 = "§aПривет, админ! Мы собираем отзывы о RestartAR.";
            line2 = "§eЕсли у тебя есть идеи или предложения, пожалуйста, свяжись с нами:";
            line3 = "§bDiscord: §fmarioneto4ka";
            line4 = "§bСервер: §fhttps://discord.gg/Y2qScHYGXB";
            disableText = "Хочешь отключить это уведомление? ";
            disableButton = "[Да]";
            donateText = "Хочешь поддержать нас?";
            donateButton = "[Пожертвовать]";
            workLine1 = "§6Разработчик плагина ищет работу!";
            workLine2 = "§eЕсли тебе нужна помощь с плагинами для Minecraft или сайтами — пиши в Discord.";
        } else {
            line1 = "§aHi admin! We're collecting feedback for RestartAR.";
            line2 = "§eIf you have any ideas or suggestions, please contact:";
            line3 = "§bDiscord: §fmarioneto4ka";
            line4 = "§bServer: §fhttps://discord.gg/Y2qScHYGXB";
            disableText = "Do you want to disable this notification? ";
            disableButton = "[Yes]";
            donateText = "Want to support us?";
            donateButton = "[Donate]";
            workLine1 = "§6The plugin developer is looking for work!";
            workLine2 = "§eIf you need help with Minecraft plugins or websites — message on Discord.";
        }

        player.sendMessage(line1);
        player.sendMessage(line2);
        player.sendMessage(line3);
        player.sendMessage(line4);

        player.sendMessage(workLine1);
        player.sendMessage(workLine2);

        TextComponent message = new TextComponent(disableText);
        TextComponent yesButton = new TextComponent(disableButton);
        yesButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        yesButton.setBold(true);
        yesButton.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/ar disablefeedback"
        ));
        message.addExtra(yesButton);

        TextComponent donateMessage = new TextComponent("  " + donateText + " ");
        TextComponent donateButtonComponent = new TextComponent(donateButton);
        donateButtonComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        donateButtonComponent.setBold(true);
        donateButtonComponent.setClickEvent(new ClickEvent(
                ClickEvent.Action.OPEN_URL,
                "https://www.paypal.com/donate/?hosted_button_id=KNGRZX82V56MG"
        ));
        donateMessage.addExtra(donateButtonComponent);

        player.spigot().sendMessage(message);
        player.spigot().sendMessage(donateMessage);
    }
}
