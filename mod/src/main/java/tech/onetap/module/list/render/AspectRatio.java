package tech.onetap.module.list.render;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "AspectRatio", moduleDesc = "Растягивает изображение под выбранное соотношение сторон", moduleCategory = ModuleCategory.RENDER)
public class AspectRatio extends Module {

    private final ModeSetting ratio = new ModeSetting("Соотношение", "16:9", "16:9", "21:9", "4:3", "Custom");
    private final SliderSetting custom = new SliderSetting("Свое", 2.0f, 1.0f, 2.4f, 0.05f)
            .setVisible(() -> ratio.is("Custom"));

    public float getTargetRatio() {
        return switch (ratio.getValue()) {
            case "16:9" -> 16f / 9f;
            case "21:9" -> 21f / 9f;
            case "4:3" -> 4f / 3f;
            default -> custom.getFloatValue();
        };
    }
}
