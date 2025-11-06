package me.marioneto4ka.restartar.Listeners;

import me.marioneto4ka.restartar.Notifications.AdminFeedback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinHandler implements Listener {

    private final AdminFeedback feedbackNotifier;

    public PlayerJoinHandler(AdminFeedback feedbackNotifier) {
        this.feedbackNotifier = feedbackNotifier;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(feedbackNotifier.getPlugin(), () -> {
            feedbackNotifier.send(player);
        }, 60L);
    }
}
