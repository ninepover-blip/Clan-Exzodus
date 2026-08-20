package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.SliderSetting;

import java.util.Locale;

@ModuleInformation(moduleName = "SantaCrit", moduleDesc = "На удар меняет шлем Санты на Нерушимый шлем", moduleCategory = ModuleCategory.COMBAT)
public class SantaCrit extends Module {
    private final SliderSetting returnDelay = new SliderSetting("Задержка возврата", 1f, 1f, 5f, 1f);
    private int unbreakableSlot = -1;
    private int restoreTicks;

    @Subscribe
    private void onAttack(EventAttack ignored) {
        if (mc.player == null || mc.interactionManager == null || restoreTicks > 0) return;
        if (!isSantaHelmet(mc.player.getEquippedStack(EquipmentSlot.HEAD))) return;
        unbreakableSlot = findInventorySlot(true);
        if (unbreakableSlot == -1) return;
        swapWithHelmet(unbreakableSlot);
        restoreTicks = returnDelay.getIntValue();
    }

    @Subscribe
    private void onTick(EventTick ignored) {
        if (restoreTicks <= 0 || --restoreTicks > 0 || unbreakableSlot == -1) return;
        swapWithHelmet(unbreakableSlot);
        unbreakableSlot = -1;
    }

    @Override
    public void onDisable() {
        if (unbreakableSlot != -1 && restoreTicks > 0 && mc.player != null && mc.interactionManager != null) {
            swapWithHelmet(unbreakableSlot);
        }
        unbreakableSlot = -1;
        restoreTicks = 0;
        super.onDisable();
    }

    private int findInventorySlot(boolean unbreakable) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (unbreakable ? isUnbreakableHelmet(stack) : isSantaHelmet(stack)) return slot < 9 ? slot + 36 : slot;
        }
        return -1;
    }

    static boolean isSantaHelmet(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("санта") || name.contains("santa")
                || name.contains("шлем сант") || name.contains("шапка сант");
    }

    static boolean isUnbreakableHelmet(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("неруш") || name.contains("unbreak");
    }

    private void swapWithHelmet(int inventorySlot) {
        // EventAttack is fired at the HEAD of attackEntity. Send all inventory
        // clicks immediately here so the server receives the helmet swap before
        // the attack packet; scheduled GUI bypass tasks are too late for a crit.
        mc.interactionManager.clickSlot(0, inventorySlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, 5, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, inventorySlot, 0, SlotActionType.PICKUP, mc.player);
    }
}
