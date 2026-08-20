package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ModuleInformation(moduleName = "ProjectileTrajectory", moduleDesc = "Траектория снарядов", moduleCategory = ModuleCategory.RENDER)
public class ProjectileTrajectory extends Module {

    private final BooleanSetting enderPearl = new BooleanSetting("Жемчуг", true);
    private final BooleanSetting arrows = new BooleanSetting("Стрелы", true);
    private final BooleanSetting trident = new BooleanSetting("Трезубец", true);
    private final BooleanSetting throwables = new BooleanSetting("Снежки и зелья", true);
    private final BooleanSetting timeLabel = new BooleanSetting("Метки времени", true);
    private final SliderSetting maxTicks = new SliderSetting("Макс. тиков", 150f, 50f, 400f, 10f);
    private final ColorSetting color = new ColorSetting("Цвет", 0xFFFFFFFF);

    private boolean registered;

    private final List<SimLine> simLines = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private final List<Vec3d> impactPoints = new ArrayList<>();

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

    private boolean isAllowed(Entity entity) {
        if (entity instanceof EnderPearlEntity) return enderPearl.getValue();
        if (entity instanceof TridentEntity) return trident.getValue();
        if (entity instanceof ArrowEntity || entity instanceof SpectralArrowEntity) return arrows.getValue();
        return entity instanceof SnowballEntity || entity instanceof EggEntity || entity instanceof PotionEntity
                || entity instanceof FireballEntity || entity instanceof FishingBobberEntity
                ? throwables.getValue() : false;
    }

    private double getGravity(Entity entity) {
        return entity instanceof EnderPearlEntity ? 0.03 : 0.05;
    }

    private boolean hitsBlock(Vec3d from, Vec3d to, Entity entity) {
        if (mc.world == null) return false;
        RaycastContext ctx = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity);
        HitResult result = mc.world.raycast(ctx);
        return result != null && result.getType() == HitResult.Type.BLOCK;
    }

    private void onRenderWorldLast(MatrixStack matrices, net.minecraft.client.render.Camera camera) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;
        if (mc.player.age % 2 != 0) return;

        simLines.clear();
        labels.clear();
        impactPoints.clear();

        Vec3d camPos = camera.getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !isAllowed(entity) || !entity.isAlive()) continue;
            simulate(entity, camPos);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(1.5f);
        RenderSystem.disableDepthTest();

        int base = color.getValue();
        if (!simLines.isEmpty()) {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (SimLine line : simLines) {
                float alpha = 1f - line.progress;
                buffer.vertex(matrix, (float) line.from.x, (float) line.from.y, (float) line.from.z)
                        .color(((base >> 16) & 0xFF) / 255f, ((base >> 8) & 0xFF) / 255f, (base & 0xFF) / 255f, alpha);
                buffer.vertex(matrix, (float) line.to.x, (float) line.to.y, (float) line.to.z)
                        .color(((base >> 16) & 0xFF) / 255f, ((base >> 8) & 0xFF) / 255f, (base & 0xFF) / 255f, alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // ── Точка приземления ──
        for (Vec3d point : impactPoints) {
            double half = 0.08;
            Box box = new Box(point.x - half, point.y - half, point.z - half, point.x + half, point.y + half, point.z + half)
                    .offset(-camPos.x, -camPos.y, -camPos.z);
            BufferBuilder boxBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            float r = ((base >> 16) & 0xFF) / 255f, g = ((base >> 8) & 0xFF) / 255f, b = (base & 0xFF) / 255f;
            line(boxBuffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, 0.9f);
            line(boxBuffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, 0.9f);
            BufferRenderer.drawWithGlobalProgram(boxBuffer.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
    }

    private void simulate(Entity entity, Vec3d camPos) {
        Vec3d pos = entity.getPos();
        Vec3d vel = entity.getVelocity();

        int max = (int) maxTicks.getFloatValue();
        float gravity = (float) getGravity(entity);
        boolean noGravity = entity.hasNoGravity();

        Vec3d prev = pos;
        for (int i = 0; i <= max; i++) {
            Vec3d next = prev.add(vel);

            double drag = entity.isTouchingWater() ? 0.8 : 0.99;
            Vec3d newVel = vel.multiply(drag);
            if (!noGravity) {
                newVel = newVel.subtract(0, gravity, 0);
            }

            simLines.add(new SimLine(prev, next, (float) i / max));

            if (hitsBlock(prev, next, entity) || next.y <= mc.world.getBottomY()) {
                impactPoints.add(next);
                if (timeLabel.getValue()) {
                    projectAndStoreLabel(next, i, camPos);
                }
                break;
            }

            prev = next;
            vel = newVel;
        }
    }

    private void projectAndStoreLabel(Vec3d point, int ticks, Vec3d camPos) {
        double fov = mc.options.getFov().getValue();
        if (fov <= 0) return;

        double relX = point.x - camPos.x;
        double relY = point.y - camPos.y;
        double relZ = point.z - camPos.z;

        double yawRad = Math.toRadians(-mc.gameRenderer.getCamera().getYaw() - 90);
        double pitchRad = Math.toRadians(-mc.gameRenderer.getCamera().getPitch());

        double newX = relZ * Math.cos(yawRad) - relX * Math.sin(yawRad);
        double newY = relY;
        double newZ = relZ * Math.sin(yawRad) + relX * Math.cos(yawRad);

        double newX2 = newX * Math.cos(pitchRad) - newY * Math.sin(pitchRad);
        double newY2 = newX * Math.sin(pitchRad) + newY * Math.cos(pitchRad);

        if (newZ <= 0) return;

        double factor = 1 / Math.tan(Math.toRadians(fov / 2));
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        double screenX = screenWidth / 2d + newX2 * factor * (screenWidth / 2d) / newZ;
        double screenY = screenHeight / 2d - newY2 * factor * (screenHeight / 2d) / newZ;

        String text = String.format(Locale.US, "%.1f сек.", ticks * 0.05f);
        labels.add(new Label((float) screenX, (float) screenY, text));
    }

    @Subscribe
    private void onHud(EventHUD event) {
        if (!isEnabled() || labels.isEmpty()) return;

        var context = event.getDrawContext();
        int color = ColorProvider.rgba(12, 12, 12, 180);

        for (Label label : labels) {
            float w = Fonts.SFREGULAR.get().getWidth(label.text, 6.5f) + 12f;
            float h = 10f;
            float x = label.x - w / 2f;
            float y = label.y + 3f;
            DrawUtil.drawRound(x, y, w, h, 3f, color);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), label.text, x + 6f, y + 1.5f, 0xFFFFFFFF, 6.5f);
        }
    }

    private record SimLine(Vec3d from, Vec3d to, float progress) {}

    private record Label(float x, float y, String text) {}
}
