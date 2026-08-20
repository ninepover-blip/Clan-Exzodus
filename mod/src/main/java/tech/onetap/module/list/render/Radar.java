package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@ModuleInformation(moduleName = "Radar", moduleDesc = "Радар игроков", moduleCategory = ModuleCategory.RENDER)
public class Radar extends Module {

    private final Draggable drag = DragManager.installDrag(this, "Radar", 140, 140);

    private final SliderSetting size = new SliderSetting("Размер", 150f, 60f, 300f, 5f);
    private final SliderSetting range = new SliderSetting("Радиус", 64f, 16f, 128f, 4f);
    private final BooleanSetting rotate = new BooleanSetting("Вращение", true);
    private final BooleanSetting background = new BooleanSetting("Фон", true);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 0.35f, 0f, 1f, 0.05f);

    @Subscribe
    public void onEventHUD(EventHUD event) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        float x = drag.getX();
        float y = drag.getY();
        float s = size.getFloatValue();
        float half = s / 2f;
        drag.setWidth(s);
        drag.setHeight(s);

        int bgAlpha = (int) (opacity.getFloatValue() * 255);
        if (background.getValue()) {
            DrawUtil.drawRound(x, y, s, s, 4f, ColorProvider.rgba(12, 12, 22, bgAlpha));
        }

        float centerX = x + half;
        float centerY = y + half;

        float yawRad = 0f;
        if (rotate.getValue()) {
            yawRad = (float) Math.toRadians(mc.player.getYaw());
        }
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double dx = player.getX() - mc.player.getX();
            double dz = player.getZ() - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range.getValue()) continue;

            float max = (float) (half - 4);
            float scale = max / range.getFloatValue();
            float rx = (float) (dx * cos + dz * sin) * scale;
            float rz = (float) (-dx * sin + dz * cos) * scale;

            boolean friend = FriendRepository.isFriend(player.getNameForScoreboard());
            int color = friend ? ColorProvider.rgba(80, 255, 120, 255) : ColorProvider.rgba(255, 80, 80, 255);
            float dot = 3f;
            DrawUtil.drawRound(centerX + rx - dot, centerY - rz - dot, dot * 2, dot * 2, dot, color);
        }

        DrawUtil.drawRound(centerX - 1.5f, centerY - 1.5f, 3, 3, 1.5f, ColorProvider.rgba(255, 255, 255, 255));
    }
}
