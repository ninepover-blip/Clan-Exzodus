package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventEntitySpawn;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventPlayerSync;
import tech.onetap.event.list.EventPlayerSyncEnd;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.player.ElytraHelper;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.util.chat.ChatUtil;
import tech.onetap.util.player.move.MoveUtil;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.base.Instance;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ModuleInformation(moduleName = "Таргет Перл", moduleDesc = "Преследует жемчуг последнего атакованного игрока", moduleCategory = ModuleCategory.MISC)
public class TargetPearl extends Module {

    public final BooleanSetting stopMotion = new BooleanSetting("Stop Motion", false).setVisible(() -> false);
    public final BooleanSetting legitStop = new BooleanSetting("Legit Stop", false).setVisible(() -> false);
    public final BooleanSetting pauseAura = new BooleanSetting("Pause Aura", false).setVisible(() -> false);
    public final BooleanSetting onlyOnGround = new BooleanSetting("Only On Ground", false).setVisible(() -> false);
    public final BooleanSetting noMove = new BooleanSetting("No Move", false).setVisible(() -> false);
    public final BooleanSetting onlyTarget = new BooleanSetting("Only Target", true).setVisible(() -> false);
    public final BooleanSetting autoElytraChase = new BooleanSetting("Преследование на элитре", true);

    private Runnable postSyncAction;
    private long lastThrowTime;
    private BlockPos targetBlock;
    private int lastPearlId;
    private int lastOurPearlId;
    private int pendingPearlId;
    private final Map<PlayerEntity, Long> targets = new HashMap<>();
    private final Map<Integer, UUID> pearlOwners = new HashMap<>();
    private boolean elytraChasing;
    private Vec3d elytraTarget;
    private long lastRocketTime;
    private LivingEntity lastCombatTarget;

    @Override
    public void onDisable() {
        postSyncAction = null;
        targetBlock = null;
        lastPearlId = 0;
        lastOurPearlId = 0;
        pendingPearlId = 0;
        targets.clear();
        pearlOwners.clear();
        elytraChasing = false;
        elytraTarget = null;
        lastCombatTarget = null;
        super.onDisable();
    }

    @Subscribe
    private void onAttack(EventAttack event) {
        if (event.getEntity() instanceof PlayerEntity player && player != mc.player) {
            lastCombatTarget = player;
        }
    }

