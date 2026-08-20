package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.lwjgl.glfw.GLFW;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.list.render.Hide;
import tech.onetap.ui.killsay.KillSayEditorScreen;
import tech.onetap.util.Inputs;
import tech.onetap.util.killsay.KillSayPresets;
import tech.onetap.util.killsay.KillSayRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInformation(moduleName = "KillSay", moduleDesc = "Пишет сообщение при убийстве и тотеме врага", moduleCategory = ModuleCategory.MISC)
public class KillSay extends Module {

    public final ModeSetting killMode = new ModeSetting("Источник фраз (убийство)", "Смешанные", "Пресеты", "Кастомные", "Смешанные");
    public final ModeSetting totemMode = new ModeSetting("Источник фраз (тотем)", "Смешанные", "Пресеты", "Кастомные", "Смешанные");
    public final BooleanSetting killEnabled = new BooleanSetting("Фразы при убийстве", true);
    public final BooleanSetting totemEnabled = new BooleanSetting("Фразы на тотем", true);
    public final BooleanSetting rmbMenu = new BooleanSetting("Меню по ПКМ", true);

    private UUID targetUuid = null;
    private String targetName = null;

    @Subscribe
    private void onTick(EventTick ignored) {
        KillAura aura = Onetap.getInstance().getModuleStorage().get(KillAura.class);
        if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
            track(aura.getTarget());
        }
    }

    @Subscribe
    private void onAttack(EventAttack event) {
        Entity entity = event.getEntity();
        if (entity instanceof PlayerEntity player && player != mc.player) {
            track(player);
        }
    }

    private void track(Entity entity) {
        if (!(entity instanceof PlayerEntity player) || player == mc.player) return;
        if (player.getUuid().equals(targetUuid)) return;
        targetUuid = player.getUuid();
        targetName = player.getName().getString();
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null || event.getType() != EventPacket.Type.RECEIVE) return;
        if (!(event.getPacket() instanceof EntityStatusS2CPacket packet)) return;

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player) || player == mc.player) return;

        // Тотем: сообщение шлём на любого игрока, а не только на отслеживаемую цель.
        if (packet.getStatus() == 35 && totemEnabled.getValue()) {
            sendMessage(pickRandom(getTotemMessages()), player.getName().getString());
            return;
        }

        if (targetUuid == null) return;
        if (!player.getUuid().equals(targetUuid)) return;

        String name = targetName != null ? targetName : player.getName().getString();

        if (packet.getStatus() == 3 && killEnabled.getValue()) {
            sendMessage(pickRandom(getKillMessages()), name);
            resetTarget();
        }
    }

    @Subscribe
    private void onKey(EventKeyInput event) {
        if (!rmbMenu.getValue()) return;
        if (event.getAction() != 1) return;
        if (mc.currentScreen != null) return;
        if (event.getKey() != Inputs.mouseButtonCode(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return;
        if (Hide.isActive) return;
        mc.setScreen(new KillSayEditorScreen(this));
    }

    private List<String> getKillMessages() {
        return resolveMessages(killMode, KillSayPresets.KILL_MESSAGES, KillSayRepository.getCustomKillMessages());
    }

    private List<String> getTotemMessages() {
        return resolveMessages(totemMode, KillSayPresets.TOTEM_MESSAGES, KillSayRepository.getCustomTotemMessages());
    }

    private List<String> resolveMessages(ModeSetting mode, String[] presets, List<String> custom) {
        if (mode.is("Пресеты")) {
            return List.of(presets);
        }
        if (mode.is("Кастомные")) {
            return new ArrayList<>(custom);
        }
        List<String> combined = new ArrayList<>();
        combined.addAll(List.of(presets));
        combined.addAll(custom);
        return combined;
    }

    private String pickRandom(List<String> messages) {
        if (messages.isEmpty()) return null;
        return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
    }

    private void sendMessage(String msg, String playerName) {
        if (msg == null || msg.isEmpty()) return;
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendChatMessage(msg.replace("{player}", playerName));
    }

    private void resetTarget() {
        targetUuid = null;
        targetName = null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        KillSayRepository.load();
        resetTarget();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetTarget();
    }
}