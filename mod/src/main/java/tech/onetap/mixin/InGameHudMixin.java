package tech.onetap.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.list.render.AntiEffects;
import tech.onetap.module.list.render.CustomCrosshair;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "render", at = @At(value = "RETURN"))
    private void render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        new EventHUD(context, tickCounter).post();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            CustomCrosshair module = Onetap.getInstance().getModuleStorage().get(CustomCrosshair.class);
            if (module != null && module.isEnabled()) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            AntiEffects module = Onetap.getInstance().getModuleStorage().get(AntiEffects.class);
            if (module != null && module.shouldHideHud()) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
}