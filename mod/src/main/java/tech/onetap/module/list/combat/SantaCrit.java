package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import tech.onetap.event.list.EventAttack;
import tech.onetap.event.list.EventKeyInput;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.Inputs;
import tech.onetap.util.chat.ChatUtil;

@ModuleInformation(moduleName = "SantaCrit", moduleDesc = "На удар одевает привязанный предмет вместо шлема и возвращает обратно", moduleCategory = ModuleCategory.COMBAT)
public class SantaCrit extends Module {
    /** Слот шлема в контейнере игрока (1.21.x: HEAD = 5). */
    private static final int HEAD_CONTAINER_SLOT = 5;

    private final SliderSetting returnDelay = new SliderSetting("Задержка возврата", 1f, 1f, 5f, 1f);
    private final BooleanSetting selectItem = new BooleanSetting("Выбрать предмет", false);

    private ItemStack boundItem = ItemStack.EMPTY;
    private String boundName = "";
    private boolean armed;
    private int swapSlot = -1;
    private int restoreTicks;

    @Subscribe
    private void onTick(EventTick ignored) {
        if (mc.player == null || mc.interactionManager == null) return;

        if (selectItem.getValue()) {
            selectItem.setValue(false);
            armed = true;
            ChatUtil.send("Возьмите предмет в основную руку и нажмите ПКМ, чтобы привязать его");
        }

        if (armed) return;

        if (restoreTicks > 0 && --restoreTicks <= 0 && swapSlot != -1) {
            restore();
        }
    }

    @Subscribe
    private void onKey(EventKeyInput event) {
        if (!armed) return;
        if (event.getAction() != 1) return;
        if (event.getKey() != Inputs.mouseButtonCode(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return;
        if (mc.currentScreen != null) return;

        ItemStack hand = mc.player.getMainHandStack();
        if (hand.isEmpty()) {
            ChatUtil.send("В руке пусто. Возьмите предмет в основную руку и нажмите ПКМ");
            return;
        }

        boundItem = hand.copy();
        boundName = hand.getName().getString();
        armed = false;
        ChatUtil.send("Привязан предмет: " + boundName);
    }

    @Subscribe
    private void onAttack(EventAttack ignored) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (armed || swapSlot != -1 || restoreTicks > 0) return;
        if (boundItem.isEmpty()) return;

        int slot = findBoundSlot();
        if (slot == -1) return;

        swapWithHelmet(slot);
        swapSlot = slot;
        restoreTicks = returnDelay.getIntValue();
    }

    @Override
    public void onDisable() {
        if (swapSlot != -1) {
            restore();
        }
        armed = false;
        super.onDisable();
    }

    private void restore() {
        if (swapSlot != -1 && mc.player != null && mc.interactionManager != null) {
            swapWithHelmet(swapSlot);
        }
        swapSlot = -1;
        restoreTicks = 0;
    }

    private int findBoundSlot() {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, boundItem)) {
                return slot < 9 ? slot + 36 : slot;
            }
        }
        return -1;
    }

    private void swapWithHelmet(int inventorySlot) {
        // EventAttack is fired at the HEAD of attackEntity. Send all inventory
        // clicks immediately here so the server receives the helmet swap before
        // the attack packet; scheduled GUI bypass tasks are too late for a crit.
        mc.interactionManager.clickSlot(0, inventorySlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, HEAD_CONTAINER_SLOT, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, inventorySlot, 0, SlotActionType.PICKUP, mc.player);
    }
}