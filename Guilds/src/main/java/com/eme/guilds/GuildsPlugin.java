package com.eme.guilds;

import com.eme.guilds.commands.GcCommand;
import com.eme.guilds.commands.GuildCommand;
import com.eme.guilds.listeners.ChatBridgeTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class GuildsPlugin extends JavaPlugin {

    private Database database;
    private GuildManager guildManager;
    private ApiClient apiClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String baseUrl = getConfig().getString("api.base-url", "").trim();
        String apiKey = getConfig().getString("api.api-key", "").trim();

        if (baseUrl.isEmpty() || baseUrl.contains("TWOJ_IP")) {
            getLogger().warning("Nie skonfigurowano api.base-url w config.yml! Most czatu z Discordem nie będzie działać (reszta gildii działa lokalnie).");
        }

        this.database = new Database(new File(getDataFolder(), "guilds.db"));
        this.database.init();

        this.apiClient = new ApiClient(baseUrl, apiKey);
        this.guildManager = new GuildManager(this, database, apiClient);
        this.guildManager.loadFromDatabase();

        getCommand("guild").setExecutor(new GuildCommand(guildManager, apiClient));
        getCommand("gc").setExecutor(new GcCommand(guildManager, apiClient));

        int pollInterval = getConfig().getInt("bridge.poll-interval-seconds", 3);
        new ChatBridgeTask(this, guildManager, apiClient)
                .runTaskTimerAsynchronously(this, 20L * pollInterval, 20L * pollInterval);

        getLogger().info("Guilds włączony. Wczytano " + guildManager.all().size() + " gildii.");
    }

    @Override
    public void onDisable() {
        if (database != null) database.close();
    }
}
