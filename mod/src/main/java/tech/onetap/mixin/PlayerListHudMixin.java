package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tech.onetap.util.friend.FriendRepository;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void infinyty$colorPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = entry.getProfile().getName();

        if (mc.player != null && entry.getProfile().getId().equals(mc.player.getUuid())) {
            cir.setReturnValue(cir.getReturnValue().copy().formatted(Formatting.BLUE));
        } else if (FriendRepository.isFriend(name)) {
            cir.setReturnValue(cir.getReturnValue().copy().formatted(Formatting.GREEN));
        }
    }
}
