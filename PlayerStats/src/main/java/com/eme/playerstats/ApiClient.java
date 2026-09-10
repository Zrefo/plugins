package com.eme.playerstats;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Klient HTTP do komunikacji z centralnym API (Serwer 1 - bot Discord + API).
 * Wszystkie wywołania są asynchroniczne (CompletableFuture), żeby NIGDY
 * nie blokować głównego wątku serwera Minecraft.
 */
public class ApiClient {

    private static final Logger LOGGER = Logger.getLogger("PlayerStats");

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient http;

    public ApiClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * GET /api/discord/link?uuid=&name=
     * Zwraca JsonObject: {linked: bool, discordId, discordTag} albo {linked:false, url}
     */
    public CompletableFuture<JsonObject> getDiscordLinkStatus(String uuid, String name) {
        String url = baseUrl + "/api/discord/link?uuid=" + enc(uuid) + "&name=" + enc(name);
        return sendGet(url);
    }

    private CompletableFuture<JsonObject> sendGet(String url) {
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
                    LOGGER.warning("Zapytanie do API nieudane (" + resp.statusCode() + "): " + resp.body());
                    return null;
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.WARNING, "Błąd połączenia z API: " + url, ex);
                    return null;
                });
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
