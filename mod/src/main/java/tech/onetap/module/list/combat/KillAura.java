package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import java.security.SecureRandom;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import tech.onetap.module.list.movement.NoGround;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.EventGameUpdate;
import tech.onetap.event.list.EventChangeSprint;
import tech.onetap.event.list.EventTick;
import tech.onetap.event.list.MoveInputEvent;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.player.ElytraHelper;
import tech.onetap.module.list.player.FreeCamera;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.math.BestPoint;
import tech.onetap.util.target.TargetRepository;
import tech.onetap.util.math.RotationUtil;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.player.combat.PredictUtils;
import tech.onetap.util.player.combat.RaytraceUtil;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.player.simulate.SimulatedPlayer;
import tech.onetap.util.render.math.GCDFixer;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.rotation.Rotation;
import tech.onetap.util.rotation.RotationComponent;
import tech.onetap.util.text.ValueUnit;
import tech.onetap.util.neuro.rotation.AIRotationRecorder;
import tech.onetap.module.list.combat.rotations.*;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "КиллАура", moduleDesc = "Жёсткое наведение и атака цели", moduleCategory = ModuleCategory.COMBAT)
public class KillAura extends Module {

    public final ModeSetting rotation = new ModeSetting(
            "Ротация", "Rage", "Rage"
    );
    public final ModeSetting sortBy = new ModeSetting("Сортировка", "FOV", "FOV", "Дистанция", "Здоровье");
    private final ModeListSetting targets = new ModeListSetting("Таргеты",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Голые", true),
            new BooleanSetting("Монстры", true),
            new BooleanSetting("Животные", true)
    );
    public final ModeSetting moveFix = new ModeSetting("MoveFix", "Сфокусированная", "Свободный", "Сфокусированная");
    public final SliderSetting snapHoldTicks = new SliderSetting("Snap tick", ValueUnit.countable("тик", "тика", "тиков"), 2, 1, 10, 1)
            .setVisible(() -> rotation.is("Snap"));

    public final SliderSetting distance = new SliderSetting("Дистанция", ValueUnit.countable("блок", "блока", "блоков"), 3, 2, 6, 0.1f);
    public final SliderSetting elytraDistance = new SliderSetting("Дистанция (Элитры)", 300, 3, 500, 10);
    private final SliderSetting preRotation = new SliderSetting("Пре дистанция", ValueUnit.countable("блок", "блока", "блоков"), 1.5f, 0, 3, 0.1f);
    private final BooleanSetting stopWhileEating = new BooleanSetting("Не бить при еде", false);
    public final BooleanSetting breakSwing = new BooleanSetting("Ломать swing", false);
    public final BooleanSetting breakShield = new BooleanSetting("Ломать щит", true);
    public final BooleanSetting forceBreakShield = new BooleanSetting("Ломать щит без задержки", true)
            .setVisible(breakShield::getValue);
    private final List<net.minecraft.item.Item> AXES = List.of(
            Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
    );
    public final BooleanSetting raycastCheck = new BooleanSetting("Raycast Check", false).setVisible(() -> false);
    public final BooleanSetting smartAim = new BooleanSetting("Smart Aim", true).setVisible(() -> false);
    public final BooleanSetting predictate = new BooleanSetting("Elytra Prediction", true).setVisible(() -> false);
    public final SliderSetting predictValue = new SliderSetting("Prediction", 3, 1, 5, 0.1f).setVisible(() -> false);

    public final BooleanSetting hitAfterOvertake = new BooleanSetting("Hit After Overtake", false).setVisible(() -> false);

    public final BooleanSetting onlySpace = new BooleanSetting("Only With Space", false).setVisible(() -> false);
    public final BooleanSetting clientLook = new BooleanSetting("Client Look", false).setVisible(() -> false);
    public final BooleanSetting showPredictPoint = new BooleanSetting("Show Prediction Point", false).setVisible(() -> false);
    public final BooleanSetting elytraTurnaround = new BooleanSetting("Elytra Turnaround", false).setVisible(() -> false);

