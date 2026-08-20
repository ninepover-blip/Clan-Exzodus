package tech.onetap.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.CustomModels;

@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void onetap$hideArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState playerState && CustomModels.appliesTo(playerState)) {
            ci.cancel();
        }
    }
}
