package tech.onetap.module.list.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.misc.AutoBuyer;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.gui.InventoryButtons;
import tech.onetap.util.player.other.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "Chest Stealer", moduleDesc = "Автоматический забор предметов из сундуков", moduleCategory = ModuleCategory.PLAYER)
public class ChestStealer extends Module {

    private final SliderSetting delay = new SliderSetting("Задержка (мс)", 80, 20, 300, 10);
    private final BooleanSetting closeWhenEmpty = new BooleanSetting("Закрывать когда пусто", true);

    private final List<Integer> queue = new ArrayList<>();
    private long lastClickAt;
    private int syncId = -1;

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.currentScreen instanceof HandledScreen<?> screen)) {
            queue.clear();
            syncId = -1;
            return;
        }
        if (!InventoryButtons.isChestScreen(screen) || AutoBuyer.isBuyerScreen(screen) || AutoBuyer.isBusy()) {
            queue.clear();
            syncId = -1;
            return;
        }

        var handler = screen.getScreenHandler();
        if (syncId != handler.syncId) {
            syncId = handler.syncId;
            queue.clear();
            int containerSize = handler.slots.size() - 36;
            for (int i = 0; i < containerSize; i++) {
                if (!handler.getSlot(i).getStack().isEmpty()) {
                    queue.add(i);
                }
            }
            lastClickAt = 0;
        }

        if (queue.isEmpty()) {
            if (closeWhenEmpty.getValue()) mc.player.closeHandledScreen();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastClickAt >= (long) (double) delay.getValue()) {
            int slotId = queue.remove(0);
            InventoryUtil.clickSlotNoSync(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
            lastClickAt = now;
        }
    }
}
