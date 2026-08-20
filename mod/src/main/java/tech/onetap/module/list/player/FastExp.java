package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

@ModuleInformation(moduleName = "Fast Exp", moduleCategory = ModuleCategory.PLAYER)
public class FastExp extends Module {
    public final SliderSetting delay = new SliderSetting("Задержка",0,0,10,1);
    @Subscribe
    private void onPlayerUpdate(EventPlayerUpdate e) {
        if (!(mc.player.getMainHandStack().getItem() == Items.EXPERIENCE_BOTTLE || mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE)) return;

        mc.itemUseCooldown = delay.getIntValue();
    }
}