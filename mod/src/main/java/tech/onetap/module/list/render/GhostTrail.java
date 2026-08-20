package tech.onetap.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.providers.ColorProvider;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "GhostTrail", moduleDesc = "Призрачный след игрока", moduleCategory = ModuleCategory.RENDER)
public class GhostTrail extends Module {

    private final SliderSetting lifetime = new SliderSetting("Время жизни", 1450f, 550f, 3000f, 50f);
    private final SliderSetting riseHeight = new SliderSetting("Высота подъёма", 1.85f, 0.45f, 4.0f, 0.05f);
    private final SliderSetting alpha = new SliderSetting("Прозрачность", 0.58f, 0.15f, 1.0f, 0.01f);
    private final SliderSetting glowPower = new SliderSetting("Сила свечения", 1.0f, 0.05f, 2.2f, 0.05f);
    private final SliderSetting maxGhosts = new SliderSetting("Макс. призраков", 7f, 1f, 16f, 1f);
    private final BooleanSetting rainbow = new BooleanSetting("Радужное свечение", false);
    private final BooleanSetting otherPlayers = new BooleanSetting("Другие игроки", true);
    private final BooleanSetting self = new BooleanSetting("Себя", true);

    private final List<GhostEntry> entries = new ArrayList<>();
    private PlayerEntityModel model;
    private boolean registered;

    private final WorldRenderEvents.Last listener = context -> {
        onRenderWorldLast(context.matrixStack(), context.camera());
    };

    @Override
    public void onEnable() {
        if (!registered) {
            WorldRenderEvents.LAST.register(listener);
            registered = true;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        entries.clear();
        super.onDisable();
    }

    private void capture(AbstractClientPlayerEntity player, float tickDelta) {
        Vec3d pos = new Vec3d(
                MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                MathHelper.lerp(tickDelta, player.prevY, player.getY()),
                MathHelper.lerp(tickDelta, player.prevZ, player.getZ()));

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw);
        float headYaw = MathHelper.lerpAngleDegrees(tickDelta, player.prevHeadYaw, player.headYaw);
        float pitch = MathHelper.lerp(tickDelta, player.prevPitch, player.getPitch());
        float limbAngle = player.limbAnimator.getPos();
        float limbDistance = Math.min(player.limbAnimator.getSpeed(), 1f);
        float swingProgress = player.getHandSwingProgress(tickDelta);

        entries.add(new GhostEntry(player, pos, bodyYaw, headYaw, pitch, limbAngle, limbDistance, swingProgress,
                player.isSneaking(), System.currentTimeMillis()));
    }

    private void onRenderWorldLast(MatrixStack matrices, net.minecraft.client.render.Camera camera) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof AbstractClientPlayerEntity player) || !entity.isAlive()) continue;
            if (entity == mc.player && !self.getValue()) continue;
            if (entity != mc.player && !otherPlayers.getValue()) continue;
            capture(player, tickDelta);
        }

        while (entries.size() > (int) maxGhosts.getFloatValue()) {
            entries.remove(0);
        }

        entries.removeIf(e -> System.currentTimeMillis() - e.startMs > lifetime.getFloatValue());
        if (entries.isEmpty()) return;

        if (model == null) {
            model = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
        }

        Vec3d camPos = camera.getPos();
        long now = System.currentTimeMillis();
        long lifeMs = (long) lifetime.getFloatValue();

        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(new BufferAllocator(1536));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        for (GhostEntry entry : entries) {
            float raw = MathHelper.clamp((now - entry.startMs) / (float) lifeMs, 0f, 1f);
            float eased = 1f - (1f - raw) * (1f - raw);
            if (eased <= 0.01f) continue;

            float rise = riseHeight.getFloatValue() * (1f - (float) Math.pow(1f - raw, 2.4));
            int color = getGhostColor(raw, eased);

            PlayerEntityRenderState state = new PlayerEntityRenderState();
            state.bodyYaw = entry.bodyYaw;
            state.yawDegrees = entry.headYaw;
            state.pitch = entry.pitch;
            state.limbFrequency = entry.limbAngle;
            state.limbAmplitudeMultiplier = entry.limbDistance;
            state.handSwingProgress = entry.swingProgress;
            state.sneaking = entry.sneaking;
            state.isInSneakingPose = entry.sneaking;
            state.preferredArm = Arm.RIGHT;
            state.pose = entry.player.getPose();

            Identifier skin = entry.player.getSkinTextures().texture();
            VertexConsumer buffer = immediate.getBuffer(RenderLayer.getEntityTranslucent(skin));

            matrices.push();
            matrices.translate(entry.pos.x - camPos.x, entry.pos.y - camPos.y + rise, entry.pos.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - entry.bodyYaw));

            model.setAngles(state);
            buffer.color(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f,
                    ((color >> 24) & 0xFF) / 255f);
            model.render(matrices, buffer, 15728880, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        immediate.draw();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private int getGhostColor(float raw, float eased) {
        int baseAlpha = (int) MathHelper.clamp(255f * eased * alpha.getFloatValue() * glowPower.getFloatValue(), 0, 255);
        if (rainbow.getValue()) {
            float hue = (raw * 0.18f + (System.currentTimeMillis() % 7000) / 7000f) % 1f;
            int rgb = Color.HSBtoRGB(hue, 0.78f, 1f);
            return (baseAlpha << 24) | (rgb & 0xFFFFFF);
        }
        int theme = ColorProvider.getThemeColor();
        return (baseAlpha << 24) | (theme & 0xFFFFFF);
    }

    private static class GhostEntry {
        final AbstractClientPlayerEntity player;
        final Vec3d pos;
        final float bodyYaw;
        final float headYaw;
        final float pitch;
        final float limbAngle;
        final float limbDistance;
        final float swingProgress;
        final boolean sneaking;
        final long startMs;

        GhostEntry(AbstractClientPlayerEntity player, Vec3d pos, float bodyYaw, float headYaw, float pitch,
                   float limbAngle, float limbDistance, float swingProgress, boolean sneaking, long startMs) {
            this.player = player;
            this.pos = pos;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.pitch = pitch;
            this.limbAngle = limbAngle;
            this.limbDistance = limbDistance;
            this.swingProgress = swingProgress;
            this.sneaking = sneaking;
            this.startMs = startMs;
        }
    }
}
