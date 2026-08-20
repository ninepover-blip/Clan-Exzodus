package tech.onetap.module.list.render.custommodel;

/**
 * Утилиты для загрузки OptiFine-моделей игроков.
 * Сервер: http://s.optifine.net
 */
public class OptiFineUtil {

    public static final String SERVER_BASE = "http://s.optifine.net";

    public static String getPlayerConfigUrl(String name) {
        return SERVER_BASE + "/users/" + name + ".cfg";
    }

    public static String getItemModelUrl(String type) {
        return SERVER_BASE + "/items/" + type + "/model.cfg";
    }

    public static String getItemTextureUrl(String type, String name) {
        return SERVER_BASE + "/items/" + type + "/users/" + name + ".png";
    }

    /** Из "optifine:textures/features/hat_bee.png" строит прямой URL. */
    public static String resolveTexturePath(String texture) {
        if (texture == null || texture.isBlank()) return null;
        String path = texture;
        int colon = texture.indexOf(':');
        if (colon != -1) path = texture.substring(colon + 1);
        if (path.startsWith("/")) path = path.substring(1);
        return SERVER_BASE + "/" + path;
    }
}
