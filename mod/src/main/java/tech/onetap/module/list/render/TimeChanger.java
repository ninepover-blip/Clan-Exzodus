package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "TimeChanger", moduleDesc = "Изменение времени суток", moduleCategory = ModuleCategory.RENDER)
public class TimeChanger extends Module {

    private final BooleanSetting selectMode = new BooleanSetting("Выбрать режим", false);
    private final ModeSetting timeOfDay = new ModeSetting("Время суток", "Ночь", "Ночь", "Утро", "День", "Вечер")
            .setVisible(selectMode::getValue);
    private final SliderSetting time = new SliderSetting("Время", 23000f, 0f, 25000f, 1f)
            .setVisible(() -> !selectMode.getValue());

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getType() == EventPacket.Type.RECEIVE && e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            e.cancelEvent();
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.world == null || mc.player == null) return;
        if (selectMode.getValue()) {
            switch (timeOfDay.getIndex()) {
                case 0 -> setTime(23200L);
                case 1 -> setTime(0L);
                case 2 -> setTime(13320L);
                case 3 -> setTime(15247L);
            }
        } else {
            setTime((long) time.getFloatValue());
        }
    }

    private void setTime(long time) {
        if (mc.world instanceof net.minecraft.client.world.ClientWorld clientWorld) {
            ((net.minecraft.client.world.ClientWorld.Properties) clientWorld.getLevelProperties()).setTimeOfDay(time);
        }
    }
}