    public static final BooleanSetting useResolver = new BooleanSetting("Elytra Resolver", false).setVisible(() -> false);

    public final BooleanSetting autoMace = new BooleanSetting("AutoMace", false).setVisible(() -> false);
    public final BooleanSetting forceAutoMace = new BooleanSetting("AutoMace без задержки", true)
            .setVisible(() -> false);
    public final BooleanSetting syncHurtTime = new BooleanSetting("Синхронизация с HurtTime", false)
            .setVisible(() -> false);
    public final ModeSetting macePriority = new ModeSetting("Приоритет булавы", "Нет",
            "Нет", "Плотность", "Пробитие", "Ветер")
            .setVisible(() -> false);
    public final BooleanSetting autoMaceElytra = new BooleanSetting("AutoMace (элитра)", false)
            .setVisible(() -> false);
    public final BooleanSetting autoMaceElytraBack = new BooleanSetting("Возврат элитры после AutoMace", false)
            .setVisible(() -> false);
    public final SliderSetting autoMaceElytraBackDelay = new SliderSetting("Задержка возврата элитры", 0, 0, 10, 1)
            .setVisible(() -> false);

    public final BooleanSetting onlyCriticalHits = new BooleanSetting("Only Critical Hits", true);
    public final BooleanSetting jumpSync = new BooleanSetting("Синхронизация прыжка", false)
            .setVisible(() -> onlyCriticalHits.getValue());
    public final BooleanSetting airJump = new BooleanSetting("Воздушный прыжок при ударе", false)
            .setVisible(() -> onlyCriticalHits.getValue());
    public final BooleanSetting doubleCrit = new BooleanSetting("Двойной крит", false)
            .setVisible(() -> onlyCriticalHits.getValue());
    private final BooleanSetting chaseTarget = new BooleanSetting("Target Lock", false).setVisible(() -> false);
    private final BooleanSetting chaseBoost = new BooleanSetting("Target Lock Boost", false).setVisible(() -> false);
    private final SliderSetting chaseLead = new SliderSetting("Target Lead", 0, 0, 0, 0.1).setVisible(() -> false);
    private final SliderSetting chaseTolerance = new SliderSetting("Target Tolerance", 0.05, 0.05, 0.05, 0.05).setVisible(() -> false);
    private final BooleanSetting chaseJump = new BooleanSetting("Target Auto Jump", true).setVisible(() -> false);

    public final SliderSetting neuroYawMultiplier = new SliderSetting("Yaw множитель", 1.0, 0.5, 2.0, 0.05)
            .setVisible(() -> rotation.is("Neuro"));
    public final SliderSetting neuroPitchMultiplier = new SliderSetting("Pitch множитель", 1.0, 0.5, 2.0, 0.05)
            .setVisible(() -> rotation.is("Neuro"));
    public final BooleanSetting neuroCorrection = new BooleanSetting("Интерполяция", false)
            .setVisible(() -> rotation.is("Neuro"));


    public boolean isResolving = false;
    public Vec3d resolverPoint = null;
    private final StopWatch resolverTimer = new StopWatch();

    // Экземпляры ротаций (каждая хранит своё внутреннее состояние)
    private final VanillaRotation vanillaRotation = new VanillaRotation();
    private final SnapRotation snapRotation = new SnapRotation();
    private final Sloth2Rotation sloth2Rotation = new Sloth2Rotation();
    private final Sloth3Rotation sloth3Rotation = new Sloth3Rotation();
    private final SlothRotation slothRotation = new SlothRotation();
    private final WellmineRotation wellmineRotation = new WellmineRotation();
    private final NoRotRotation noRotRotation = new NoRotRotation();
    private final LonyGriefRotation lonyGriefRotation = new LonyGriefRotation();
    private final VulcanRotation vulcanRotation = new VulcanRotation();
    private final FuntimeRotation funtimeRotation = new FuntimeRotation();
    private final SpookyTimeRotation spookyTimeRotation = new SpookyTimeRotation();
    private final UniversalRotation universalRotation = new UniversalRotation();
    private final GrimFunRotation grimFunRotation = new GrimFunRotation();
    private final NeuroRotation neuroRotation = new NeuroRotation();