    @Subscribe
    private void onEntitySpawn(EventEntitySpawn e) {
        if (e.getEntity() instanceof EnderPearlEntity pearl) {
            if (pearl.getOwner() instanceof PlayerEntity owner) {
                pearlOwners.put(pearl.getId(), owner.getUuid());
                if (owner.equals(mc.player)) lastOurPearlId = pearl.getId();
                return;
            }
            mc.world.getPlayers().stream()
                    .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(e.getEntity().getPos())))
                    .ifPresent(player -> {
                        if (player.squaredDistanceTo(e.getEntity().getPos()) <= 9.0) {
                            pearlOwners.put(e.getEntity().getId(), player.getUuid());
                            if (player.equals(mc.player)) lastOurPearlId = e.getEntity().getId();
                        }
                    });
        }
    }

    @Subscribe
    private void onSync(EventPlayerSync event) {
        if (handleElytraChase()) return;

        if (System.currentTimeMillis() - lastThrowTime < 250)
            return;

        for (Entity ent : mc.world.getEntities()) {
            if (!(ent instanceof EnderPearlEntity pearl))
                continue;
            if (ent.getId() == lastPearlId || ent.getId() == lastOurPearlId)
                continue;

            UUID trackedOwner = pearlOwners.get(pearl.getId());
            PlayerEntity thrower = trackedOwner == null ? null : mc.world.getPlayerByUuid(trackedOwner);
            if (thrower == null && pearl.getOwner() instanceof PlayerEntity player)
                thrower = player;
            if (thrower == null) {
                thrower = mc.world.getPlayers().stream()
                        .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(ent.getPos())))
                        .orElse(null);
            }

            if (thrower == null || thrower.equals(mc.player)) continue;
            LivingEntity attacked = lastCombatTarget != null && lastCombatTarget.isAlive()
                    ? lastCombatTarget : KillAura.getLastAttackedTarget();
            if (!(attacked instanceof PlayerEntity) || !thrower.getUuid().equals(attacked.getUuid())) continue;

            targetBlock = calcTrajectory(ent);
            if (targetBlock != null) {
                pendingPearlId = ent.getId();
                break;
            }
        }

        if (targetBlock == null)
            return;

        float rotationPitch = (float) (-Math.toDegrees(calcTrajectory(targetBlock)));
        float rotationYaw = (float) Math.toDegrees(Math.atan2(
                targetBlock.getZ() + 0.5f - mc.player.getZ(),
                targetBlock.getX() + 0.5f - mc.player.getX()
        )) - 90.0f;

        BlockPos tracedBP = checkTrajectory(rotationYaw, rotationPitch);

        if (autoElytraChase.getValue()
                && (tracedBP == null || targetBlock.getSquaredDistance(tracedBP.toCenterPos()) > 49.0)
                && startElytraChase(targetBlock.toCenterPos())) {
            lastPearlId = pendingPearlId;
            pendingPearlId = 0;
            targetBlock = null;
            return;
        }

        // Client/server collision rounding may differ by several blocks. The old
        // six-block validation rejected otherwise valid chase throws. Keep the
        // calculated enemy landing point as a fallback.
        if (tracedBP == null || targetBlock.getSquaredDistance(tracedBP.toCenterPos()) > 144)
            tracedBP = targetBlock;

        if (pauseAura.getValue()) {
            KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
            if (aura != null && aura.isEnabled())
                aura.toggle();
        }

        if (onlyOnGround.getValue() && !mc.player.isOnGround()) {
            targetBlock = null;
            return;
        }

        if (noMove.getValue() && MoveUtil.hasPlayerMovement()) {
            targetBlock = null;
            return;
        }

        if (stopMotion.getValue() && MoveUtil.hasPlayerMovement()) {
            if (!legitStop.getValue())
                mc.player.setVelocity(0, 0, 0);
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
            mc.player.input.movementForward = 0;
            mc.player.input.movementSideways = 0;
            return;
        }

        ChatUtil.send("Chasing pearl on X:" + tracedBP.getX() + " Y:" + tracedBP.getY() + " Z:" + tracedBP.getZ());

        mc.player.setYaw(rotationYaw);
        mc.player.setPitch(MathHelper.clamp(rotationPitch, -89, 89));

        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        postSyncAction = () -> {
            int epSlot = findEPSlot();
            int originalSlot = mc.player.getInventory().selectedSlot;
            if (epSlot != -1) {
                mc.player.getInventory().selectedSlot = epSlot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(epSlot));
                mc.interactionManager.sendSequencedPacket(mc.world, sequence ->
                        new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch));
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.player.getInventory().selectedSlot = originalSlot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
                lastPearlId = pendingPearlId;
                pendingPearlId = 0;
                lastThrowTime = System.currentTimeMillis();
            }
        };

        targetBlock = null;
    }

    @Subscribe
    private void onPostSync(EventPlayerSyncEnd event) {
        if (postSyncAction != null) {
            postSyncAction.run();
            postSyncAction = null;
        }
    }

    private int findEPSlot() {
        int epSlot = -1;
        if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL)
            epSlot = mc.player.getInventory().selectedSlot;
        if (epSlot == -1)
            for (int l = 0; l < 9; ++l)
                if (mc.player.getInventory().getStack(l).getItem() == Items.ENDER_PEARL) {
                    epSlot = l;
                    break;
                }
        return epSlot;
    }

    private boolean startElytraChase(Vec3d destination) {
        if (InventoryUtil.findBestElytraSlot() == -1) return false;
        if (InventoryUtil.searchItem(Items.FIREWORK_ROCKET) == -1) return false;

        elytraTarget = destination;
        elytraChasing = true;
        lastRocketTime = 0;
        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            Instance.get(ElytraHelper.class).swap(false);
        }
        return true;
    }

    private boolean handleElytraChase() {
        if (!elytraChasing || elytraTarget == null || mc.player == null) return false;

        if (mc.player.getPos().distanceTo(elytraTarget) <= 7.0) {
            if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                Instance.get(ElytraHelper.class).swap(true);
            }
            elytraChasing = false;
            elytraTarget = null;
            return false;
        }

        if (!mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            Instance.get(ElytraHelper.class).swap(false);
            return true;
        }

        if (mc.player.isOnGround()) {
            mc.player.jump();
            return true;
        }

        if (!mc.player.isGliding()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
                    mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
            return true;
        }

        Vec3d delta = elytraTarget.subtract(mc.player.getEyePos());
        double horizontal = Math.hypot(delta.x, delta.z);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0f;
        float pitch = MathHelper.clamp((float) -Math.toDegrees(Math.atan2(delta.y, horizontal)), -35.0f, 35.0f);
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);

        if (System.currentTimeMillis() - lastRocketTime >= 900) {
            InventoryUtil.swapAndUseWithGuiBypass(Items.FIREWORK_ROCKET);
            lastRocketTime = System.currentTimeMillis();
        }
        return true;
    }

    private float calcTrajectory(BlockPos bp) {
        double a = Math.hypot(bp.getX() + 0.5f - mc.player.getX(), bp.getZ() + 0.5f - mc.player.getZ());
        double y = 6.125 * ((bp.getY() + 1f) - (mc.player.getY() + (double) mc.player.getEyeHeight(mc.player.getPose())));
        y = 0.05000000074505806 * ((0.05000000074505806 * (a * a)) + y);
        y = Math.sqrt(9.37890625 - y);
        double d = 3.0625 - y;
        y = Math.atan2(d * d + y, 0.05000000074505806 * a);
        d = Math.atan2(d, 0.05000000074505806 * a);
        return (float) Math.min(y, d);
    }

    private BlockPos calcTrajectory(Entity e) {
        return traceTrajectory(e.getX(), e.getY(), e.getZ(), e.getVelocity().x, e.getVelocity().y, e.getVelocity().z);
    }

    private BlockPos checkTrajectory(float yaw, float pitch) {
        if (Float.isNaN(pitch))
            return null;
        float yawRad = yaw / 180.0f * 3.1415927f;
        float pitchRad = pitch / 180.0f * 3.1415927f;
        double x = mc.player.getX() - MathHelper.cos(yawRad) * 0.16f;
        double y = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()) - 0.1000000014901161;
        double z = mc.player.getZ() - MathHelper.sin(yawRad) * 0.16f;
        double motionX = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad) * 0.4f;
        double motionY = -MathHelper.sin(pitchRad) * 0.4f;
        double motionZ = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad) * 0.4f;
        final float distance = MathHelper.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;
        motionX *= 1.5f;
        motionY *= 1.5f;
        motionZ *= 1.5f;
        if (!mc.player.isOnGround())
            motionY += mc.player.getVelocity().getY();
        return traceTrajectory(x, y, z, motionX, motionY, motionZ);
    }

    private BlockPos traceTrajectory(double x, double y, double z, double mx, double my, double mz) {
        Vec3d lastPos;
        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3d(x, y, z);
            x += mx;
            y += my;
            z += mz;
            mx *= 0.99;
            my *= 0.99;
            mz *= 0.99;
            my -= 0.03f;
            Vec3d pos = new Vec3d(x, y, z);
            BlockHitResult bhr = mc.world.raycast(new RaycastContext(lastPos, pos,
                    RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK)
                return bhr.getBlockPos();

            if (y <= -65) break;
        }
        return BlockPos.ofFloored(x, y, z);
    }
}
