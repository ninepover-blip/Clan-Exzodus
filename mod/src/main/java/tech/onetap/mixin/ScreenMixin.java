package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Unique
    private static final Identifier MENU_BG = Identifier.of("mre", "images/infynyty-menu.png");

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void onRenderBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!isMenuScreen(screen)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        try {
            AbstractTexture bgTex = mc.getTextureManager().getTexture(MENU_BG);
            Builder.texture()
                .size(new SizeState(w, h))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, 200)))
                .texture(0, 0, 1, 1, bgTex)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), 0, 0, 0);
        } catch (Exception e) {
            // fallback
        }

        DrawUtil.drawRound(0, 0, w, h, 0,
                ColorProvider.rgba(10, 5, 22, 180),
                ColorProvider.rgba(22, 8, 40, 180));
    }

    @Unique
    private boolean isMenuScreen(Screen screen) {
        return screen instanceof MultiplayerScreen
            || screen instanceof SelectWorldScreen
            || screen instanceof OptionsScreen;
    }
}
