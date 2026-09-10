package com.eme.playerstats.commands;

import com.eme.playerstats.ApiClient;
import com.eme.playerstats.Database;
import com.eme.playerstats.PlayerStatsPlugin;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LinkCommand implements CommandExecutor {

    private final PlayerStatsPlugin plugin;
    private final Database database;
    private final ApiClient apiClient;
    private final int pollIntervalSeconds;
    private final int pollTimeoutSeconds;

    // Żeby gracz nie mógł odpalić kilku pollujących zadań na raz
    private final Set<UUID> currentlyPolling = new HashSet<>();

    public LinkCommand(PlayerStatsPlugin plugin, Database database, ApiClient apiClient,
                        int pollIntervalSeconds, int pollTimeoutSeconds) {
        this.plugin = plugin;
        this.database = database;
        this.apiClient = apiClient;
        this.pollIntervalSeconds = pollIntervalSeconds;
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (!currentlyPolling.add(uuid)) {
            player.sendMessage(ChatColor.YELLOW + "Sprawdzanie autoryzacji już trwa - kliknij wcześniej wysłany link.");
            return true;
        }

        player.sendMessage(ChatColor.GRAY + "Sprawdzanie statusu połączenia z Discordem...");

        apiClient.getDiscordLinkStatus(uuid.toString(), name).thenAccept(json -> {
            if (json == null) {
                currentlyPolling.remove(uuid);
                sendOnMainThread(player, ChatColor.RED + "Nie udało się połączyć z serwerem API. Spróbuj później.");
                return;
            }

            if (json.get("linked").getAsBoolean()) {
                String discordId = json.get("discordId").getAsString();
                String discordTag = json.get("discordTag").getAsString();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                        database.saveDiscordLink(uuid, discordId, discordTag));
                currentlyPolling.remove(uuid);
                sendOnMainThread(player, ChatColor.GREEN + "Twoje konto jest już połączone z Discordem: " + discordTag);
                return;
            }

            String url = json.get("url").getAsString();
            sendOnMainThread(player, buildLinkComponent(url));
            pollUntilLinked(player, uuid, name, 0);
        });

        return true;
    }

    private void pollUntilLinked(Player player, UUID uuid, String name, int elapsedSeconds) {
        if (elapsedSeconds >= pollTimeoutSeconds) {
            currentlyPolling.remove(uuid);
            sendOnMainThread(player, ChatColor.RED + "Czas na autoryzację minął. Wpisz /link ponownie.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    currentlyPolling.remove(uuid);
                    return;
                }
                apiClient.getDiscordLinkStatus(uuid.toString(), name).thenAccept(json -> {
                    if (json != null && json.has("linked") && json.get("linked").getAsBoolean()) {
                        String discordId = json.get("discordId").getAsString();
                        String discordTag = json.get("discordTag").getAsString();
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                                database.saveDiscordLink(uuid, discordId, discordTag));
                        currentlyPolling.remove(uuid);
                        sendOnMainThread(player, ChatColor.GREEN + "Połączono z Discordem: " + discordTag + "!");
                    } else {
                        pollUntilLinked(player, uuid, name, elapsedSeconds + pollIntervalSeconds);
                    }
                });
            }
        }.runTaskLaterAsynchronously(plugin, pollIntervalSeconds * 20L);
    }

    private void sendOnMainThread(Player player, String message) {
        plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(message));
    }

    private void sendOnMainThread(Player player, ComponentBuilder component) {
        plugin.getServer().getScheduler().runTask(plugin, () -> player.spigot().sendMessage(component.create()));
    }

    private ComponentBuilder buildLinkComponent(String url) {
        return new ComponentBuilder("Kliknij tutaj aby połączyć konto z Discordem")
                .color(ChatColor.AQUA)
                .bold(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
    }
}
