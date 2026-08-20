package tech.onetap.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.module.list.render.SmartLeaves;

@Mixin(BlockModels.class)
public class MixinBlockModels {

    @Inject(method = "getModel", at = @At("RETURN"), cancellable = true)
    private void smartLeaves(BlockState state, CallbackInfoReturnable<BakedModel> cir) {
        BakedModel model = cir.getReturnValue();
        if (model == null) return;
        cir.setReturnValue(SmartLeaves.getModel(state, model));
    }
}
