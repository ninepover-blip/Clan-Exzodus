package tech.onetap.util.killsay;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Хранит пользовательские сообщения KillSay (фразы убийства и фразы тотема). */
public class KillSayRepository {

    private static final File file = new File(".options/killsay.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private static final List<String> customKillMessages = new ArrayList<>();

    @Getter
    private static final List<String> customTotemMessages = new ArrayList<>();

    public static void addKillMessage(String message) {
        String normalized = normalize(message);
        if (normalized.isEmpty() || customKillMessages.contains(normalized)) return;
        customKillMessages.add(normalized);
        save();
    }

    public static void addTotemMessage(String message) {
        String normalized = normalize(message);
        if (normalized.isEmpty() || customTotemMessages.contains(normalized)) return;
        customTotemMessages.add(normalized);
        save();
    }

    public static void removeKillMessage(String message) {
        if (customKillMessages.remove(message)) save();
    }

    public static void removeTotemMessage(String message) {
        if (customTotemMessages.remove(message)) save();
    }

    public static void editKillMessage(String oldMessage, String newMessage) {
        int index = customKillMessages.indexOf(oldMessage);
        String normalized = normalize(newMessage);
        if (index == -1 || normalized.isEmpty()) return;
        if (customKillMessages.contains(normalized)) return;
        customKillMessages.set(index, normalized);
        save();
    }

    public static void editTotemMessage(String oldMessage, String newMessage) {
        int index = customTotemMessages.indexOf(oldMessage);
        String normalized = normalize(newMessage);
        if (index == -1 || normalized.isEmpty()) return;
        if (customTotemMessages.contains(normalized)) return;
        customTotemMessages.set(index, normalized);
        save();
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                KillSayData data = new KillSayData(customKillMessages, customTotemMessages);
                gson.toJson(data, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public static void load() {
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<KillSayData>() {}.getType();
            KillSayData data = gson.fromJson(reader, type);
            if (data != null) {
                customKillMessages.clear();
                if (data.killMessages != null) customKillMessages.addAll(data.killMessages);
                customTotemMessages.clear();
                if (data.totemMessages != null) customTotemMessages.addAll(data.totemMessages);
            }
        } catch (IOException ignored) {
        }
    }

    private static String normalize(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.length() > 128) {
            message = message.substring(0, 128);
        }
        return message;
    }

    private static class KillSayData {
        List<String> killMessages;
        List<String> totemMessages;

        public KillSayData() {
        }

        public KillSayData(List<String> killMessages, List<String> totemMessages) {
            this.killMessages = killMessages;
            this.totemMessages = totemMessages;
        }
    }
}