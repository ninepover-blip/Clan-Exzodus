package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

@ModuleInformation(moduleName = "Reload", moduleDesc = "Перезапускает Speed/Water Speed после флага", moduleCategory = ModuleCategory.MOVEMENT)
public class Reload extends Module {
    private boolean restoreSpeed;
    private boolean restoreWaterSpeed;
    private int waitTicks;

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE || !(event.getPacket() instanceof PlayerPositionLookS2CPacket)) return;
        Speed speed = Onetap.getInstance().getModuleStorage().get(Speed.class);
        WaterSpeed waterSpeed = Onetap.getInstance().getModuleStorage().get(WaterSpeed.class);
        restoreSpeed = speed != null && speed.isEnabled();
        restoreWaterSpeed = waterSpeed != null && waterSpeed.isEnabled();
        if (restoreSpeed) speed.setEnabled(false);
        if (restoreWaterSpeed) waterSpeed.setEnabled(false);
        waitTicks = 2;
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (waitTicks <= 0 || --waitTicks > 0) return;
        if (restoreSpeed) Onetap.getInstance().getModuleStorage().get(Speed.class).setEnabled(true);
        if (restoreWaterSpeed) Onetap.getInstance().getModuleStorage().get(WaterSpeed.class).setEnabled(true);
        restoreSpeed = false;
        restoreWaterSpeed = false;
    }
}
