package tech.onetap.module.list.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ActionBindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.player.move.MoveUtil;

@ModuleInformation(moduleName = "Jesus", moduleDesc = "Ходьба по воде (Recode / Oblivion)", moduleCategory = ModuleCategory.MOVEMENT)
public class Jesus extends Module {

    private final ModeSetting mod = new ModeSetting("Мод", "Recode", "Recode", "Oblivion");

    private final ModeSetting recodeMode = new ModeSetting("Режим", "Авто", "Авто", "Простой").setVisible(() -> mod.is("Recode"));
    private final SliderSetting speedSlider = new SliderSetting("Скорость", 0.2f, 0.2f, 1.05f, 0.01f)
            .setVisible(() -> mod.is("Recode") && recodeMode.is("Простой"));
    private final ActionBindSetting boostKey = new ActionBindSetting("Кнопка Буста").setVisible(() -> mod.is("Recode"));

    private final ModeSetting oblivionMode = new ModeSetting("Режим (Oblivion)", "Default", "Default", "MetaHvH", "Meta")
            .setVisible(() -> mod.is("Oblivion"));
    private final SliderSetting zoomSpeed = new SliderSetting("Zoom Speed", 7f, 0f, 10f, 0.1f)
            .setVisible(() -> mod.is("Oblivion") && oblivionMode.is("Default"));
    private final BooleanSetting noJump = new BooleanSetting("Do not land", false)
            .setVisible(() -> mod.is("Oblivion") && oblivionMode.is("Default"));
    private final SliderSetting metaSpeed = new SliderSetting("Meta Speed", 0.2f, 0.2f, 1.05f, 0.01f)
            .setVisible(() -> mod.is("Oblivion") && oblivionMode.is("Meta"));
    private final ActionBindSetting boostKeyOblivion = new ActionBindSetting("BoostKey")
            .setVisible(() -> mod.is("Oblivion") && oblivionMode.is("MetaHvH"));

    private int ticks;
    private long boostEndTime = 0;
    private boolean isBoosting = false;
    private boolean boostPressed = false;

    private final float s15 = 0.47F;
    private final float s0 = 0.43F;
    private final float s20 = 0.515F;
    private final float s25 = 0.54F;

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
        if (e.getAction() != 1) return;
        ActionBindSetting activeBoost = mod.is("Recode") ? boostKey : boostKeyOblivion;
        if (activeBoost.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN && e.getKey() == activeBoost.getKeyCode()) {
            boostPressed = true;
        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || !mc.player.isAlive()) return;

