package com.eme.playerstats;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lokalna baza SQLite pluginu. Trzyma nick, łączny czas gry oraz (jeśli jest)
 * powiązany Discord ID/tag każdego gracza który kiedykolwiek wszedł na serwer.
 */
public class Database {

    private final File file;
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger("PlayerStats");

    public Database(File file) {
        this.file = file;
    }

    public void init() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        total_playtime_seconds INTEGER NOT NULL DEFAULT 0,
                        discord_id TEXT,
                        discord_tag TEXT
                    )
                """);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Nie udało się zainicjować bazy danych", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd zamykania bazy danych", e);
        }
    }

    public synchronized void ensurePlayer(UUID uuid, String name) {
        String sql = """
            INSERT INTO players (uuid, name) VALUES (?, ?)
            ON CONFLICT(uuid) DO UPDATE SET name = excluded.name
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd ensurePlayer", e);
        }
    }

    public synchronized void addPlaytime(UUID uuid, long additionalSeconds) {
        String sql = "UPDATE players SET total_playtime_seconds = total_playtime_seconds + ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, additionalSeconds);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd addPlaytime", e);
        }
    }

    public synchronized void saveDiscordLink(UUID uuid, String discordId, String discordTag) {
        String sql = "UPDATE players SET discord_id = ?, discord_tag = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, discordId);
            ps.setString(2, discordTag);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd saveDiscordLink", e);
        }
    }

    public synchronized PlayerRecord getPlayer(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PlayerRecord(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getLong("total_playtime_seconds"),
                        rs.getString("discord_id"),
                        rs.getString("discord_tag")
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd getPlayer", e);
            return null;
        }
    }

    public synchronized PlayerRecord getPlayerByName(String name) {
        String sql = "SELECT * FROM players WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new PlayerRecord(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getLong("total_playtime_seconds"),
                        rs.getString("discord_id"),
                        rs.getString("discord_tag")
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd getPlayerByName", e);
            return null;
        }
    }

    public record PlayerRecord(UUID uuid, String name, long totalPlaytimeSeconds, String discordId, String discordTag) {
        public boolean isLinked() {
            return discordId != null && !discordId.isEmpty();
        }
    }
}
