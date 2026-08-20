package tech.onetap.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.onetap.event.list.ChatEvent;
import tech.onetap.event.list.EventEntitySpawn;
import tech.onetap.util.commands.defaults.ClipBypass;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(
            method = "sendChatMessage(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendChatMessage(String string, CallbackInfo ci) {
        var event = new ChatEvent(string, false);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "sendChatCommand(Ljava/lang/String;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sendChatCommand(String string, CallbackInfo ci) {
        var event = new ChatEvent(string, true);
        event.post();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Shadow
    private ClientWorld world;

    @Inject(
            method = "onEntitySpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;playSpawnSound(Lnet/minecraft/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void hookEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        var entity = this.world.getEntityById(packet.getEntityId());

        if (entity == null) return;

        var event = new EventEntitySpawn(entity);
        event.post();
    }

    @Inject(
            method = "onPlayerPositionLook",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelRubberbandWhileTp(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (!ClipBypass.isTpSessionActive()) return;

        // Отменяем руббербенд (откат позиции) и удерживаем игрока на точке ТП
        ci.cancel();
        var target = ClipBypass.getTpTarget();
        var player = MinecraftClient.getInstance().player;
        if (target != null && player != null) {
            player.setPosition(target.x, target.y, target.z);
        }
    }
}