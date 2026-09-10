package com.eme.guilds.listeners;

import com.eme.guilds.ApiClient;
import com.eme.guilds.Guild;
import com.eme.guilds.GuildManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Co X sekund odpytuje API o nowe wiadomości Discord -> gra dla każdej
 * gildii i wyświetla je online członkom.
 */
public class ChatBridgeTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final GuildManager guildManager;
    private final ApiClient apiClient;

    public ChatBridgeTask(JavaPlugin plugin, GuildManager guildManager, ApiClient apiClient) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.apiClient = apiClient;
    }

    @Override
    public void run() {
        for (Guild guild : guildManager.all()) {
            // Nie ma sensu odpytywać dla gildii bez nikogo online
            boolean anyoneOnline = guild.getMembers().stream().anyMatch(u -> Bukkit.getPlayer(u) != null);
            if (!anyoneOnline) continue;

            apiClient.pollInbox(guild.getName(), guild.getLastSeenMessageId()).thenAccept(json -> {
                if (json == null) return;
                JsonArray messages = json.getAsJsonArray("messages");
                if (messages == null || messages.isEmpty()) return;

                for (int i = 0; i < messages.size(); i++) {
                    JsonObject msg = messages.get(i).getAsJsonObject();
                    String author = msg.get("author").getAsString();
                    String content = msg.get("content").getAsString();
                    String formatted = ChatColor.BLUE + "[Discord] " + ChatColor.WHITE + author + ChatColor.GRAY + ": " + ChatColor.RESET + content;

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (var memberUuid : guild.getMembers()) {
                            Player online = Bukkit.getPlayer(memberUuid);
                            if (online != null) online.sendMessage(formatted);
                        }
                    });
                }

                if (json.has("lastId")) {
                    guild.setLastSeenMessageId(json.get("lastId").getAsLong());
                }
            });
        }
    }
}
