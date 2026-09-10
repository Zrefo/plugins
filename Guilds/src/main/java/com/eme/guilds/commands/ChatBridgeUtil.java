package com.eme.guilds.commands;

import com.eme.guilds.ApiClient;
import com.eme.guilds.Guild;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ChatBridgeUtil {

    /**
     * Wysyła wiadomość do wszystkich online członków gildii w grze
     * i przekazuje ją (asynchronicznie) na Discorda poprzez API.
     */
    public static void broadcastAndRelay(Guild guild, String authorName, String message, ApiClient apiClient) {
        String formatted = ChatColor.LIGHT_PURPLE + "[Gildia] " + ChatColor.WHITE + authorName + ChatColor.GRAY + ": " + ChatColor.RESET + message;

        for (var memberUuid : guild.getMembers()) {
            Player online = Bukkit.getPlayer(memberUuid);
            if (online != null) {
                online.sendMessage(formatted);
            }
        }

        apiClient.relayChatToDiscord(guild.getName(), authorName, message);
    }
}
