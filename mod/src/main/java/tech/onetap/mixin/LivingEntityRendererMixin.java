package tech.onetap.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.CustomModel;
import tech.onetap.module.list.render.CustomModels;
import tech.onetap.util.base.Instance;
import tech.onetap.util.render.model.CustomModelContext;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Shadow
    protected EntityModel model;

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void onetap$captureRenderContext(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        if (!CustomModels.appliesTo(playerState)) return;
        CustomModelContext.currentState = playerState;
        CustomModelContext.currentProvider = vertexConsumers;
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("RETURN"))
    private void onetap$clearRenderContext(LivingEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        CustomModelContext.currentState = null;
        CustomModelContext.currentProvider = null;
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V",
                    shift = At.Shift.AFTER))
    private void onetap$renderCustomModel(LivingEntityRenderState state, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!Instance.get(CustomModel.class).isEnabled()) return;
        if (!(state instanceof PlayerEntityRenderState playerState)) return;
        if (!(this.model instanceof PlayerEntityModel playerModel)) return;
        CustomModel.renderCustomItems(playerState, matrices, vertexConsumers, light, playerModel);
    }
}
