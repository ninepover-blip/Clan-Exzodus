package tech.onetap.module.list.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.render.cosmetic.CosmeticMesh;
import tech.onetap.util.render.providers.ColorProvider;

@ModuleInformation(moduleName = "Wings", moduleDesc = "Wings and halo cosmetics", moduleCategory = ModuleCategory.RENDER)
public final class Wings extends Module {
    private final ModeSetting style = new ModeSetting("Style", "Wings",
            "Wings", "Halo", "Both");
    private final ModeSetting colorMode = new ModeSetting("Cosmetic Color", "Theme",
            "Theme", "Rainbow", "Custom");
    private final ColorSetting customColor = new ColorSetting("Custom Color", 0xFF9B6CFF)
            .setVisible(() -> colorMode.is("Custom"));
    private final SliderSetting scale = new SliderSetting("Wing Scale", 2.2f, 1.0f, 4.0f, 0.05f);
    private final SliderSetting height = new SliderSetting("Wing Height", 1.3f, 0.5f, 3.0f, 0.05f);
    private final SliderSetting backOffset = new SliderSetting("Back Offset", 0.12f, -0.3f, 0.6f, 0.01f);
    private final SliderSetting haloScale = new SliderSetting("Halo Scale", 1.0f, 0.5f, 2.5f, 0.05f);
    private final SliderSetting haloHeight = new SliderSetting("Halo Height", 2.25f, 1.0f, 4.0f, 0.05f);
    private final SliderSetting opacity = new SliderSetting("Opacity", 100f, 20f, 100f, 1f);

    public Wings() {
        WorldRenderEvents.LAST.register(this::renderWings);
    }

    private void renderWings(WorldRenderContext context) {
        if (!isEnabled() || mc.player == null || mc.world == null || !mc.player.isAlive()) return;

        boolean renderWings = style.is("Wings") || style.is("Both");
        boolean renderHalo = style.is("Halo") || style.is("Both");
        if (!renderWings && !renderHalo) return;

        int color = cosmeticColor();
        float alpha = opacity.getFloatValue() / 100f;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickCounter().getTickDelta(true);
        Vec3d playerPosition = mc.player.getLerpedPos(tickDelta);
        float aimYaw = MathHelper.lerpAngleDegrees(tickDelta, mc.player.prevYaw, mc.player.getYaw());

        if (renderWings) {
            renderMesh(matrices, camera, playerPosition, aimYaw, "wings",
                    -backOffset.getFloatValue(), height.getFloatValue(), scale.getFloatValue(), color, alpha);
        }
        if (renderHalo) {
            renderMesh(matrices, camera, playerPosition, aimYaw, "halo",
                    0f, haloHeight.getFloatValue(), haloScale.getFloatValue(), color, alpha);
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderMesh(MatrixStack matrices, Vec3d camera, Vec3d playerPosition, float yaw,
                            String meshName, float offsetX, float offsetY, float meshScale, int color, float alpha) {
        CosmeticMesh mesh = CosmeticMesh.get(meshName);
        if (mesh.isEmpty()) return;

        matrices.push();
        matrices.translate(playerPosition.x - camera.x, playerPosition.y - camera.y, playerPosition.z - camera.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw - 90f));
        matrices.translate(offsetX, offsetY, 0f);
        matrices.scale(meshScale, meshScale, meshScale);

        BufferBuilder buffer = CosmeticMesh.begin();
        mesh.append(matrices, buffer, color, alpha);
        CosmeticMesh.draw(buffer);
        matrices.pop();
    }

    private int cosmeticColor() {
        if (colorMode.is("Custom")) return customColor.getValue();
        if (colorMode.is("Rainbow")) {
            float hue = (System.currentTimeMillis() % 6000L) / 6000f;
            return java.awt.Color.HSBtoRGB(hue, 0.7f, 1f);
        }
        float wave = (float) ((Math.sin(System.currentTimeMillis() / 650.0) + 1.0) * 0.5);
        return ColorProvider.interpolateColor(ColorProvider.getThemeColor(), ColorProvider.getThemeColorTwo(), wave);
    }
}
