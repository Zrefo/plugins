package com.eme.guilds.commands;

import com.eme.guilds.ApiClient;
import com.eme.guilds.Guild;
import com.eme.guilds.GuildManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GuildCommand implements CommandExecutor {

    private final GuildManager guildManager;
    private final ApiClient apiClient;

    public GuildCommand(GuildManager guildManager, ApiClient apiClient) {
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
            player.sendMessage(ChatColor.YELLOW + "Użycie: /guild create <nazwa> | /guild invite <gracz> | /guild chat <wiadomość>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "chat" -> handleChat(player, args);
            default -> player.sendMessage(ChatColor.RED + "Nieznana podkomenda. Użyj create, invite albo chat.");
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /guild create <nazwa>");
            return;
        }
        String name = args[1];

        if (guildManager.getByMember(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Jesteś już w gildii.");
            return;
        }
        if (!GuildManager.isValidName(name)) {
            player.sendMessage(ChatColor.RED + "Nazwa gildii może mieć maksymalnie 5 znaków i być tylko literami/cyframi (bez polskich znaków i spacji).");
            return;
        }
        if (guildManager.existsByName(name)) {
            player.sendMessage(ChatColor.RED + "Gildia o tej nazwie już istnieje.");
            return;
        }

        guildManager.createGuild(name, player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Założono gildię " + ChatColor.WHITE + name + ChatColor.GREEN + "!");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /guild invite <gracz>");
            return;
        }

        Guild guild = guildManager.getByMember(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej gildii.");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Gracz " + args[1] + " nie jest online.");
            return;
        }
        if (guildManager.getByMember(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + target.getName() + " jest już w jakiejś gildii.");
            return;
        }

        guildManager.addMember(guild, target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Dodano " + target.getName() + " do gildii " + guild.getName() + ".");
        target.sendMessage(ChatColor.GREEN + "Zostałeś dodany do gildii " + guild.getName() + " przez " + player.getName() + "!");
    }

    private void handleChat(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Użycie: /guild chat <wiadomość>");
            return;
        }
        Guild guild = guildManager.getByMember(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(ChatColor.RED + "Nie jesteś w żadnej gildii.");
            return;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        ChatBridgeUtil.broadcastAndRelay(guild, player.getName(), message, apiClient);
    }
}
