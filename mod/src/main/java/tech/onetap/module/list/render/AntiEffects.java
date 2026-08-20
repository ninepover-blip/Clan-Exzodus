package tech.onetap.module.list.render;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;

@ModuleInformation(moduleName = "AntiEffects", moduleDesc = "Скрытие эффектов зелий", moduleCategory = ModuleCategory.RENDER)
public class AntiEffects extends Module {

    private final BooleanSetting hud = new BooleanSetting("HUD эффекты", true);
    private final BooleanSetting inventory = new BooleanSetting("Инвентарь", true);

    public boolean shouldHideHud() {
        return isEnabled() && hud.getValue();
    }

    public boolean shouldHideInventory() {
        return isEnabled() && inventory.getValue();
    }
}
