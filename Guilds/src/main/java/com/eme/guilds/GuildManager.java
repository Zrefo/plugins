package com.eme.guilds;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GuildManager {

    // Nazwa gildii (lowercase) -> gildia. Trzymana w pamięci dla szybkiego dostępu,
    // odzwierciedlona w lokalnej bazie SQLite.
    private final Map<String, Guild> guildsByName = new ConcurrentHashMap<>();
    private final Map<UUID, String> guildByMember = new ConcurrentHashMap<>();

    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9]{1,5}$");

    private final JavaPlugin plugin;
    private final Database database;
    private final ApiClient apiClient;

    public GuildManager(JavaPlugin plugin, Database database, ApiClient apiClient) {
        this.plugin = plugin;
        this.database = database;
        this.apiClient = apiClient;
    }

    public void loadFromDatabase() {
        for (Guild guild : database.loadAll()) {
            guildsByName.put(key(guild.getName()), guild);
            for (UUID member : guild.getMembers()) {
                guildByMember.put(member, key(guild.getName()));
            }
        }
    }

    public static boolean isValidName(String name) {
        return VALID_NAME.matcher(name).matches();
    }

    public boolean existsByName(String name) {
        return guildsByName.containsKey(key(name));
    }

    public Guild getByName(String name) {
        return guildsByName.get(key(name));
    }

    public Guild getByMember(UUID uuid) {
        String guildKey = guildByMember.get(uuid);
        return guildKey == null ? null : guildsByName.get(guildKey);
    }

    public Collection<Guild> all() {
        return guildsByName.values();
    }

    /**
     * Tworzy nową gildię i asynchronicznie zapisuje ją lokalnie oraz synchronizuje z API
     * (żeby bot Discord wiedział kto jest właścicielem - potrzebne do /guild chat-setup).
     */
    public Guild createGuild(String name, UUID owner) {
        Guild guild = new Guild(name, owner);
        guildsByName.put(key(name), guild);
        guildByMember.put(owner, key(name));
        persistAndSync(guild);
        return guild;
    }

    public void addMember(Guild guild, UUID member) {
        guild.getMembers().add(member);
        guildByMember.put(member, key(guild.getName()));
        persistAndSync(guild);
    }

    private void persistAndSync(Guild guild) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            database.saveGuild(guild);
            apiClient.syncGuild(guild.getName(), guild.getOwnerUuid().toString(), guild.getMembers());
        });
    }

    private static String key(String name) {
        return name.toLowerCase();
    }
}
