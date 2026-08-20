package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import org.lwjgl.glfw.GLFW;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.Onetap;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ActionBindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.player.move.MoveUtil;
import tech.onetap.util.player.combat.HvhTargetPredict;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@ModuleInformation(moduleName = "Speed", moduleCategory = ModuleCategory.MOVEMENT)
public class Speed extends Module {

    private final ModeSetting mode = new ModeSetting("Мод", "Recode", "Recode", "Oblivion", "Grim", "GrimEntity", "Polar", "Collision", "Meta");
    private final SliderSetting grimEntityRange = new SliderSetting("GrimEntity Radius", 2.1d, 0.5d, 4.0d, 0.1d)
            .setVisible(() -> mode.is("GrimEntity"));
    private final SliderSetting grimEntityBoost = new SliderSetting("GrimEntity Boost", 1.09d, 1.0d, 1.3d, 0.01d)
            .setVisible(() -> mode.is("GrimEntity"));
    private final SliderSetting polarSpeed = new SliderSetting("Polar Speed", 0.36d, 0.2d, 0.8d, 0.01d)
            .setVisible(() -> mode.is("Polar"));
    private final BooleanSetting polarJump = new BooleanSetting("Polar Auto Jump", true)
            .setVisible(() -> mode.is("Polar"));
    private final BooleanSetting hvhTarget = new BooleanSetting("HvH Target", false);
    private final SliderSetting hvhPredictStrength = new SliderSetting("Сила предикта", 4.0d, 0.5d, 20.0d, 0.1d)
            .setVisible(hvhTarget::getValue);
    private final BooleanSetting leave = new BooleanSetting("Leave", false).setVisible(hvhTarget::getValue);
    private final SliderSetting leaveDistance = new SliderSetting("Дистанция отхода", 8.0d, 0.5d, 20.0d, 0.5d)
            .setVisible(() -> hvhTarget.getValue() && leave.getValue());
    private final SliderSetting attackDistance = new SliderSetting("Радиус удара", 4.0d, 0.5d, 6.0d, 0.1d)
            .setVisible(() -> hvhTarget.getValue() && leave.getValue());
    private LivingEntity lastHvhTarget;

    // ===== Recode =====
    private final ModeSetting recodeMode = new ModeSetting("Режим", "Авто", "Авто", "Простой").setVisible(() -> mode.is("Recode"));
    private final SliderSetting speed = new SliderSetting("Скорость", 0.36d, 0.2d, 1.05d, 0.01d)
            .setVisible(() -> mode.is("Recode") && recodeMode.is("Простой"));

    // ===== Общее =====
    private final ActionBindSetting boostKey = new ActionBindSetting("Кнопка Буста");

    private long boostEndTime;
    private boolean isBoosting;
    private boolean boostPressed;
    private boolean wasInLiquid;
    private long liquidExitTime;

    private static final String[] BOOSTER_BALLS = {
            "Шар Геракла 2", "Шар CHAMPION", "Шар Аида 2", "Шар GOD", "КУБИК-РУБИК", "Шар BUNNY"
    };

    private static boolean isBoosterBall(String itemName) {
        for (String ball : BOOSTER_BALLS) {
            if (itemName.contains(ball)) {
                return true;
            }
        }
        return false;
    }