        if (boostPressed) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900;
            boostPressed = false;
        }
        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }

        if (mod.is("Recode")) {
            if (mc.player.isTouchingWater() || mc.player.isInLava()) {
                handleRecode();
            }
        } else {
            if (oblivionMode.is("Default")) {
                handleWaterAndAirMovement();
            } else if (mc.player.isTouchingWater() || mc.player.isInLava()) {
                handleMeta();
            }
        }
    }

    private void handleRecode() {
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance deEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        ItemStack offHandItem = mc.player.getOffHandStack();
        String itemName = offHandItem.getName().getString();
        ItemStack headItem = mc.player.getInventory().armor.get(3);
        ItemStack bootsItem = mc.player.getInventory().armor.get(0);
        ItemStack pantsItem = mc.player.getInventory().armor.get(1);
        ItemStack grudItem = mc.player.getInventory().armor.get(2);
        String headItemName = headItem.getName().getString();
        String bootsItemName = bootsItem.getName().getString();
        String pantsItemName = pantsItem.getName().getString();
        String grudItemName = grudItem.getName().getString();

        float appliedSpeed = 0F;
        if (recodeMode.is("Авто")) {
            if (speedEffect != null) {
                if (speedEffect.getAmplifier() == 2) {
                    appliedSpeed = isBoosterBall(itemName) ? s15 * 1.145F : s0 * 1.145F;
                } else if (speedEffect.getAmplifier() == 1) {
                    appliedSpeed = isBoosterBall(itemName) ? s15 : s0;
                }
            } else {
                appliedSpeed = isBoosterBall(itemName) ? s15 * 0.68F : s0 * 0.68F;
            }
        } else {
            appliedSpeed = speedSlider.getFloatValue();
        }

        appliedSpeed = applyMultipliers(appliedSpeed, deEffect,
                bootsItem, bootsItemName, pantsItem, pantsItemName, headItem, headItemName, grudItem, grudItemName);

        MoveUtil.setMotion(appliedSpeed);
        boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (!isMoving) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(0, velocity.y, 0);
        }
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, mc.options.jumpKey.isPressed() ? 0.019 : 0.003, velocity.z);
    }

    private float applyMultipliers(float appliedSpeed, StatusEffectInstance deEffect,
                                   ItemStack bootsItem, String bootsItemName,
                                   ItemStack pantsItem, String pantsItemName,
                                   ItemStack headItem, String headItemName,
                                   ItemStack grudItem, String grudItemName) {
        if (deEffect != null) {
            appliedSpeed *= 0.85f;
        }
        if (bootsItem.isOf(Items.GOLDEN_BOOTS) && bootsItemName.contains("Тапочки админа SoveryBRIZ")) {
            appliedSpeed *= 1.01F;
        }
        if (pantsItem.isOf(Items.GOLDEN_LEGGINGS) && pantsItemName.contains("Штаны админа stqffy")) {
            appliedSpeed *= 1.02F;
        }
        if (headItem.isOf(Items.GOLDEN_HELMET) && headItemName.contains("Шляпа админа Vester")) {
            appliedSpeed *= 1.05F;
        }
        if (grudItem.isOf(Items.GOLDEN_CHESTPLATE) && grudItemName.contains("Грудак админа lxckscream")) {
            appliedSpeed *= 1.03F;
        }
        if (headItem.isOf(Items.PLAYER_HEAD) && headItemName.contains("Новогодний Подарок")) {
            appliedSpeed *= 0.75F;
        }
        if (isBoosting) {
            appliedSpeed *= 1.85F;
        }
        return appliedSpeed;
    }

    private void handleWaterAndAirMovement() {
        BlockPos playerPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + 0.008D, mc.player.getZ());
        if (mc.world.getBlockState(playerPos).isOf(net.minecraft.block.Blocks.WATER) && !mc.player.isOnGround()) {
            boolean isUp = mc.world.getBlockState(BlockPos.ofFloored(mc.player.getX(), mc.player.getY() + 0.03D, mc.player.getZ()))
                    .isOf(net.minecraft.block.Blocks.WATER);
            Vec3d velocity = mc.player.getVelocity();
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            float yPort = horizontal > 0.1D ? 0.02F : 0.032F;
            double yVel = mc.player.fallDistance < 3.5D ? (isUp ? yPort : -yPort) : -0.1D;
            mc.player.setVelocity(velocity.x, yVel, velocity.z);
        }

        double posY = mc.player.getY();
        if (posY > (double) ((int) posY) + 0.89D && posY <= (double) ((int) posY + 1) || mc.player.fallDistance > 3.5D) {
            mc.player.setPosition(mc.player.getX(), (double) ((int) posY + 1) + 1.0E-45D, mc.player.getZ());
            if (!mc.player.isTouchingWater()) {
                BlockPos waterBlockPos = BlockPos.ofFloored(mc.player.getX(), mc.player.getY() - 0.1D, mc.player.getZ());
                if (mc.world.getBlockState(waterBlockPos).isOf(net.minecraft.block.Blocks.WATER)) {
                    movementInWater();
                }
            }
        }
    }

    private void movementInWater() {
        if (mc.player.horizontalCollision && !noJump.getValue()) {
            mc.player.setVelocity(0, 0.2D, 0);
        }
        if (ticks == 1) {
            MoveUtil.setMotion(1.1f);
            ticks = 0;
        } else {
            ticks = 1;
        }
    }

    private void handleMeta() {
        StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
        StatusEffectInstance deEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
        ItemStack offHandItem = mc.player.getOffHandStack();
        String itemName = offHandItem.getName().getString();
        float appliedSpeed = 0F;

        if (oblivionMode.is("MetaHvH")) {
            if (speedEffect != null) {
                if (speedEffect.getAmplifier() == 2) {
                    appliedSpeed = isBoosterBall(itemName) ? s20 * 1.14F : s0 * 1.14F;
                } else if (speedEffect.getAmplifier() == 1) {
                    appliedSpeed = isBoosterBall(itemName) ? s20 : s0;
                }
            } else {
                appliedSpeed = isBoosterBall(itemName) ? s20 * 0.68F : s0 * 0.68F;
            }
        } else if (oblivionMode.is("Meta")) {
            appliedSpeed = metaSpeed.getFloatValue();
        }

        if (deEffect != null) {
            appliedSpeed *= 0.85f;
        }
        if (isBoosting) {
            appliedSpeed *= 1.85F;
        }

        MoveUtil.setMotion(appliedSpeed);
        boolean isMoving = mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
        if (!isMoving) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(0, velocity.y, 0);
        }
        Vec3d velocity = mc.player.getVelocity();
        mc.player.setVelocity(velocity.x, mc.options.jumpKey.isPressed() ? 0.019 : 0.003, velocity.z);
    }
}
