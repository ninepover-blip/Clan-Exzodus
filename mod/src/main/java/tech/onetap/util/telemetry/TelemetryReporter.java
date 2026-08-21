package tech.onetap.util.telemetry;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class TelemetryReporter {
    private static final String ENDPOINT = "https://clan-exzodus.vercel.app/api/telemetry";
    // Для Infinity-мода софт = infinity. У Lobok-мода здесь должно быть "lobok".
    private static final String SOFTWARE = "infinity";
    private static String lastServer;
    private TelemetryReporter() {}

    /** Лёгкая телеметрия: шлём событие ТОЛЬКО при заходе на новый сервер (без постоянного пинга). */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { lastServer = null; return; }
        String server = mc.getCurrentServerEntry() == null ? "Singleplayer" : mc.getCurrentServerEntry().address;
        if (server.equals(lastServer)) return;
        lastServer = server;
        String nickname = mc.player.getNameForScoreboard();
        CompletableFuture.runAsync(() -> send(nickname, server));
    }

    private static void send(String nickname, String server) {
        try {
            Path keyFile = Path.of(".options", "license-key.txt");
            String key = Files.exists(keyFile) ? Files.readString(keyFile).trim() : "";
            if (key.isEmpty()) return;
            JsonObject body = new JsonObject();
            body.addProperty("action", "join");
            body.addProperty("key", key);
            body.addProperty("nickname", nickname);
            body.addProperty("server", server);
            body.addProperty("software", SOFTWARE);
            byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
            HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream output = connection.getOutputStream()) { output.write(data); }
            connection.getResponseCode(); connection.disconnect();
        } catch (Exception ignored) {}
    }
}