    @Subscribe
    private void onKey(EventKeyInput e) {
        if (e.getAction() == 1 && boostKey.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN && e.getKey() == boostKey.getKeyCode()) {
            boostPressed = true;
        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null) return;

        if (boostPressed) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900;
            boostPressed = false;
        }

        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }

        boolean inLiquid = mc.player.isTouchingWater() || mc.player.isInLava()
                || mc.player.isSubmergedIn(FluidTags.WATER) || mc.player.isSubmergedIn(FluidTags.LAVA);

        if (mode.is("Oblivion")) {
            if (inLiquid) {
                wasInLiquid = true;
                return;
            }
            if (wasInLiquid) {
                wasInLiquid = false;
                liquidExitTime = System.currentTimeMillis();
            }
            if (System.currentTimeMillis() - liquidExitTime < 250) return;
        } else {
            if (inLiquid) {
                if (!wasInLiquid) {
                    wasInLiquid = true;
                }
                return;
            }
            wasInLiquid = false;
        }

        if (mc.player.getAbilities().flying || mc.player.getAbilities().creativeMode || mc.player.isGliding()) {
            return;
        }

        float appliedSpeed = switch (mode.getValue()) {
            case "Oblivion" -> oblivionSpeed();
            case "GrimEntity" -> grimEntitySpeed();
            case "Polar" -> getPolarSpeed();
            default -> recodeSpeed();
        };
        MoveUtil.setMotion(appliedSpeed);
        if (hvhTarget.getValue()) handleHvhTarget();
    }

    private float grimEntitySpeed() {
        int nearby = 0;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && mc.player.distanceTo(player) <= grimEntityRange.getValue()) nearby++;
        }
        float base = recodeSpeed();
        return nearby > 0 ? (float) (base * Math.pow(grimEntityBoost.getValue(), Math.min(nearby, 3))) : base;
    }

    private float getPolarSpeed() {
        if (polarJump.getValue() && MoveUtil.hasPlayerMovement() && mc.player.isOnGround()) mc.player.jump();
        float value = polarSpeed.getFloatValue();
        if (!mc.player.isOnGround()) value *= 1.08f;
        return value;
    }

    private void handleHvhTarget() {
        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        if (aura == null || !aura.isEnabled() || aura.getTarget() == null) return;
        LivingEntity target = aura.getTarget();
        if (lastHvhTarget != null && lastHvhTarget != target) HvhTargetPredict.reset(lastHvhTarget);
        lastHvhTarget = target;

        Vec3d velocity = mc.player.getVelocity();
        double speed = Math.hypot(velocity.x, velocity.z);
        if (speed < 0.01) speed = 0.2873;
        Vec3d predicted = HvhTargetPredict.predict(target, hvhPredictStrength.getValue());
        double dx = predicted.x - mc.player.getX();
        double dz = predicted.z - mc.player.getZ();
        double distance = Math.hypot(dx, dz);

        if (leave.getValue() && aura.ticksToAttack > 0 && distance < leaveDistance.getValue()) {
            dx = mc.player.getX() - predicted.x;
            dz = mc.player.getZ() - predicted.z;
        } else if (leave.getValue() && aura.ticksToAttack <= 0 && distance <= attackDistance.getValue()) {
            return;
        } else if (distance <= 0.6) {
            mc.player.setVelocity(0, velocity.y, 0);
            return;
        }

        double length = Math.hypot(dx, dz);
        if (length > 1.0E-6) mc.player.setVelocity(dx / length * speed, velocity.y, dz / length * speed);
    }

    private float recodeSpeed() {
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance deEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = mc.player.getOffHandStack().getName().getString();
        ItemStack bootsItem = mc.player.getEquippedStack(EquipmentSlot.FEET);
        ItemStack pantsItem = mc.player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack chestItem = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack headItem = mc.player.getEquippedStack(EquipmentSlot.HEAD);
        String bootsItemName = bootsItem.getName().getString();
        String pantsItemName = pantsItem.getName().getString();
        String chestItemName = chestItem.getName().getString();
        String headItemName = headItem.getName().getString();

        float appliedSpeed;
        if (recodeMode.is("Простой")) {
            appliedSpeed = (float) speed.getValue();
        } else {
            appliedSpeed = 0;
            if (speedEffect != null) {
                if (speedEffect.getAmplifier() == 2) {
                    appliedSpeed = isBoosterBall(itemName) ? s20 * 1.1555F : s0 * 1.1555F;
                } else if (speedEffect.getAmplifier() == 1) {
                    appliedSpeed = isBoosterBall(itemName) ? s20 : s0;
                }
            } else {
                appliedSpeed = isBoosterBall(itemName) ? s20 * 0.68F : s0 * 0.68F;
            }
        }

        if (deEffect != null) {
            appliedSpeed *= 0.835f;
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= 1.435F;
        }

        if (bootsItem.getItem() == Items.GOLDEN_BOOTS && bootsItemName.contains("Тапочки админа SoveryBRIZ")) {
            appliedSpeed *= 1.01F;
        }

        if (pantsItem.getItem() == Items.GOLDEN_LEGGINGS && pantsItemName.contains("Штаны админа stqffy")) {
            appliedSpeed *= 1.02F;
        }

        if (headItem.getItem() == Items.GOLDEN_HELMET && headItemName.contains("Шляпа админа Vester")) {
            appliedSpeed *= 1.05F;
        }

        if (headItem.getItem() == Items.PLAYER_HEAD && headItemName.contains("Новогодний Подарок")) {
            appliedSpeed *= 0.75F;
        }

        if (isBoosting) {
            appliedSpeed *= 1.12F;
        }

        if (chestItem.getItem() == Items.GOLDEN_CHESTPLATE && chestItemName.contains("Грудак админа lxckscream")) {
            appliedSpeed *= 1.03F;
        }

        return appliedSpeed;
    }

    private float oblivionSpeed() {
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance deEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        String itemName = mc.player.getOffHandStack().getName().getString();

        float appliedSpeed = s0o;
        if (speedEffect != null) {
            if (speedEffect.getAmplifier() == 2) {
                appliedSpeed = isBoosterBall(itemName) ? s20o * 1.63f : s0o * 1.62f;
            } else if (speedEffect.getAmplifier() == 1) {
                appliedSpeed = isBoosterBall(itemName) ? s20o * 1.423f : s0o * 1.423f;
            }
        } else {
            if (isBoosterBall(itemName)) {
                appliedSpeed = s20o;
            }
        }

        if (deEffect != null) {
            appliedSpeed *= 0.835f;
        }

        if (!mc.player.isOnGround()) {
            appliedSpeed *= 1.435F;
        }

        if (isBoosting) {
            appliedSpeed *= 1.12F;
        }

        return appliedSpeed;
    }

    @Override
    public void onDisable() {
        isBoosting = false;
        boostPressed = false;
        wasInLiquid = false;
        liquidExitTime = 0;
        if (lastHvhTarget != null) HvhTargetPredict.reset(lastHvhTarget);
        lastHvhTarget = null;
        super.onDisable();
    }

    public boolean isHvhTargetEnabled() {
        return hvhTarget.getValue();
    }

    private static final float s20 = 0.425F;
    private static final float s0 = 0.36F;
    private static final float s20o = 0.3085F;
    private static final float s0o = 0.254F;
}
