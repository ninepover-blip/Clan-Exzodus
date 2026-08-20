package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Target Strafe", moduleCategory = ModuleCategory.MOVEMENT)
public class TargetStrafe extends Module {

    public final BooleanSetting jump = new BooleanSetting("Jump", true);
    public final SliderSetting distance = new SliderSetting("Distance", 3.0f, 0.5f, 7.0f, 0.1f);
    public final SliderSetting speed = new SliderSetting("Speed", 0.3f, 0.05f, 2.0f, 0.05f);

    private boolean switchDir;
    private int jumpTicks;
    private int waterTicks;

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        if (aura == null || !aura.isEnabled() || aura.getTarget() == null) return;

        if (mc.player.isSubmergedInWater()) {
            waterTicks = 10;
        } else {
            waterTicks--;
        }

        if (!canStrafe()) return;

        jumpTicks--;
        LivingEntity target = aura.getTarget();

        if (mc.player.isOnGround() && jump.getValue()) {
            mc.player.jump();
        }

        double speedVal = speed.getValue();
        double dist = distance.getValue();
        double distToTarget = Math.sqrt(mc.player.squaredDistanceTo(target));

        if (distToTarget < 0.001) return;

        double wrap = Math.atan2(mc.player.getZ() - target.getZ(), mc.player.getX() - target.getX());
        double angularOffset = speedVal / dist;
        wrap += switchDir ? angularOffset : -angularOffset;

        double pointX = target.getX() + dist * Math.cos(wrap);
        double pointZ = target.getZ() + dist * Math.sin(wrap);

        if (needToSwitch(pointX, pointZ)) {
            switchDir = !switchDir;
            wrap += 2 * (switchDir ? angularOffset : -angularOffset);
            pointX = target.getX() + dist * Math.cos(wrap);
            pointZ = target.getZ() + dist * Math.sin(wrap);
        }

        double dx = pointX - mc.player.getX();
        double dz = pointZ - mc.player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001) return;
        double motionX = (dx / len) * speedVal;
        double motionZ = (dz / len) * speedVal;

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
    }

    private boolean canStrafe() {
        if (mc.player.isSneaking()) return false;
        if (mc.player.isInLava()) return false;
        if (Onetap.getInstance().getModuleStorage().get(Scaffold.class).isEnabled()) return false;
        if (Onetap.getInstance().getModuleStorage().get(Speed.class).isEnabled()) return false;
        if (mc.player.isSubmergedInWater() || waterTicks > 0) return false;
        return !mc.player.getAbilities().flying;
    }

    private boolean needToSwitch(double x, double z) {
        if (mc.player.horizontalCollision || ((mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed()) && jumpTicks <= 0)) {
            jumpTicks = 10;
            return true;
        }
        for (int i = (int) (mc.player.getY() + 4); i >= 0; --i) {
            BlockPos playerPos = new BlockPos((int) Math.floor(x), (int) Math.floor(i), (int) Math.floor(z));
            if (mc.world.getBlockState(playerPos).getBlock().equals(Blocks.LAVA)
                    || mc.world.getBlockState(playerPos).getBlock().equals(Blocks.FIRE)) {
                return true;
            }
            if (mc.world.isAir(playerPos)) continue;
            return false;
        }
        return false;
    }
}
