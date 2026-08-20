package tech.onetap.module.list.player;

import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;

@ModuleInformation(moduleName = "Cords Dropper", moduleCategory = ModuleCategory.PLAYER)
public class CordsDropper extends Module {

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player != null) {
            String message = String.format("! All %.0f %.0f %.0f Helpa",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            mc.player.networkHandler.sendChatMessage(message);
        }
        mc.execute(() -> setEnabled(false));
    }
}
