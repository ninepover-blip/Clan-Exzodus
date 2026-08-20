package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import tech.onetap.event.list.EventPacket;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.util.base.Instance;

@ModuleInformation(moduleName = "Optimization", moduleDesc = "Оптимизация рендера (Glare / Particles / Nametags)", moduleCategory = ModuleCategory.RENDER)
public class Optimization extends Module {

    public final ModeListSetting optimizeSelection = new ModeListSetting("Optimize",
            new BooleanSetting("Glare", true),
            new BooleanSetting("Particles", true),
            new BooleanSetting("Nametags", false));

    public static boolean isActive() {
        try {
            Optimization optimization = Instance.get(Optimization.class);
            return optimization != null && optimization.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isGlareEnabled() {
        try {
            Optimization optimization = Instance.get(Optimization.class);
            return optimization != null && optimization.isEnabled() && optimization.optimizeSelection.isEnabled("Glare");
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean isNametagsEnabled() {
        try {
            Optimization optimization = Instance.get(Optimization.class);
            return optimization != null && optimization.isEnabled() && optimization.optimizeSelection.isEnabled("Nametags");
        } catch (Exception ignored) {
            return false;
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.getType() != EventPacket.Type.RECEIVE) return;
        if (event.getPacket() instanceof ParticleS2CPacket && optimizeSelection.isEnabled("Particles")) {
            event.cancelEvent();
        }
    }

    public static boolean shouldDisableClickGuiBlur() {
        return isActive() && net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof tech.onetap.ui.ClickGuiScreen;
    }

    public static boolean shouldDisableInterfaceBlur() {
        return isActive();
    }
}