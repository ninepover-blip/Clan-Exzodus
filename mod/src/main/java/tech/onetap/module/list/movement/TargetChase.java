package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import tech.onetap.Onetap;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.rotation.RotationComponent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "Target Chase", moduleDesc = "Vanilla movement straight toward the KillAura target", moduleCategory = ModuleCategory.MOVEMENT)
public class TargetChase extends Module {
    private final SliderSetting stopDistance = new SliderSetting("Stop Distance", 0.35, 0.1, 2.0, 0.05);
    private final SliderSetting leadDistance = new SliderSetting("Lead Distance", 0.5, 0.1, 1.5, 0.1);
    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", true);

    @Subscribe
    private void onMoveInput(MoveInputEvent event) {
        if (mc.player == null || mc.world == null || mc.player.isSneaking()) return;

        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        if (aura == null || !aura.isEnabled() || aura.getTarget() == null) return;
        Vec3d facing = aura.getTarget().getRotationVector().multiply(1, 0, 1);
        if (facing.lengthSquared() < 1.0e-4) facing = new Vec3d(0, 0, 1);
        facing = facing.normalize().multiply(leadDistance.getValue());
        Vec3d holdPoint = aura.getTarget().getPos().add(facing);

        double dx = holdPoint.x - mc.player.getX();
        double dz = holdPoint.z - mc.player.getZ();
        if (dx * dx + dz * dz <= stopDistance.getValue() * stopDistance.getValue()) return;

        // Only vanilla input is changed. No velocity, timer or movement packets.
        event.forward = 1.0f;
        event.strafe = 0.0f;
        float desiredYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        RotationComponent.fixMovement(event, MathHelper.wrapDegrees(desiredYaw), mc.player.getYaw());
        if (autoJump.getValue() && mc.player.horizontalCollision && mc.player.isOnGround()) {
            event.jump = true;
        }
    }
}
