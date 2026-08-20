package tech.onetap.module.list.combat.rotations;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.util.math.BestPoint;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.player.combat.PredictUtils;
import tech.onetap.util.rotation.MoveFixMode;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;

public class GrimFunRotation extends RotationMode {
    @Override
    public void update(KillAura ka, LivingEntity target) {
        var mc = ka.mc;
        if (mc.player == null || target == null) return;

        Vec3d targetPoint = ka.resolveMultipoint(target,
                target.getBoundingBox().getCenter().add(0, target.getHeight() * 0.12, 0),
                ka.distance.getValue());
        if (target.isGliding() && ka.predictate.getValue() && !ka.isTurnaroundActive) {
            targetPoint = PredictUtils.getPredicted(target, ka.predictValue.getValue());
        } else {
            // A tiny movement lead keeps the ray inside the hitbox on the next server tick.
            Vec3d velocity = target.getVelocity();
            targetPoint = targetPoint.add(velocity.x * 0.20, velocity.y * 0.10, velocity.z * 0.20);
        }

        Rotation aim = new Rotation(RotationUtil.calculate(targetPoint));
        // Grim mode must keep looking at the target during the whole cooldown.
        // The old up/down pitch spoof was the main source of missed hits.
        Rotation rotation = new Rotation(aim.getYaw(), aim.getPitch());
        RotationComponent.update(rotation, 360, 360, 360, 360, 0, 1, false, ka.getMoveFixMode(), "KillAura");
        ka.lastYaw = rotation.getYaw();
        ka.lastPitch = rotation.getPitch();
    }

    @Override
    public void reset(KillAura ka) {
    }
}
