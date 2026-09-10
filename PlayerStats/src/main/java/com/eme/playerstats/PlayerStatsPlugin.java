package com.eme.playerstats;

import com.eme.playerstats.commands.LinkCommand;
import com.eme.playerstats.commands.StatsCommand;
import com.eme.playerstats.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PlayerStatsPlugin extends JavaPlugin {

    private Database database;
    private ApiClient apiClient;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String baseUrl = getConfig().getString("api.base-url", "").trim();
        String apiKey = getConfig().getString("api.api-key", "").trim();

        if (baseUrl.isEmpty() || baseUrl.contains("TWOJ_IP")) {
            getLogger().warning("Nie skonfigurowano api.base-url w config.yml! Komendy /link i /stats (część discordowa) nie będą działać.");
        }

        this.database = new Database(new File(getDataFolder(), "playerstats.db"));
        this.database.init();

        this.apiClient = new ApiClient(baseUrl, apiKey);

        int pollInterval = getConfig().getInt("link.poll-interval-seconds", 3);
        int pollTimeout = getConfig().getInt("link.poll-timeout-seconds", 120);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, database), this);

        LinkCommand linkCommand = new LinkCommand(this, database, apiClient, pollInterval, pollTimeout);
        getCommand("link").setExecutor(linkCommand);

        StatsCommand statsCommand = new StatsCommand(database, apiClient);
        getCommand("stats").setExecutor(statsCommand);

        getLogger().info("PlayerStats włączony.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
    }

    public Database getDatabase() {
        return database;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }
}
