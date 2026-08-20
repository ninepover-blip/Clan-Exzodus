package tech.onetap.util.friend;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.util.QuickLogger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class FriendRepository implements QuickLogger {

    private static final File file = new File(".options/friends.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    private static final List<Friend> friends = new ArrayList<>();

    public static void addFriend(String name) {
        String normalized = normalize(name);
        if (normalized.isEmpty() || isFriend(normalized)) {
            return;
        }
        friends.add(new Friend(normalized));
        friends.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        save();
    }

    public static boolean add(String rawName) {
        String normalized = normalize(rawName);
        if (normalized.isEmpty() || isFriend(normalized)) {
            return false;
        }
        friends.add(new Friend(normalized));
        friends.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        save();
        return true;
    }

    public static void removeFriend(String name) {
        boolean removed = friends.removeIf(friend -> friend.name().equalsIgnoreCase(name));
        if (removed) {
            save();
        }
    }

    public static boolean remove(String rawName) {
        String name = normalize(rawName);
        boolean removed = friends.removeIf(friend -> friend.name().equalsIgnoreCase(name));
        if (removed) {
            save();
        }
        return removed;
    }

    public static boolean shouldAttack(PlayerEntity player) {
        return !isFriend(player.getNameForScoreboard());
    }

    public static boolean isFriend(String friend) {
        return friends.stream().anyMatch(f -> (f.name().equalsIgnoreCase(friend)));
    }

    /** Проверка по имени с возможным серверным префиксом (клан/донат/ранг). */
    public static boolean isFriendName(String full) {
        if (full == null || full.isEmpty()) return false;
        if (isFriend(full)) return true;
        String lower = full.toLowerCase(Locale.ROOT);
        for (Friend f : friends) {
            if (lower.endsWith(f.name().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static Friend getFriend(String name) {
        return friends.stream()
                .filter(f -> f.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static List<String> getFriendNames() {
        List<String> names = new ArrayList<>();
        for (Friend friend : friends) {
            names.add(friend.name());
        }
        return Collections.unmodifiableList(names);
    }

    public static int clear() {
        int count = friends.size();
        if (count > 0) {
            friends.clear();
            save();
        }
        return count;
    }

    public static void save() {
        try {
            file.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                gson.toJson(friends, writer);
            }
        } catch (IOException e) {
        }
    }

    public static void load() {
        if (!file.exists()) {
            save();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<ArrayList<Friend>>() {}.getType();
            List<Friend> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                friends.clear();
                friends.addAll(loaded);
            }
        } catch (IOException e) {
        }
    }

    private static String normalize(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        String cleaned = name.replaceAll("[^A-Za-z0-9_]", "").toLowerCase(Locale.ROOT);
        return cleaned.isEmpty() ? "" : name;
    }
}
