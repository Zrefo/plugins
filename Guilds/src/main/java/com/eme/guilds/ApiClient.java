package com.eme.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klient HTTP do komunikacji z centralnym API (Serwer 1). Używany TYLKO
 * do mostu czatu z Discordem - cała reszta logiki gildii jest lokalna.
 */
public class ApiClient {

    private static final Logger LOGGER = Logger.getLogger("Guilds");

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    /** POST /api/guild/callback  {type:"sync", name, ownerUuid, members[]} */
    public void syncGuild(String name, String ownerUuid, Collection<UUID> members) {
        JsonArray membersArray = new JsonArray();
        for (UUID m : members) membersArray.add(m.toString());

        JsonObject body = new JsonObject();
        body.addProperty("type", "sync");
        body.addProperty("name", name);
        body.addProperty("ownerUuid", ownerUuid);
        body.add("members", membersArray);

        sendPost("/api/guild/callback", body.toString());
    }

    /** POST /api/guild/callback  {type:"chat", name, author, content} */
    public void relayChatToDiscord(String guildName, String author, String content) {
        JsonObject body = new JsonObject();
        body.addProperty("type", "chat");
        body.addProperty("name", guildName);
        body.addProperty("author", author);
        body.addProperty("content", content);

        sendPost("/api/guild/callback", body.toString());
    }

    /** GET /api/guild/inbox?guild=&sinceId=  -> nowe wiadomości Discord -> gra */
    public CompletableFuture<JsonObject> pollInbox(String guildName, long sinceId) {
        String url = baseUrl + "/api/guild/inbox?guild=" + enc(guildName) + "&sinceId=" + sinceId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return JsonParser.parseString(resp.body()).getAsJsonObject();
                    }
                    LOGGER.warning("Inbox request failed (" + resp.statusCode() + "): " + resp.body());
                    return null;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Błąd połączenia z API (inbox)", ex);
                    return null;
                });
    }

    private void sendPost(String path, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("X-API-Key", apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Błąd połączenia z API (" + path + ")", ex);
                    return null;
                });
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