    private boolean interpolationRotationInitialized;
    private LivingEntity interpolationRotationTarget;
    private float interpolatedYaw;
    private float interpolatedPitch;

    private long lastJerkTime = 0;

    private float targetOvershootYaw = 0;
    private float targetOvershootPitch = 0;
    private float jerkSpeedMultiplier = 1.0f;

    public boolean isTurnaroundActive = false;
    public static boolean isSlowdownActive = false;
    private static StopWatch stopWatch = new StopWatch();
    @Getter
    private LivingEntity target;
    public static LivingEntity lastTarget;
    @Getter
    private static LivingEntity lastAttackedTarget;
    public int ticksToAttack;
    private boolean doubleCritPending;
    private boolean autoMaceElytraSwapped;
    private boolean autoMaceElytraSwappedThisAttack;
    private int autoMaceElytraBackTicks;

    private int razvorotikTicks;

    public boolean back;
    public float speedAcceleration;
    public float obhod;
    public static long lastPhysicalMoveTime;

    // Поля для логики Snap (используются также в canAttack/onUpdate)
    public boolean snapActive = false;
    public int snapTimer = 0;

    public boolean isSnapActive() {
        return snapActive;
    }

    public tech.onetap.util.rotation.MoveFixMode getMoveFixMode() {
        return moveFix.is("Свободный") ? tech.onetap.util.rotation.MoveFixMode.FREE : tech.onetap.util.rotation.MoveFixMode.CORRECT;
    }

    private final StopWatch turnaroundTimer = new StopWatch();

    public float preddict;
    public float lastYaw;
    public float lastPitch;
    private float velocityYaw = 0.0F;

    private boolean renderListenerRegistered = false;
    private final WorldRenderEvents.Last renderListener = context -> {
        if (isEnabled() && showPredictPoint.getValue()) {
            renderPredictPoint(context.matrixStack(), context.camera(), context.tickCounter().getTickDelta(true));
        }
    };

