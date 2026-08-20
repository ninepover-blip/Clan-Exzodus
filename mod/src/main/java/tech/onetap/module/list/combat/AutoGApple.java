package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.text.ValueUnit;

/**
 * Adapted from ThunderHack's AutoGApple for the LayF/Onetap event and
 * settings systems. It automatically holds use while a golden apple is
 * available in the offhand and health is below the configured threshold.
 */
@ModuleInformation(
        moduleName = "Auto GApple",
        moduleDesc = "Автоматически ест золотое яблоко при низком здоровье",
        moduleCategory = ModuleCategory.COMBAT
)
public final class AutoGApple extends Module {

    // 1 = 0.5 сердца (1 HP), 10 = 5 сердец (10 HP, половина), 20 = полные сердца
    private final SliderSetting health =
            new SliderSetting("Здоровье", ValueUnit.abbreviation("ХП"), 10, 1, 20, 1);

    private boolean usingApple;

    @Subscribe
    private void onUpdate(EventPlayerUpdate ignored) {
        if (mc.player == null || mc.world == null) {
            stopUsing();
            return;
        }

        Item offhandItem = mc.player.getOffHandStack().getItem();
        boolean appleInOffhand =
                offhandItem == Items.GOLDEN_APPLE || offhandItem == Items.ENCHANTED_GOLDEN_APPLE;
        float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();

        if (appleInOffhand && currentHealth <= health.getFloatValue()) {
            usingApple = true;
            mc.options.useKey.setPressed(true);
        } else {
            stopUsing();
        }
    }

    private void stopUsing() {
        if (usingApple) {
            mc.options.useKey.setPressed(false);
            usingApple = false;
        }
    }

    @Override
    public void onDisable() {
        stopUsing();
        super.onDisable();
    }
}
