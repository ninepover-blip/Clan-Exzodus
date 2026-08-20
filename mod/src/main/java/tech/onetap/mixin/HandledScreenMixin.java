package tech.onetap.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHandledScreen;
import tech.onetap.module.list.player.ItemScroller;
import tech.onetap.util.base.Instance;
import tech.onetap.util.gui.InventoryButtons;
import tech.onetap.util.player.other.InventoryUtil;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Onetap.getInstance().getEventBus().post(new EventHandledScreen(focusedSlot));
        InventoryButtons.render(screen, context, mouseX, mouseY, delta, y);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (InventoryButtons.onMouseClicked(screen, mouseX, mouseY, button, y)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (verticalAmount == 0) return;

        ItemScroller scroller = Instance.get(ItemScroller.class);
        if (scroller == null || !scroller.isEnabled()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Slot slot = getSlotAt(screen, mouseX, mouseY);
        if (slot == null || slot.getStack().isEmpty()) return;

        if (screen instanceof InventoryScreen && slot.id < 9) return;

        var client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.interactionManager == null || client.player == null) return;
        client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0,
                SlotActionType.QUICK_MOVE, client.player);
        cir.setReturnValue(true);
    }

    private Slot getSlotAt(HandledScreen<?> screen, double mouseX, double mouseY) {
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!slot.isEnabled()) continue;
            if (mouseX >= x + slot.x - 1 && mouseX < x + slot.x + 17
                    && mouseY >= y + slot.y - 1 && mouseY < y + slot.y + 17) {
                return slot;
            }
        }
        return null;
    }
}