    private void findResolverPoint() {
        if (mc.player == null || mc.world == null) return;
        Vec3d eye = mc.player.getEyePos();

        float oppositeYaw = mc.player.getYaw() + 180f;
        float searchPitch = -50f;

        int[] yawOffsets = {0, 30, -30, 45, -45, 60, -60, 90, -90};

        for (int offset : yawOffsets) {
            float testYaw = oppositeYaw + offset;

            float radYaw = (float) Math.toRadians(testYaw);
            float radPitch = (float) Math.toRadians(searchPitch);

            double x = -Math.sin(radYaw) * Math.cos(radPitch);
            double y = -Math.sin(radPitch);
            double z = Math.cos(radYaw) * Math.cos(radPitch);

            Vec3d checkVec = new Vec3d(x, y, z).normalize().multiply(8.0);
            Vec3d endPoint = eye.add(checkVec);

            if (mc.world.raycast(new RaycastContext(eye, endPoint, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS) {
                resolverPoint = endPoint;
                return;
            }
        }
        resolverPoint = null;
    }

    @Subscribe
    private void onGameUpdate(EventGameUpdate e) {
        if (mc.player == null) return;
        if (target == null && !rotation.is("Universal")) return;

        Onetap.getInstance().getModuleStorage().setRandomness(1);

        if (AIRotationRecorder.isRecording()) {
            return;
        }

        if (isResolving && target != null) {
            if (resolverTimer.isReached(300)) {
                isResolving = false;
            } else if (resolverPoint != null) {
                var rot = new Rotation(RotationUtil.calculate(resolverPoint));
                RotationComponent.update(rot, 360, 360, 360, 360, 0, 1, clientLook.getValue(), getMoveFixMode(), "KillAura");
                lastYaw = rot.getYaw();
                lastPitch = rot.getPitch();
                return;
            }
        }

        switch (rotation.getValue()) {
            case "Vanilla" -> vanillaRotation.update(this, target);
            case "Snap" -> snapRotation.update(this, target);
            case "Sloth2" -> sloth2Rotation.update(this, target);
            case "Sloth3" -> sloth3Rotation.update(this, target);
            case "Sloth" -> slothRotation.update(this, target);
            case "Wellmine old" -> wellmineRotation.update(this, target);
            case "NoRot" -> noRotRotation.update(this, target);
            case "LonyGrief" -> lonyGriefRotation.update(this, target);
            case "Vulcan" -> vulcanRotation.update(this, target);
            case "Funtime" -> funtimeRotation.update(this, target);
            case "SpookyTime" -> spookyTimeRotation.update(this, target);
            case "Universal" -> universalRotation.update(this, target);
            case "GrimFun" -> grimFunRotation.update(this, target);
            case "Rage" -> grimFunRotation.update(this, target);
            case "Neuro" -> neuroRotation.update(this, target);
        }
    }

    @Subscribe
    private void onChangeSprint(EventChangeSprint e) {
        if (canStopSprinting()) e.setSprinting(false);
    }

    @Subscribe
    private void onUpdate(final EventTick ignored) {
        if (mc.player == null || mc.world == null) return;

        if (ticksToAttack > 0) ticksToAttack--;
        updateAutoMaceElytraBack();
        if (razvorotikTicks > 0) razvorotikTicks--;

        updateTarget();

        if (target != null) {
            lastTarget = target;
            isSlowdownActive = false;

            if (canStopSprinting()) {
                mc.player.setSprinting(false);
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }

            handleJumpSync();

            if (doubleCritPending) {
                if (mc.player.isOnGround()) {
                    doubleCritPending = false;
                } else if (mc.player.getAttackCooldownProgress(0.5f) >= 0.95f && ticksToAttack <= 0) {
                    ticksToAttack = 0;
                    if (canAttack()) {
                        performAttack();
                        doubleCritPending = false;
                    }
                }
            }

            if (canAttack()) {
                if (useResolver.getValue() && mc.player.isGliding()) {
                    mc.player.setVelocity(0, 0, 0);
                    findResolverPoint();
                    if (resolverPoint != null) {
                        isResolving = true;
                        resolverTimer.reset();
                    }
                }
                performAttack();
            }
        } else {
            doubleCritPending = false;
            speedAcceleration = 0;
            razvorotikTicks = 0;
            snapActive = false;
            snapTimer = 0;
            slothRotation.reset(this);
            sloth2Rotation.reset(this);
            if (!rotation.is("Universal")) {
                universalRotation.reset(this);
            }
            grimFunRotation.reset(this);
            neuroRotation.reset(this);
        }
    }

    private void performAttack() {
        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
        int previousSlot = swapToAxe();

        Criticals crits = Onetap.getInstance().getModuleStorage().get(Criticals.class);
        if (crits != null && crits.isEnabled()) {
            crits.killAuraTriggered = true;
            crits.doCrit();
        }

        mc.interactionManager.attackEntity(mc.player, target);
        lastAttackedTarget = target;

        if (crits != null) crits.killAuraTriggered = false;

        mc.player.swingHand(breakSwing.getValue() ? Hand.OFF_HAND : Hand.MAIN_HAND);

        if (previousSlot != -1) {
            swapBack(previousSlot);
        }

        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));

        if (!isForceBreakShieldReady()) {
            ticksToAttack = 10;
        }

        if (rotation.is("Sloth2")) {
            sloth2Rotation.onAttack();
        }

        if (rotation.is("Snap")) {
            snapActive = false;
            snapTimer = 0;
        }

        if (airJump.getValue() && !mc.player.isOnGround()) {
            var velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, 0.42, velocity.z);
        }

