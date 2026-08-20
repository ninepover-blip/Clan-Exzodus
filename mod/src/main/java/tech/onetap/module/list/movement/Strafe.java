package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.AirBlock;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SoulSandBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerSync;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventSprintSync;
import tech.onetap.mixin.ClientPlayerEntityAccessor;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.player.move.MoveUtil;
import tech.onetap.util.player.move.StrafeMovement;

import java.util.Random;

@ModuleInformation(moduleName = "Strafe", moduleDesc = "Быстрое передвижение (Matrix обход)", moduleCategory = ModuleCategory.MOVEMENT)
public class Strafe extends Module {

    private final ModeSetting mode = new ModeSetting("Обход", "Matrix Hard", "Matrix Hard", "Matrix");
    private final BooleanSetting damageBoost = new BooleanSetting("Буст с дамагом", false);
    private final SliderSetting boostSpeed = new SliderSetting("Значение буста", 0.7d, 0.1d, 2.0d, 0.05d)
            .setVisible(() -> damageBoost.getValue());
    private final BooleanSetting autoJump = new BooleanSetting("Прыгать", false);
    private final BooleanSetting moveDir = new BooleanSetting("Направление", true);
    private final SliderSetting speed2Boost = new SliderSetting("Скорость II", 10.0d, 10.0d, 11.0d, 0.01d);
    private final SliderSetting speed3Boost = new SliderSetting("Скорость III", 10.0d, 10.0d, 11.0d, 0.01d);
    private final SliderSetting speed4Boost = new SliderSetting("Скорость IV", 10.0d, 10.0d, 11.0d, 0.01d);

    private final StrafeMovement strafeMovement = new StrafeMovement();
    private final Random random = new Random();

