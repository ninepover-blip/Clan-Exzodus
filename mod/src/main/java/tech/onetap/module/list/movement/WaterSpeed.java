package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.player.PlayerEntity;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Water Speed", moduleDesc = "Ускорение в воде", moduleCategory = ModuleCategory.MOVEMENT)
public class WaterSpeed extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Polar",
            "NCP", "Matrix", "Grim", "OldGrim", "Polar");

    private final SliderSetting matrixSpeed = new SliderSetting("Matrix Speed", 1.05f, 1.0f, 1.5f, 0.01f)
            .setVisible(() -> mode.is("Matrix"));
    private final SliderSetting grimSpeed = new SliderSetting("Grim Speed", 1.005f, 1.0f, 1.5f, 0.001f)
            .setVisible(() -> mode.is("Grim"));
    private final SliderSetting oldGrimSpeed = new SliderSetting("Old Grim Speed", 1.03f, 1.0f, 1.5f, 0.001f)
            .setVisible(() -> mode.is("OldGrim"));
    private final SliderSetting polarSpeed = new SliderSetting("Polar Speed", 1.05f, 1.0f, 1.5f, 0.01f)
            .setVisible(() -> mode.is("Polar"));
    private final SliderSetting ncpSpeed = new SliderSetting("NCP Speed", 1.15f, 1.0f, 2.0f, 0.01f)
            .setVisible(() -> mode.is("NCP"));

    private float getSpeed() {
        switch (mode.getValue()) {
            case "Matrix":
                return matrixSpeed.getFloatValue();
            case "Grim":
                return grimSpeed.getFloatValue();
            case "OldGrim":
                return oldGrimSpeed.getFloatValue();
            case "Polar":
                return polarSpeed.getFloatValue();
            case "NCP":
            default:
                return ncpSpeed.getFloatValue();
        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || !mc.player.isAlive()) return;
        if (!mc.player.isTouchingWater()) return;

        float speed = getSpeed();
        var velocity = mc.player.getVelocity();
        mc.player.setVelocity(
                velocity.x * speed,
                velocity.y,
                velocity.z * speed
        );
    }
}
