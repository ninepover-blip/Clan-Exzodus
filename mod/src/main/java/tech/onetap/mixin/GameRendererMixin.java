package tech.onetap.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.event.list.EventWorldRender;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.module.list.render.AspectRatio;
import tech.onetap.util.base.Instance;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @ModifyReturnValue(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    private Matrix4f applyAspectRatio(Matrix4f projection) {
        AspectRatio aspect = Instance.get(AspectRatio.class);
        if (aspect == null || !aspect.isEnabled()) return projection;
        float current = (float) net.minecraft.client.MinecraftClient.getInstance().getWindow().getFramebufferWidth()
                / net.minecraft.client.MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        return projection.scale(current / aspect.getTargetRatio(), 1.0f, 1.0f);
    }

    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
    public void hookWorldRender(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 2) Matrix4f matrix4f) {
        var matrixStack = new MatrixStack();
        matrixStack.multiplyPositionMatrix(matrix4f);

        var event = new EventWorldRender(matrixStack, tickCounter.getTickDelta(false));
        event.post();
        DrawUtil.onRender3D(event.getMatrixStack());
    }
}
