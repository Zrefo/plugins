package com.eme.playerstats.commands;

import com.eme.playerstats.ApiClient;
import com.eme.playerstats.Database;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StatsCommand implements CommandExecutor {

    private final Database database;
    private final ApiClient apiClient;

    public StatsCommand(Database database, ApiClient apiClient) {
        this.database = database;
        this.apiClient = apiClient;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String targetName = args.length > 0 ? args[0] : sender.getName();

        if (args.length == 0 && !(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("Użycie: /stats <gracz>");
            return true;
        }

        Database.PlayerRecord record = database.getPlayerByName(targetName);
        if (record == null) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza " + targetName + " w bazie statystyk.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Statystyki: " + record.name() + " ===");
        sender.sendMessage(ChatColor.YELLOW + "Łączny czas gry: " + ChatColor.WHITE + formatPlaytime(record.totalPlaytimeSeconds()));

        // Odśwież status Discorda z API (asynchronicznie), z fallbackiem na dane lokalne.
        apiClient.getDiscordLinkStatus(record.uuid().toString(), record.name()).thenAccept(json -> {
            String line;
            if (json != null && json.has("linked") && json.get("linked").getAsBoolean()) {
                line = ChatColor.YELLOW + "Discord: " + ChatColor.GREEN + json.get("discordTag").getAsString();
            } else if (record.isLinked()) {
                line = ChatColor.YELLOW + "Discord: " + ChatColor.GREEN + record.discordTag() + ChatColor.GRAY + " (dane z pamięci podręcznej)";
            } else {
                line = ChatColor.YELLOW + "Discord: " + ChatColor.RED + "niepołączony" + ChatColor.GRAY + " (użyj /link)";
            }
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PlayerStats"), () -> sender.sendMessage(line));
        });

        return true;
    }

    private String formatPlaytime(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