    private boolean normalDamage;
    private boolean explosionDamage;
    private long damageExpireTime;

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.is("Matrix Hard")) {
            if (strafes()) {
                handleStrafesMove();
            } else {
                strafeMovement.setOldSpeed(0);
            }
        } else {
            handleMatrixMove();
        }

        if (autoJump.getValue() && mc.player.isOnGround()) {
            mc.player.jump();
        }
    }

    @Subscribe
    private void onSync(EventPlayerSync e) {
        if (mc.player == null || mc.world == null) return;

        double dx = mc.player.getX() - mc.player.prevX;
        double dz = mc.player.getZ() - mc.player.prevZ;
        strafeMovement.postMove(Math.sqrt(dx * dx + dz * dz));

        if (moveDir.getValue()) {
            mc.player.setHeadYaw(MoveUtil.getdir());
            mc.player.setBodyYaw(mc.player.getYaw());
        }
    }

    @Subscribe
    private void onSprintSync(EventSprintSync e) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("Matrix Hard") || !strafes()) return;

        ClientPlayerEntityAccessor accessor = (ClientPlayerEntityAccessor) mc.player;
        if (mc.player.isOnGround()) {
            if (accessor.getServerSprintState()) {
                sendSprintPacket(false);
                accessor.setServerSprintState(false);
            }
        } else if (strafeMovement.isNeedSwap()) {
            boolean state = !accessor.getServerSprintState();
            sendSprintPacket(state);
            accessor.setServerSprintState(state);
            strafeMovement.setNeedSwap(false);
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getType() != EventPacket.Type.RECEIVE || mc.player == null) return;

        boolean isDamage = explosionDamage;

        if (e.getPacket() instanceof ExplosionS2CPacket) {
            explosionDamage = true;
        }

        if (!isDamage) {
            if (e.getPacket() instanceof EntityStatusS2CPacket statusPacket
                    && statusPacket.getStatus() == 2 && statusPacket.getEntity(mc.world) == mc.player) {
                normalDamage = true;
                damageExpireTime = System.currentTimeMillis() + 700;
            }
        } else if (mc.player.hurtTime > 0) {
            normalDamage = false;
            explosionDamage = false;
        }

        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            strafeMovement.setOldSpeed(0);
        }
    }

    private void handleStrafesMove() {
        if (damageBoost.getValue() && normalDamage && System.currentTimeMillis() >= damageExpireTime) {
            normalDamage = false;
        }

        float damageSpeed = (float) (boostSpeed.getValue() / 10.0);
        boolean hasTime = normalDamage && System.currentTimeMillis() < damageExpireTime;
        double speed = strafeMovement.calculateSpeed(damageBoost.getValue(), hasTime, autoJump.getValue(), damageSpeed);

        MoveUtil.setMotion(speed);
        applySpeedBoost();
    }

    private void handleMatrixMove() {
        if (MoveUtil.hasPlayerMovement() && getMotion() <= 0.289385188 && !mc.player.isOnGround()) {
            double motion = (!reason() && !mc.player.isUsingItem())
                    ? 0.245 - random.nextFloat() * 1.0E-6
                    : getMotion() - 1.0E-5;
            setStrafe(motion);
        }
    }

    private void setMoveMotion(final double motion) {
        float forward = mc.player.input.movementForward;
        float strafe = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward == 0 && strafe == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += forward > 0 ? -45 : 45;
                } else if (strafe < 0) {
                    yaw += forward > 0 ? 45 : -45;
                }
                strafe = 0;
                forward = forward > 0 ? 1 : -1;
            }
            mc.player.setVelocity(
                    forward * motion * MathHelper.cos((float) Math.toRadians(yaw + 90.0f))
                            + strafe * motion * MathHelper.sin((float) Math.toRadians(yaw + 90.0f)),
                    mc.player.getVelocity().y,
                    forward * motion * MathHelper.sin((float) Math.toRadians(yaw + 90.0f))
                            - strafe * motion * MathHelper.cos((float) Math.toRadians(yaw + 90.0f)));
        }
    }

    private void setStrafe(double motion) {
        if (!MoveUtil.hasPlayerMovement()) return;
        double radians = Math.toRadians(MoveUtil.getdir());
        mc.player.setVelocity(-Math.sin(radians) * motion, mc.player.getVelocity().y, Math.cos(radians) * motion);
    }

    private void applySpeedBoost() {
        float multiplier = 1.00F;

        float multiplier2 = (float) speed2Boost.getValue() * 0.1F;
        float multiplier3 = (float) speed3Boost.getValue() * 0.1F;
        float multiplier4 = (float) speed4Boost.getValue() * 0.1F;

        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        if (speedEffect != null && speedEffect.getDuration() > 0) {
            switch (speedEffect.getAmplifier()) {
                case 1 -> multiplier *= multiplier2;
                case 2 -> multiplier *= multiplier3;
                case 3 -> multiplier *= multiplier4;
            }
        }

        if (mc.player.input.movementForward > 0) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x * multiplier, velocity.y, velocity.z * multiplier);
        }
    }

    private double getMotion() {
        Vec3d velocity = mc.player.getVelocity();
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }

    private boolean reason() {
        boolean critWater = mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof FluidBlock
                && mc.world.getBlockState(mc.player.getBlockPos().up()).getBlock() instanceof AirBlock;
        return mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.isClimbing()
                || (mc.player.isTouchingWater() && !critWater) || mc.player.getAbilities().flying;
    }

    private boolean strafes() {
        if (mc.player == null || mc.world == null) {
            return false;
        }
        if (mc.player.isSneaking() || mc.player.isGliding()) {
            return false;
        }
        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            if (mc.options.jumpKey.isPressed() && !mc.player.isSneaking()
                    && !(mc.world.getBlockState(mc.player.getBlockPos().up()).getBlock() instanceof AirBlock)) {
                return false;
            }
        }
        if (mc.world.getBlockState(mc.player.getBlockPos()).getBlock() instanceof CobwebBlock
                || mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.01, mc.player.getZ())).getBlock() instanceof SoulSandBlock) {
            return false;
        }
        return !mc.player.getAbilities().flying && !mc.player.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private void sendSprintPacket(boolean sprinting) {
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player,
                sprinting ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }

    @Override
    public void onEnable() {
        strafeMovement.setOldSpeed(0);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.setVelocity(mc.player.getVelocity().x * 0.7f, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.7f);
        }
        super.onDisable();
    }
}
