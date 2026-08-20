package tech.onetap.util.player.move;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;

@Getter
@Setter
public class StrafeMovement {

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private double oldSpeed;
    private float contextFriction;
    private boolean needSwap;
    private boolean needSprintState;
    private int counter, noSlowTicks;

    public double calculateSpeed(boolean damageBoost, boolean hasTime, boolean autoJump, float damageSpeed) {
        final boolean fromGround = mc.player.isOnGround();
        final boolean jump = mc.player.getVelocity().y > 0;
        final float speedAttributes = getAIMoveSpeed();
        final float frictionFactor = getFrictionFactor();
        float n6 = mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.88f : 0.91F;

        if (fromGround) {
            n6 = frictionFactor;
        }
        final float n7 = 0.16277136f / (n6 * n6 * n6);
        float n8;
        if (fromGround) {
            n8 = speedAttributes * n7;
            if (jump) {
                n8 += 0.2f;
            }
        } else {
            n8 = (damageBoost && hasTime && (autoJump || mc.options.jumpKey.isPressed()) ? damageSpeed : 0.0255f);
        }
        boolean noslow = false;
        double max2 = oldSpeed + n8;
        double max = 0.0;
        if (mc.player.isUsingItem() && !jump) {
            double n10 = oldSpeed + n8 * 0.25;
            double motionY2 = mc.player.getVelocity().y;
            if (motionY2 != 0.0 && Math.abs(motionY2) < 0.08) {
                n10 += 0.055;
            }
            if (max2 > (max = Math.max(0.043, n10))) {
                noslow = true;
                ++noSlowTicks;
            } else {
                noSlowTicks = Math.max(noSlowTicks - 1, 0);
            }
        } else {
            noSlowTicks = 0;
        }
        if (noSlowTicks > 3) {
            max2 = max - (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST) && mc.player.isUsingItem() ? 0.3 : 0.019);
        } else {
            max2 = Math.max(noslow ? 0 : 0.25, max2) - (counter++ % 2 == 0 ? 0.001 : 0.002);
        }
        contextFriction = n6;
        if (!fromGround) {
            needSwap = true;
            needSprintState = !isServerSprinting();
        } else {
            needSprintState = false;
        }
        return max2;
    }

    public void postMove(final double horizontal) {
        oldSpeed = horizontal * contextFriction;
    }

    private float getAIMoveSpeed() {
        boolean prevSprinting = mc.player.isSprinting();
        mc.player.setSprinting(false);
        float speed = mc.player.getMovementSpeed() * 1.3f;
        mc.player.setSprinting(prevSprinting);
        return speed;
    }

    private float getFrictionFactor() {
        BlockPos.Mutable blockpos = new BlockPos.Mutable();
        blockpos.set(mc.player.getX(), mc.player.getBoundingBox().minY - 1.0D, mc.player.getZ());

        return mc.world.getBlockState(blockpos).getBlock().getSlipperiness() * 0.91F;
    }

    private boolean isServerSprinting() {
        return ((tech.onetap.mixin.ClientPlayerEntityAccessor) mc.player).getServerSprintState();
    }
}
