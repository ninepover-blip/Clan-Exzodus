package tech.onetap.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.AntiEffects;

@Mixin(StatusEffectsDisplay.class)
public class StatusEffectsDisplayMixin {

    @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
    private void onDrawStatusEffects(DrawContext context, int x, int y, float tickDelta, CallbackInfo ci) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            AntiEffects module = Onetap.getInstance().getModuleStorage().get(AntiEffects.class);
            if (module != null && module.shouldHideInventory()) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
}
