package tech.onetap.module.list.render.custommodel;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Парсер OptiFine-модели предмета (items/{type}/model.cfg).
 * Формат:
 * {"type":"PlayerItem","texture":"optifine:textures/features/hat_bee.png",
 *  "textureSize":[16,16],
 *  "models":[{"id":"...","type":"ModelBox","attachTo":"head","invertAxis":"yz",
 *             "translate":[2,8,0],"rotate":[0,0,0],
 *             "boxes":[{"textureOffset":[0,0],"coordinates":[-1,0,0,1,4,1]}]}]}
 */
public class PlayerItemParser {

    private static final Gson GSON = new Gson();
    private static final EnumSet<Direction> ALL_DIRECTIONS = EnumSet.allOf(Direction.class);

    public static PlayerItemModel parse(String type, String body) {
        if (body == null || body.isBlank()) return null;
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null) return null;

        JsonArray size = root.has("textureSize") ? root.getAsJsonArray("textureSize") : null;
        int texW = size != null && size.size() > 0 ? size.get(0).getAsInt() : 16;
        int texH = size != null && size.size() > 1 ? size.get(1).getAsInt() : 16;

        String texture = root.has("texture") ? root.get("texture").getAsString() : null;

        PlayerItemModel model = new PlayerItemModel(type, texture);

        JsonArray models = root.has("models") ? root.getAsJsonArray("models") : null;
        if (models == null) return model;

        for (JsonElement element : models) {
            if (!element.isJsonObject()) continue;
            JsonObject obj = element.getAsJsonObject();

            String modelType = obj.has("type") ? obj.get("type").getAsString() : "ModelBox";
            if (!"ModelBox".equalsIgnoreCase(modelType)) continue;

            boolean isHead = "head".equalsIgnoreCase(obj.has("attachTo") ? obj.get("attachTo").getAsString() : "head");

            float tx = getFloat(obj, "translate", 0, 0f);
            float ty = getFloat(obj, "translate", 1, 0f);
            float tz = getFloat(obj, "translate", 2, 0f);
            float rx = getFloat(obj, "rotate", 0, 0f);
            float ry = getFloat(obj, "rotate", 1, 0f);
            float rz = getFloat(obj, "rotate", 2, 0f);

            JsonArray boxes = obj.has("boxes") ? obj.getAsJsonArray("boxes") : null;
            if (boxes == null || boxes.isEmpty()) continue;

            List<ModelPart.Cuboid> cuboids = new ArrayList<>();
            for (JsonElement boxElement : boxes) {
                if (!boxElement.isJsonObject()) continue;
                JsonObject box = boxElement.getAsJsonObject();

                int u = box.has("textureOffset") && box.getAsJsonArray("textureOffset").size() > 0
                        ? box.getAsJsonArray("textureOffset").get(0).getAsInt() : 0;
                int v = box.has("textureOffset") && box.getAsJsonArray("textureOffset").size() > 1
                        ? box.getAsJsonArray("textureOffset").get(1).getAsInt() : 0;

                JsonArray coords = box.has("coordinates") ? box.getAsJsonArray("coordinates") : null;
                if (coords == null || coords.size() < 6) continue;

                float x = coords.get(0).getAsFloat();
                float y = coords.get(1).getAsFloat();
                float z = coords.get(2).getAsFloat();
                float w = coords.get(3).getAsFloat();
                float h = coords.get(4).getAsFloat();
                float d = coords.get(5).getAsFloat();

                cuboids.add(new ModelPart.Cuboid(u, v, x, y, z, w, h, d,
                        0f, 0f, 0f, false, texW, texH, ALL_DIRECTIONS));
            }

            if (cuboids.isEmpty()) continue;

            ModelPart part = new ModelPart(cuboids, Map.of());
            part.setTransform(ModelTransform.of(
                    tx / 16f, ty / 16f, tz / 16f,
                    MathHelper.RADIANS_PER_DEGREE * rx,
                    MathHelper.RADIANS_PER_DEGREE * ry,
                    MathHelper.RADIANS_PER_DEGREE * rz));
            part.setDefaultTransform(part.getTransform());

            if (isHead) {
                model.addHeadItem(part);
            } else {
                model.addBodyItem(part);
            }
        }
        return model;
    }

    private static float getFloat(JsonObject obj, String key, int index, float def) {
        if (obj.has(key)) {
            JsonElement element = obj.get(key);
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (index < array.size()) return array.get(index).getAsFloat();
            } else if (element.isJsonPrimitive()) {
                return element.getAsFloat();
            }
        }
        return def;
    }
}