        if (doubleCrit.getValue() && !mc.player.isOnGround()) {
            doubleCritPending = true;
        }
    }

    private void handleJumpSync() {
        if (!jumpSync.getValue() || !onlyCriticalHits.getValue()) return;
        if (target == null || mc.player == null || mc.world == null) return;
        if (!mc.player.isOnGround()) return;
        if (mc.player.isTouchingWater() || mc.player.isSubmergedInWater() || mc.player.isInLava()) return;
        if (mc.player.isClimbing()) return;
        if (mc.player.getAbilities().flying || mc.player.isGliding()) return;
        if (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION)) return;

        float progress = mc.player.getAttackCooldownProgress(0.5f);
        if (progress < 0.35f || progress > 0.75f) return;
        if (!isInAttackDistance(mc.player, target)) return;
        if (hasLowCeilingAbove()) return;

        mc.player.jump();
    }

    private boolean hasLowCeilingAbove() {
        net.minecraft.util.math.BlockPos pos = mc.player.getBlockPos();
        for (int dy = 2; dy <= 3; dy++) {
            net.minecraft.util.math.BlockPos above = pos.up(dy);
            net.minecraft.block.BlockState state = mc.world.getBlockState(above);
            if (!state.isAir() && !state.getCollisionShape(mc.world, above).isEmpty()) return true;
        }
        return false;
    }

    private boolean isValidEntity(Entity entity) {
        if (!entity.isAlive()) return false;
        FreeCamera freeCamera = Onetap.getInstance().getModuleStorage().get(FreeCamera.class);
        PlayerEntity player = freeCamera.fakePlayer != null
                ? (PlayerEntity) freeCamera.fakePlayer
                : (PlayerEntity) mc.player;
        if (entity == freeCamera.fakePlayer) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;
        if (entity instanceof PlayerEntity p) {
            if (p.getArmor() != 0 && !targets.isEnabled("Игроки")) return false;
            if (p.getArmor() == 0 && !targets.isEnabled("Голые")) return false;
            if (Onetap.getInstance().getModuleStorage().get(AntiBot.class).isBot(p)) return false;
            if (!FriendRepository.shouldAttack(p)) return false;
        } else if (entity instanceof HostileEntity || entity instanceof AmbientEntity) {
            if (!targets.isEnabled("Монстры")) return false;
        } else if (entity instanceof PassiveEntity || entity instanceof FishEntity) {
            if (!targets.isEnabled("Животные")) return false;
        } else {
            return false;
        }
        if (player.getEyePos().distanceTo(BestPoint.getNearestPoint(entity)) > getTargetSearchDistance(player))
            return false;
        return true;
    }

    public boolean canAttack() {
        if (target == null) return false;
        if (mc.player.isUsingItem() && stopWhileEating.getValue()) return false;

        FreeCamera freeCamera = Onetap.getInstance().getModuleStorage().get(FreeCamera.class);
        PlayerEntity player = freeCamera.fakePlayer != null
                ? (PlayerEntity) freeCamera.fakePlayer
                : (PlayerEntity) mc.player;

        if (!isInAttackDistance(player, target)) return false;

        // Rage always attacks through the rotation already sent by RotationComponent.
        // Checking the local camera direction here used to cancel every hit when
        // Client Look was disabled.
        isTurnaroundActive = false;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.90f) return false;
        if (ticksToAttack > 0) return false;
        if (onlyCriticalHits.getValue()) {
            boolean fallingCritical = !mc.player.isOnGround()
                    && mc.player.fallDistance > 0.0f
                    && mc.player.getVelocity().y < -0.001;
            if (!fallingCritical) return false;
        }
        return true;
    }

    private boolean isInAttackDistance(PlayerEntity player, LivingEntity entity) {
        Vec3d nearestPoint = BestPoint.getNearestPoint(entity);
        if (nearestPoint == null) return false;

        double attackDistance = player.isGliding() ? elytraDistance.getValue() : distance.getValue();
        return player.getEyePos().distanceTo(nearestPoint) <= attackDistance;
    }

    private double getTargetSearchDistance(PlayerEntity player) {
        double searchDistance = player.isGliding() ? elytraDistance.getValue() : distance.getValue() + preRotation.getValue();

        // HvH Target: если в Speed (HvH) включён HvH Target — ищем цель на любой дистанции (100 блоков)
        tech.onetap.module.list.movement.Speed speed = Onetap.getInstance().getModuleStorage().get(tech.onetap.module.list.movement.Speed.class);
        if (speed != null && speed.isEnabled() && speed.isHvhTargetEnabled()) {
            searchDistance = Math.max(searchDistance, 100.0);
        }

        return searchDistance;
    }

    private boolean canReachWithPositionAura(LivingEntity entity) {
        return false;
    }

    private boolean isTargetBlocking() {
        return target != null && target.isUsingItem() && target.getActiveItem().isOf(Items.SHIELD);
    }

    private boolean isForceBreakShieldReady() {
        int axeSlot = -1;
        for (net.minecraft.item.Item axe : AXES) {
            int slot = InventoryUtil.searchItemHotbar(axe);
            if (slot != -1) {
                axeSlot = slot;
                break;
            }
        }

        return breakShield.getValue()
                && forceBreakShield.getValue()
                && isTargetBlocking()
                && axeSlot != -1;
    }

    private int swapToAxe() {
        if (!breakShield.getValue() || target == null || !isTargetBlocking()) return -1;

        boolean hasAxe = AXES.stream().anyMatch(axe -> InventoryUtil.searchItemHotbar(axe) != -1);
        if (!hasAxe) return -1;

        int axeSlot = -1;
        for (net.minecraft.item.Item axe : AXES) {
            int slot = InventoryUtil.searchItemHotbar(axe);
            if (slot != -1) {
                axeSlot = slot;
                break;
            }
        }

        if (axeSlot == -1 || mc.player.getInventory().selectedSlot == axeSlot) return -1;

        int previousSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = axeSlot;
        mc.interactionManager.syncSelectedSlot();
        return previousSlot;
    }

    private int swapToMace() {
        if (!autoMace.getValue()) return -1;
        if (mc.player.isGliding() && !autoMaceElytra.getValue()) return -1;

        // Проверяем, включен ли NoGround через хранилище модулей
        boolean isNoGroundActive = Onetap.getInstance().getModuleStorage().get(NoGround.class).isEnabled()
                || Onetap.getInstance().getModuleStorage().get(MaceKill.class).isEnabled();

        // Если NoGround/MaceKill выключены, оставляем стандартную проверку на дистанцию падения
        if (!isNoGroundActive && mc.player.fallDistance < 1.8f) return -1;

        int maceSlot = findBestMaceSlot();
        if (maceSlot == -1) return -1;

        int previousSlot = mc.player.getInventory().selectedSlot;
        if (previousSlot == maceSlot) {
            swapElytraForAutoMace();
            return -1;
        }

        swapElytraForAutoMace();

        mc.player.getInventory().selectedSlot = maceSlot;
        mc.interactionManager.syncSelectedSlot();
        return previousSlot;
    }

    private int findBestMaceSlot() {
        int firstMaceSlot = -1;
        int bestSlot = -1;
        int bestPriorityLevel = -1;

        var density = mc.world.getRegistryManager()
                .getOptional(RegistryKeys.ENCHANTMENT).get()
                .getEntry(Enchantments.DENSITY.getValue()).orElseThrow();
        var breach = mc.world.getRegistryManager()
                .getOptional(RegistryKeys.ENCHANTMENT).get()
                .getEntry(Enchantments.BREACH.getValue()).orElseThrow();
        var windBurst = mc.world.getRegistryManager()
                .getOptional(RegistryKeys.ENCHANTMENT).get()
                .getEntry(Enchantments.WIND_BURST.getValue()).orElseThrow();

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isOf(Items.MACE)) continue;

            if (firstMaceSlot == -1) firstMaceSlot = slot;
            if (macePriority.is("Нет")) continue;

            int level = 0;
            switch (macePriority.getValue()) {
                case "Плотность" -> level = EnchantmentHelper.getLevel(density, stack);
                case "Пробитие" -> level = EnchantmentHelper.getLevel(breach, stack);
                case "Ветер" -> level = EnchantmentHelper.getLevel(windBurst, stack);
            }

            if (level > bestPriorityLevel) {
                bestPriorityLevel = level;
                bestSlot = slot;
            }
        }

        if (macePriority.is("Нет")) return firstMaceSlot;
        return bestSlot != -1 ? bestSlot : firstMaceSlot;
    }

    private void swapElytraForAutoMace() {
        if (!autoMaceElytra.getValue()) return;
        if (!mc.player.isGliding()) return;
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) return;

        Instance.get(ElytraHelper.class).swap(true);
        autoMaceElytraSwapped = true;
        autoMaceElytraSwappedThisAttack = true;
    }

    private void scheduleAutoMaceElytraBack() {
        if (!autoMaceElytraBack.getValue()) return;
        if (!autoMaceElytraSwappedThisAttack) return;

        autoMaceElytraBackTicks = (int) autoMaceElytraBackDelay.getValue();
        if (autoMaceElytraBackTicks <= 0) {
            swapBackElytraForAutoMace();
        }
    }

    private void updateAutoMaceElytraBack() {
        if (!autoMaceElytraSwapped) return;
        if (autoMaceElytraBackTicks <= 0) return;

        autoMaceElytraBackTicks--;
        if (autoMaceElytraBackTicks <= 0) {
            swapBackElytraForAutoMace();
        }
    }

    private void swapBackElytraForAutoMace() {
        Instance.get(ElytraHelper.class).swap(false);
        autoMaceElytraSwapped = false;
        autoMaceElytraSwappedThisAttack = false;
        autoMaceElytraBackTicks = 0;
    }

    private void swapBack(int previousSlot) {
        if (previousSlot == -1) return;

        mc.player.getInventory().selectedSlot = previousSlot;
        mc.interactionManager.syncSelectedSlot();
    }

    private boolean isMaceAttackReady() {
        boolean isNoGroundActive = Onetap.getInstance().getModuleStorage().get(NoGround.class).isEnabled();

        return autoMace.getValue()
                && (!mc.player.isGliding() || autoMaceElytra.getValue())
                && (isNoGroundActive || mc.player.fallDistance >= 1.8f)
                && findBestMaceSlot() != -1;
    }

    private boolean isForceAutoMaceReady() {
        if (!autoMace.getValue() || !forceAutoMace.getValue()) return false;
        if (!isMaceAttackReady()) return false;
        if (syncHurtTime.getValue() && target != null && target.hurtTime > 1) return false;
        return true;
    }

    public boolean canStopSprinting() {
        if (target == null) return false;
        if (!Onetap.getInstance().getIdealHitUtils().cooldownIsReached(true)) return false;
        if (ticksToAttack > 1) return false;
        if (SimulatedPlayer.simulateLocalPlayer(1).fallDistance == 0) return false;
        return true;
    }

    @Subscribe
    private void onChaseInput(MoveInputEvent event) {
        // Target movement assistance was removed. KillAura only rotates the character.
    }

    private void updateTarget() {
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        LivingEntity bestTargetList = null;
        double bestTargetListScore = Double.NEGATIVE_INFINITY;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living) {
                if (!isValidEntity(entity)) continue;

                double score;
                switch (sortBy.getValue()) {
                    case "Дистанция" -> {
                        score = -eyePos.distanceTo(BestPoint.getNearestPoint(entity));
                    }
                    case "Здоровье" -> {
                        score = -living.getHealth();
                    }
                    default -> {
                        Vec3d targetVec = BestPoint.getNearestPoint(entity).subtract(eyePos).normalize();
                        score = lookVec.dotProduct(targetVec);
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = living;
                }

                if (entity instanceof PlayerEntity p && TargetRepository.isTarget(p.getNameForScoreboard())) {
                    if (score > bestTargetListScore) {
                        bestTargetListScore = score;
                        bestTargetList = living;
                    }
                }
            }
        }

        if (bestTargetList != null) {
            if (target == null || !isValidEntity(target)
                    || !(target instanceof PlayerEntity cur) || !TargetRepository.isTarget(cur.getNameForScoreboard())) {
                this.target = bestTargetList;
            }
        } else if (target == null || !isValidEntity(target)) {
            this.target = best;
        }
    }

    public Vec3d resolveMultipoint(LivingEntity target, Vec3d point, double range) {
        if (!smartAim.getValue() || target == null) {
            return point;
        }

        return BestPoint.getNearestVisiblePoint(target, point, range);
    }

    private float applyGCD(float deltaRotation) {
        float sensitivity = (float) (mc.options.getMouseSensitivity().getValue() * 0.6f + 0.2f);
        float multiplier = sensitivity * sensitivity * sensitivity * 8.0f * 0.15f;
        return (Math.round(deltaRotation / multiplier) * multiplier);
    }

    private void renderPredictPoint(MatrixStack matrices, Camera camera, float tickDelta) {
        if (target == null || !target.isGliding()) return;

        Vec3d predictPos = PredictUtils.getPredictedRender(target, predictValue.getValue(), tickDelta);
        Vec3d camPos = camera.getPos();

        double renderX = predictPos.x - camPos.x;
        double renderY = predictPos.y - camPos.y;
        double renderZ = predictPos.z - camPos.z;

        float size = 0.35f;
        int color = ColorProvider.getThemeColor();

        matrices.push();
        matrices.translate(renderX, renderY, renderZ);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 1;

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, -size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, a);

        buffer.vertex(matrix, size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, size, size, size).color(r, g, b, a);

        buffer.vertex(matrix, -size, -size, size).color(r, g, b, a);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void renderPositionAuraPoint(MatrixStack matrices, Camera camera) {
        if (target == null) return;

        Vec3d position = null;
        BoatAura boatAura = Instance.get(BoatAura.class);
        if (boatAura != null && boatAura.isEnabled()) {
            position = boatAura.getRenderPosition(target);
        }

        if (position == null) {
            TpAura tpAura = Instance.get(TpAura.class);
            if (tpAura != null && tpAura.isEnabled()) {
                position = tpAura.getRenderPosition(target);
            }
        }

        if (position == null) return;

        Vec3d camPos = camera.getPos();
        double minX = position.x - 0.35 - camPos.x;
        double minY = position.y - camPos.y;
        double minZ = position.z - 0.35 - camPos.z;
        double maxX = position.x + 0.35 - camPos.x;
        double maxY = position.y + 0.7 - camPos.y;
        double maxZ = position.z + 0.35 - camPos.z;

        int color = ColorProvider.getThemeColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(2.0f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        drawLineBox(buffer, matrix, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 1.0f);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void drawLineBox(BufferBuilder buffer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
    }

    @Override
    public void onEnable() {
        this.lastYaw = 0.0f;
        this.lastPitch = 0.0f;
        target = null;
        razvorotikTicks = 0;
        snapActive = false;
        snapTimer = 0;
        neuroRotation.reset(this);
        Onetap.getInstance().getModuleStorage().setSpeedAcceleration(0);

        if (!renderListenerRegistered) {
            WorldRenderEvents.LAST.register(renderListener);
            renderListenerRegistered = true;
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        target = null;
        ticksToAttack = 0;
        speedAcceleration = 0;
        interpolationRotationInitialized = false;
        interpolationRotationTarget = null;
        targetOvershootYaw = 0;
        targetOvershootPitch = 0;
        jerkSpeedMultiplier = 1.0f;
        razvorotikTicks = 0;
        snapActive = false;
        snapTimer = 0;
        isResolving = false;
        resolverPoint = null;
        neuroRotation.reset(this);
        Onetap.getInstance().getModuleStorage().setSpeedAcceleration(0);
        Onetap.getInstance().getModuleStorage().setRandomness(1);
        RotationComponent.getInstance().clearMoveFixMode("KillAura");
        RotationComponent.getInstance().stopRotation();
        super.onDisable();
    }
}
