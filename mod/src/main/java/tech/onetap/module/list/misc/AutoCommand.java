package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;
import tech.onetap.event.list.EventPacket;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.TextSetting;

import java.util.Locale;

@ModuleInformation(moduleName = "Auto Command", moduleDesc = "Автокоманды по таймеру и после PvP", moduleCategory = ModuleCategory.MISC)
public class AutoCommand extends Module {

    private final BooleanSetting byTimer = new BooleanSetting("По таймеру", true);
    private final SliderSetting interval = new SliderSetting("Интервал (мин)", 5, 1, 120, 1);
    private final BooleanSetting afterPvP = new BooleanSetting("После PvP", true);
    private final TextSetting pvpTrigger = new TextSetting("Сообщение конца PvP", "поединок окончен");
    private final ModeListSetting commands = new ModeListSetting("Команды",
            new BooleanSetting("/fix all", true),
            new BooleanSetting("/spawn", false),
            new BooleanSetting("/hub", false),
            new BooleanSetting("/fly", false),
            new BooleanSetting("/heal", false),
            new BooleanSetting("/feed", false),
            new BooleanSetting("/god", false)
    );

    private long lastRunAt;

    @Override
    public void onEnable() {
        super.onEnable();
        lastRunAt = System.currentTimeMillis();
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null) return;
        if (!byTimer.getValue()) return;
        if (System.currentTimeMillis() - lastRunAt >= (long) (interval.getValue() * 60_000)) {
            runCommands();
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mc.player == null || !afterPvP.getValue()) return;
        if (e.getType() != EventPacket.Type.RECEIVE) return;
        if (!(e.getPacket() instanceof GameMessageS2CPacket packet)) return;

        String trigger = pvpTrigger.getValue();
        if (trigger == null || trigger.isEmpty()) return;
        if (packet.content().getString().toLowerCase(Locale.ROOT).contains(trigger.toLowerCase(Locale.ROOT))) {
            runCommands();
        }
    }

    private void runCommands() {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRunAt < 3000) return;
        lastRunAt = now;

        for (BooleanSetting setting : commands.getSettings()) {
            if (!setting.getValue()) continue;
            String cmd = setting.getName();
            String clean = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            logDirect("Выполняю: " + cmd, Formatting.AQUA);
            mc.player.networkHandler.sendChatCommand(clean);
        }
    }
}
