package com.eme.playerstats.listeners;

import com.eme.playerstats.Database;
import com.eme.playerstats.PlayerStatsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final PlayerStatsPlugin plugin;
    private final Database database;
    private final Map<UUID, Long> joinTimestamps = new ConcurrentHashMap<>();

    public PlayerListener(PlayerStatsPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        joinTimestamps.put(uuid, System.currentTimeMillis());

        // Operacja na bazie danych - zawsze asynchronicznie, żeby nie zamrażać serwera
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                database.ensurePlayer(uuid, player.getName())
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Long joinedAt = joinTimestamps.remove(uuid);
        if (joinedAt == null) return;

        long secondsPlayed = (System.currentTimeMillis() - joinedAt) / 1000L;
        if (secondsPlayed <= 0) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                database.addPlaytime(uuid, secondsPlayed)
        );
    }
}
