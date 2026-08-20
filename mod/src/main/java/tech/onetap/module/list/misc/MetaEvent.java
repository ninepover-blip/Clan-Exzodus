package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ProfilelessChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventPacket;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.renderers.DrawUtil;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "Meta Event", moduleCategory = ModuleCategory.MISC)
public class MetaEvent extends Module {

    private final Map<String, Vec3d> waysMap = new LinkedHashMap<>();
    private final BooleanSetting notifyGoldBlock = new BooleanSetting("Gold Block", true);
    private final BooleanSetting notifyAirdrop = new BooleanSetting("Air Drop", true);
    private final BooleanSetting notifyShip = new BooleanSetting("Boat", true);

    @Override
    public void onEnable() {
        super.onEnable();
        waysMap.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        waysMap.clear();
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;

        String chatMessage = null;
        if (event.getPacket() instanceof ChatMessageS2CPacket packet) {
            chatMessage = packet.body().content();
        } else if (event.getPacket() instanceof ProfilelessChatMessageS2CPacket packet) {
            chatMessage = packet.message().getString();
        } else if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            chatMessage = packet.content().getString();
        }
        if (chatMessage == null) return;
        if (chatMessage.contains("Золотой блок » Появился Золотой блок на координатах") && notifyGoldBlock.getValue()) {
            Pattern pattern = Pattern.compile("Золотой блок » Появился Золотой блок на координатах: (-?\\d+\\.\\d+), (-?\\d+\\.\\d+), (-?\\d+\\.\\d+)");
            Matcher matcher = pattern.matcher(chatMessage);
            if (matcher.find()) {
                double x = Double.parseDouble(matcher.group(1));
                double y = Double.parseDouble(matcher.group(2));
                double z = Double.parseDouble(matcher.group(3));
                waysMap.put("З.Блок", new Vec3d(x, y, z));
                mc.getNetworkHandler().sendChatMessage(".gps " + (int) x + " " + (int) z);
                logDirect("Добавил золотой блок на координатах: " + x + " " + y + " " + z);
            } else {
                logDirect("Не удалось найти координаты для золотого блока.");
            }
        }

        if (chatMessage.contains("Обнаружен аирдроп") && notifyAirdrop.getValue()) {
            Pattern pattern = Pattern.compile("AirDrop » Его координаты: X: (-?\\d+), Y: (-?\\d+), Z: (-?\\d+)");
            Matcher matcher = pattern.matcher(chatMessage);
            if (matcher.find()) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                waysMap.put("Аирдроп", new Vec3d(x, y, z));
                mc.getNetworkHandler().sendChatMessage(".gps " + x + " " + z);
                logDirect("Добавил аир дроп на координатах: " + x + " " + y + " " + z);
            } else {
                logDirect("Не удалось найти координаты для аир дропа.");
            }
        }

        if (chatMessage.contains("Сокровища Воздушных Пиратов были найдены") && notifyShip.getValue()) {
            Pattern pattern = Pattern.compile("Сокровища Воздушных Пиратов были найдены! Координаты: (-?\\d+), (-?\\d+), (-?\\d+)");
            Matcher matcher = pattern.matcher(chatMessage);
            if (matcher.find()) {
                int x = Integer.parseInt(matcher.group(1));
                int y = Integer.parseInt(matcher.group(2));
                int z = Integer.parseInt(matcher.group(3));
                waysMap.put("Корабль", new Vec3d(x, y, z));
                mc.getNetworkHandler().sendChatMessage(".gps " + x + " " + z);
                logDirect("Добавил корабль на координатах: " + x + " " + y + " " + z);
            } else {
                logDirect("Не удалось найти координаты для корабля.");
            }
        }

        if (chatMessage.startsWith(".way add")) {
            Pattern pattern = Pattern.compile(".way add (\\w+) (-?\\d+) (-?\\d+) (-?\\d+)");
            Matcher matcher = pattern.matcher(chatMessage);
            if (matcher.find()) {
                String name = matcher.group(1);
                int x = Integer.parseInt(matcher.group(2));
                int y = Integer.parseInt(matcher.group(3));
                int z = Integer.parseInt(matcher.group(4));
                waysMap.put(name, new Vec3d(x, y, z));
                logDirect("Добавил точку с именем " + name + " на координатах: " + x + " " + y + " " + z);
            } else {
                logDirect("Не удалось распознать координаты для .way.");
            }
        }
    }

    @Subscribe
    private void onDisplay(EventHUD e) {
        if (waysMap.isEmpty() || mc.player == null) {
            return;
        }

        int windowWidth = mc.getWindow().getScaledWidth();
        int windowHeight = mc.getWindow().getScaledHeight();

        for (Map.Entry<String, Vec3d> entry : waysMap.entrySet()) {
            String name = entry.getKey();
            Vec3d vec = entry.getValue();

            net.minecraft.client.util.math.Vector2f vec2f = ProjectionUtil.project(vec.x + 0.5, vec.y + 0.5, vec.z + 0.5);

            int distance = (int) mc.player.getPos().distanceTo(vec);

            String text = distance + "M";
            if (name.contains("Корабль")) {
                text = "Корабль " + text;
            }
            if (name.contains("З.Блок")) {
                text = "Золотой блок " + text;
            }
            if (name.contains("Аирдроп")) {
                text = "Аирдроп " + text;
            }

            float textWidth = Fonts.SFREGULAR.get().getWidth(text, 8f);
            float fontHeight = 8f;

            float posX = vec2f.getX() - textWidth / 2;
            float posY = vec2f.getY() - fontHeight / 2;

            float padding = 2;

            if (vec2f.getX() > 0 && vec2f.getY() > 0 && vec2f.getX() < windowWidth && vec2f.getY() < windowHeight) {
                DrawUtil.drawRound(posX - padding, posY - padding, textWidth + padding * 2, fontHeight + padding * 2, 3f, new Color(0, 0, 0, 90).getRGB());
                DrawUtil.drawText(Fonts.SFREGULAR.get(), text, posX, posY, Color.WHITE.getRGB(), 8f);
            }
        }
    }
}
