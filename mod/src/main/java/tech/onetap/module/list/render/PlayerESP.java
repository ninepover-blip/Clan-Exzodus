package tech.onetap.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.friend.FriendRepository;

@ModuleInformation(moduleName = "PlayerESP", moduleDesc = "Подсветка игроков", moduleCategory = ModuleCategory.RENDER)
public class PlayerESP extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Box", "Box", "Outline");
    private final BooleanSetting fill = new BooleanSetting("Заливка", true).setVisible(() -> mode.is("Box"));
    private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", false);
    private final BooleanSetting targetSelf = new BooleanSetting("Себе", false);
    private final SliderSetting lineWidth = new SliderSetting("Толщина", 2.0f, 0.5f, 5.0f, 0.1f);
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей", 0xFF55FF55);
    private final ColorSetting enemyColor = new ColorSetting("Цвет врагов", 0xFFFF5555);

    private boolean registered = false;

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

    private void onRenderWorldLast(MatrixStack matrices, Camera camera) {
        if (!isEnabled() || mc.player == null || mc.world == null) return;

        Vec3d camPos = camera.getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(lineWidth.getFloatValue());

        float width = 2.5f;
        float outlineAlpha = 1f;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || !player.isAlive()) continue;
            if (player == mc.player && !targetSelf.getValue()) continue;

            boolean friend = FriendRepository.isFriend(player.getNameForScoreboard());
            int color = friend ? friendColor.getValue() : enemyColor.getValue();

            if (throughWalls.getValue()) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }

            double x = player.getX() - camPos.x;
            double y = player.getY() - camPos.y;
            double z = player.getZ() - camPos.z;
            float hw = player.getWidth() / 2f;
            Box box = new Box(x - hw, y, z - hw, x + hw, y + player.getHeight(), z + hw);

            if (mode.is("Box")) {
                if (fill.getValue()) {
                    BufferBuilder fillBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                    drawFilledBox(fillBuffer, matrix, box, color, 0.15f);
                    BufferRenderer.drawWithGlobalProgram(fillBuffer.end());
                }
            }

            BufferBuilder lineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            drawBoxOutline(lineBuffer, matrix, box, color, outlineAlpha);
            BufferRenderer.drawWithGlobalProgram(lineBuffer.end());
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
    }

    private void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, Box box, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = alpha;

        line(buffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        line(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);

        line(buffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, matrix, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);

        line(buffer, matrix, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(buffer, matrix, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
    }

    private void drawFilledBox(BufferBuilder buffer, Matrix4f matrix, Box box, int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        quad(buffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, alpha);
        quad(buffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, alpha);
    }

    private void quad(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
        buffer.vertex(matrix, (float) x1, (float) y2, (float) z2).color(r, g, b, a);
    }

    private void line(BufferBuilder buffer, Matrix4f matrix, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
    }
}
