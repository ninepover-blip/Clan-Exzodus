package tech.onetap.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.module.list.render.ShulkerTooltip;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void onGetTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        try {
            if (Onetap.getInstance() == null) return;
            if (Onetap.getInstance().getModuleStorage() == null) return;

            ShulkerTooltip module = Onetap.getInstance().getModuleStorage().get(ShulkerTooltip.class);
            if (module == null || !module.canPreview((ItemStack) (Object) this)) return;

            List<Text> lines = module.getPreviewLines((ItemStack) (Object) this);
            if (lines.isEmpty()) return;

            List<Text> tooltip = cir.getReturnValue();
            tooltip.add(Text.empty());
            tooltip.addAll(lines);
        } catch (Exception ignored) {}
    }
}
