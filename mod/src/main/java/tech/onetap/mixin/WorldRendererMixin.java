package tech.onetap.mixin;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.Optimization;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "HEAD"), cancellable = true)
    private void drawEntityOutlinesFramebuffer(CallbackInfo ci) {
        if (Optimization.isGlareEnabled()) {
            ci.cancel();
        }
    }
}