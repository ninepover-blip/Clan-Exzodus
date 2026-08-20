package tech.onetap.mixin;

import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.CustomModels;
import tech.onetap.util.render.model.CustomModelContext;
import tech.onetap.util.render.model.CustomPlayerModel;

@Mixin(Model.class)
public class ModelMixin {

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
    private void onetap$renderCustomModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntityModel playerModel)) return;
        if (CustomModelContext.currentState == null || CustomModelContext.currentProvider == null) return;

        CustomPlayerModel model = CustomModels.modelFor(CustomModelContext.currentState);
        if (model == null) return;

        ci.cancel();
        model.render(matrices, CustomModelContext.currentProvider, light, overlay, color, playerModel);
    }
}
