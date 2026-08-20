package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@ModuleInformation(moduleName = "LagMeter", moduleDesc = "Отображение пинга", moduleCategory = ModuleCategory.RENDER)
public class LagMeter extends Module {

    private final Draggable drag = DragManager.installDrag(this, "LagMeter", 10, 10);

    private final BooleanSetting background = new BooleanSetting("Фон", true);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 0.35f, 0f, 1f, 0.05f);

    private long ping = 0;
    private int ticks = 0;

    @Subscribe
    public void onEventTick(EventTick event) {
        if (++ticks < 10) return;
        ticks = 0;
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        if (entry != null) {
            ping = entry.getLatency();
        }
    }

    @Subscribe
    public void onEventHUD(EventHUD event) {
        if (!isEnabled()) return;

        float x = drag.getX();
        float y = drag.getY();
        String text = "Пинг: " + ping + " мс";
        float textWidth = Fonts.SFMEDIUM.get().getWidth(text, 7.5f);

        int color = ColorProvider.getThemeColor();
        if (ping < 80) {
            color = ColorProvider.rgba(80, 255, 120, 255);
        } else if (ping < 200) {
            color = ColorProvider.rgba(255, 200, 80, 255);
        } else {
            color = ColorProvider.rgba(255, 80, 80, 255);
        }

        if (background.getValue()) {
            int bgAlpha = (int) (opacity.getFloatValue() * 255);
            DrawUtil.drawRound(x, y, textWidth + 12, 15, 4f, ColorProvider.rgba(12, 12, 22, bgAlpha));
        }
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x + 6f, y + 3.5f, color, 7.5f);

        drag.setWidth(textWidth + 12);
        drag.setHeight(15);
    }
}
