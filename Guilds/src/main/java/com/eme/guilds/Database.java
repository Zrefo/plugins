package com.eme.guilds;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lokalna baza SQLite pluginu Guilds. Cała logika gildii (tworzenie, zapraszanie,
 * czat) jest w pełni lokalna dla serwera Minecraft - nie wymaga API.
 * API jest potrzebne TYLKO do mostu czatu z Discordem.
 */
public class Database {

    private final File file;
    private Connection connection;
    private static final Logger LOGGER = Logger.getLogger("Guilds");

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
                    CREATE TABLE IF NOT EXISTS guilds (
                        name TEXT PRIMARY KEY,
                        owner_uuid TEXT NOT NULL
                    )
                """);
                st.execute("""
                    CREATE TABLE IF NOT EXISTS guild_members (
                        guild_name TEXT NOT NULL,
                        member_uuid TEXT NOT NULL,
                        PRIMARY KEY (guild_name, member_uuid)
                    )
                """);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Nie udało się zainicjować bazy danych", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd zamykania bazy danych", e);
        }
    }

    public synchronized void saveGuild(Guild guild) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO guilds (name, owner_uuid) VALUES (?, ?) " +
                "ON CONFLICT(name) DO UPDATE SET owner_uuid = excluded.owner_uuid")) {
            ps.setString(1, guild.getName());
            ps.setString(2, guild.getOwnerUuid().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd saveGuild", e);
        }

        try (PreparedStatement del = connection.prepareStatement("DELETE FROM guild_members WHERE guild_name = ?")) {
            del.setString(1, guild.getName());
            del.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd czyszczenia członków", e);
        }

        try (PreparedStatement ins = connection.prepareStatement(
                "INSERT INTO guild_members (guild_name, member_uuid) VALUES (?, ?)")) {
            for (UUID member : guild.getMembers()) {
                ins.setString(1, guild.getName());
                ins.setString(2, member.toString());
                ins.addBatch();
            }
            ins.executeBatch();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd zapisu członków", e);
        }
    }

    public synchronized List<Guild> loadAll() {
        List<Guild> result = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT name, owner_uuid FROM guilds")) {
            while (rs.next()) {
                String name = rs.getString("name");
                UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                Guild guild = new Guild(name, owner);
                loadMembersInto(guild);
                result.add(guild);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Błąd loadAll", e);
        }
        return result;
    }

    private void loadMembersInto(Guild guild) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT member_uuid FROM guild_members WHERE guild_name = ?")) {
            ps.setString(1, guild.getName());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guild.getMembers().add(UUID.fromString(rs.getString("member_uuid")));
                }
            }
        }
    }
}
