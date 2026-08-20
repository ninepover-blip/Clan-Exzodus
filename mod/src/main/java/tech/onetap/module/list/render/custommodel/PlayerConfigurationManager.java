package tech.onetap.module.list.render.custommodel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Загрузка OptiFine-конфигов игроков и моделей предметов с s.optifine.net.
 * Формат: users/{ник}.cfg -> {"items":[{"active":"true","type":"hat_bee"}]}
 */
public class PlayerConfigurationManager {

    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "OptiFine-Download");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, PlayerConfiguration> configCache = new ConcurrentHashMap<>();
    private static final Map<String, PlayerItemModel> modelCache = new ConcurrentHashMap<>();
    private static final Map<String, Identifier> textureCache = new ConcurrentHashMap<>();

    public static PlayerConfiguration getConfig(String name) {
        if (name == null || name.isBlank()) return null;
        PlayerConfiguration config = configCache.get(name);
        if (config != null) return config;

        // Блокируем повторные запросы: кладём пустую заглушку сразу.
        configCache.putIfAbsent(name, PlayerConfiguration.EMPTY);
        downloadConfig(name);
        return null;
    }

    private static void downloadConfig(String name) {
        EXECUTOR.submit(() -> {
            try {
                String body = fetch(OptiFineUtil.getPlayerConfigUrl(name));
                PlayerConfiguration config = parseConfig(body, name);
                configCache.put(name, config);
                for (String type : config.getActiveTypes()) {
                    loadItem(type, name);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static PlayerConfiguration parseConfig(String body, String name) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null) return PlayerConfiguration.EMPTY;
        JsonArray items = root.has("items") ? root.getAsJsonArray("items") : null;
        if (items == null) return PlayerConfiguration.EMPTY;

        List<PlayerItemConfig> list = new ArrayList<>();
        for (JsonElement element : items) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : null;
            boolean active = !obj.has("active") || obj.get("active").getAsBoolean();
            if (type != null && active) list.add(new PlayerItemConfig(type, name));
        }
        return new PlayerConfiguration(list);
    }

    private static void loadItem(String type, String name) {
        if (modelCache.containsKey(type)) return;
        try {
            String body = fetch(OptiFineUtil.getItemModelUrl(type));
            PlayerItemModel model = PlayerItemParser.parse(type, body);
            if (model == null) return;
            modelCache.put(type, model);

            Identifier textureId = textureCache.get(type);
            if (textureId == null) {
                String textureUrl = OptiFineUtil.getItemTextureUrl(type, name);
                byte[] bytes = downloadBytes(textureUrl);
                if (bytes == null && model.getTexturePath() != null) {
                    bytes = downloadBytes(OptiFineUtil.resolveTexturePath(model.getTexturePath()));
                }
                if (bytes == null) return;
                byte[] finalBytes = bytes;
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(finalBytes));
                        Identifier id = Identifier.of("optifine", "playeritems/" + type.replace('/', '_'));
                        MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
                        textureCache.put(type, id);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    public static PlayerItemModel getModel(String type) {
        return modelCache.get(type);
    }

    public static Identifier getTexture(String type) {
        return textureCache.get(type);
    }

    private static String fetch(String url) throws Exception {
        byte[] bytes = downloadBytes(url);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] downloadBytes(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        int code = connection.getResponseCode();
        if (code != 200) return null;
        InputStream in = connection.getInputStream();
        byte[] bytes = in.readAllBytes();
        in.close();
        connection.disconnect();
        return bytes;
    }

    public record PlayerItemConfig(String type, String name) {}
}
