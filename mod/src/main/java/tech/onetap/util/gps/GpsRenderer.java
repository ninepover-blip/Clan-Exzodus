package tech.onetap.util.gps;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.Heightmap;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

public final class GpsRenderer implements IMinecraft {
    private static final GpsRenderer INSTANCE = new GpsRenderer();
    public static GpsRenderer get() { return INSTANCE; }

    private double targetX, targetZ;
    @Setter private boolean enabled;

    private GpsRenderer() { Onetap.getInstance().getEventBus().register(this); }

    public void setTarget(double x, double z) { this.targetX = x; this.targetZ = z; }

    @Subscribe
    private void onHud(EventHUD e) {
        if (!enabled) return;
        if (MinecraftClient.getInstance().player == null) return;

        MatrixStack ms = e.getDrawContext().getMatrices();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        Vec3d pos = mc.player.getPos();
        double dx = targetX - pos.x; double dz = targetZ - pos.z;
        int dist = (int) Math.sqrt(dx*dx + dz*dz);

        float angle = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float rot = MathHelper.wrapDegrees(angle - mc.player.getYaw());

        String distTxt = dist + "m";
        int theme = ColorProvider.getThemeColor();
        int white = ColorProvider.rgba(255,255,255,255);
        double cx = sw / 2.0, cy = sh / 2.0 - 80;

        ms.push();
        ms.translate(cx, cy, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
        drawArrow(ms.peek().getPositionMatrix(), theme);
        ms.pop();

        float w1 = Fonts.SFMEDIUM.get().getWidth(distTxt,7);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), distTxt, (float)(cx-w1/2), (float)(cy+8), white,7);
    }

    private void drawArrow(Matrix4f matrix, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = 255;
        buffer.vertex(matrix, 0f, -9f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -5.5f, 6f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, 5.5f, 6f, 0f).color(r, g, b, a);
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Subscribe
    private void onWorld(EventWorldRender e) {
        if (!enabled) return;
        if (mc.world == null || mc.player == null) return;

        int theme = ColorProvider.getThemeColor();
        int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(targetX), (int) Math.floor(targetZ));
        int bottom = mc.world.getBottomY();

        DrawUtil.drawLine(targetX, bottom, targetZ, targetX, top, targetZ, theme, 2.5f, false);

        double gy = mc.player.getY() - 1;
        DrawUtil.drawLine(targetX - 1, gy, targetZ, targetX + 1, gy, targetZ,
                ColorProvider.setAlpha(theme, 160), 1.5f, false);
        DrawUtil.drawLine(targetX, gy, targetZ - 1, targetX, gy, targetZ + 1,
                ColorProvider.setAlpha(theme, 160), 1.5f, false);
    }
}
