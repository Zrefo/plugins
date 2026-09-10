package com.eme.guilds.commands;

import com.eme.guilds.ApiClient;
import com.eme.guilds.Guild;
import com.eme.guilds.GuildManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class GcCommand implements CommandExecutor {

    private final GuildManager guildManager;
    private final ApiClient apiClient;

    public GcCommand(GuildManager guildManager, ApiClient apiClient) {
        this.guildManager = guildManager;
        this.apiClient = apiClient;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Ta komenda jest tylko dla graczy.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Użycie: /gc <wiadomość>");
            return true;
        }

        Guild guild = guildManager.getByMember(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej gildii.");
            return true;
        }

        String message = String.join(" ", args);
        ChatBridgeUtil.broadcastAndRelay(guild, player.getName(), message, apiClient);
        return true;
    }
}
