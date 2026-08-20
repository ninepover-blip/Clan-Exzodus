package tech.onetap.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.module.list.render.SwingAnimations;
import tech.onetap.module.list.render.ViewModel;
import tech.onetap.util.base.Instance;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {

    @Shadow
    protected abstract void swingArm(float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm);

    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    public void injectAfterMatrixPushHandPosition(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var viewModel = Instance.get(ViewModel.class);
        if (viewModel.isEnabled() && !item.isEmpty() && !item.contains(DataComponentTypes.MAP_ID)) {
            var isMainHand = hand == Hand.MAIN_HAND;
            var arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            viewModel.applyHandPosition(matrices, arm);
        }
    }

    @Redirect(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FFLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V",
                    ordinal = 2
            )
    )
    public void redirectSwingArmForCustomAnim(HeldItemRenderer instance, float swingProgress, float equipProgress, MatrixStack matrices, int armX, Arm arm) {
        var swingAnimation = Instance.get(SwingAnimations.class);
        if (swingAnimation.isEnabled()) {
            if (arm == Arm.RIGHT) {
                swingAnimation.renderSwordAnimation(matrices, swingProgress, equipProgress, arm);
            } else {
                swingArm(swingProgress, equipProgress, matrices, armX, arm);
            }
        } else {
            swingArm(swingProgress, equipProgress, matrices, armX, arm);
        }
    }
}
