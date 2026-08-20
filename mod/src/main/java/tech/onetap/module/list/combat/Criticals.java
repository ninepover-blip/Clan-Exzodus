package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventChangeSprint;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.packet.NetworkUtils;
import tech.onetap.util.player.other.WorldUtils;

@ModuleInformation(moduleName = "Criticals", moduleCategory = ModuleCategory.COMBAT)
public class Criticals extends Module {

    public final ModeSetting mode = new ModeSetting("Режим", "Grim", "Grim", "Grim Double", "Polar", "Packet", "UpdatedNCP", "NCP", "Strict", "Legit");

    public boolean isDoubleCrit() {
        return mode.is("Grim Double");
    }

    public static boolean killAuraTriggered;

    @Subscribe
    private void onTick(EventTick ignored) {
        if (!isEnabled() || !mode.is("Legit")) return;
        if (mc.player == null) return;
        if (mc.options.attackKey.isPressed()
                && mc.player.isOnGround()
                && mc.player.getAttackCooldownProgress(0f) >= 0.99f) {
            mc.player.jump();
        }
    }

    @Subscribe
    private void onAttack(EventAttack e) {
        if (killAuraTriggered) return;
        if (mc.player == null || mc.world == null) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        doCrit();
    }

    @Subscribe
    private void onChangeSprint(EventChangeSprint e) {
        if (mc.player == null || mc.world == null) return;
        if (!isEnabled()) return;
        if (killAuraTriggered) return;
        e.setSprinting(false);
    }

    public void doCrit() {
        if (mc.player == null || mc.world == null) return;

        mc.player.setSprinting(false);
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));

        switch (mode.getValue()) {
            case "Legit" -> {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
                }
            }
            case "Grim" -> {
                // A normal descending jump is already a valid critical hit.
                if (!mc.player.isOnGround()) return;
                if (!canPacketCrit()) return;

                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();

                mc.player.fallDistance = 0.1f;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0625, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0015, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
            }
            case "Grim Double" -> {
                // Двойной крит: полный пакетный цикл Grim + повторный подъём,
                // чтобы KillAura могла нанести второй удар в том же падении.
                if (!mc.player.isOnGround()) return;
                if (!canPacketCrit()) return;

                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();

                mc.player.fallDistance = 0.1f;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0625, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0015, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0625, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
            }
            case "Polar" -> {
                // Polar (Verus): двойной оффсет 0.0625 в одном тике
                if (!canPacketCrit()) return;

                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0625, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y + 0.0625, z, false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        x, y, z, false, false
                ));
            }
            case "NCP" -> {
                // Классический NCP: 0.0625 вверх и обратно
                if (!canPacketCrit()) return;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
            }
            case "Packet" -> {
                if (!canPacketCrit()) return;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.0625, mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
            }
            case "UpdatedNCP" -> {
                if (!canPacketCrit()) return;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.0000008, mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
            }
            case "Strict" -> {
                if (!canPacketCrit()) return;

                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.062600301692775, mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY() + 0.07260029960661, mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
                NetworkUtils.sendSilentPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
            }
        }

        if (!killAuraTriggered) {
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(mc.player.input.playerInput));
        }
    }

    private boolean canPacketCrit() {
        return mc.player.isOnGround()
                && !mc.player.getAbilities().flying
                && !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
                && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !mc.player.isInLava()
                && !mc.player.isSubmergedInWater()
                && mc.world.getBlockState(mc.player.getBlockPos()).getBlock() != Blocks.LADDER;
    }
}
