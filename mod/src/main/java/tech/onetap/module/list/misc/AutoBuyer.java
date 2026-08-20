package tech.onetap.module.list.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.event.list.EventRightClickBlock;
import tech.onetap.event.list.EventTick;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.render.hud.Interface;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.TextSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.player.other.SlownessManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ModuleInformation(moduleName = "Auto Buyer", moduleDesc = "Мониторит /buyer и продаёт указанные товары", moduleCategory = ModuleCategory.MISC)
public class AutoBuyer extends Module {

    private enum State { IDLE, SCAN, REOPEN_CHEST, TAKING, SELLING }

    private final SliderSetting intervalMin = new SliderSetting("Интервал проверки (мин)", 5, 1, 60, 1);
    private final SliderSetting sellDelay = new SliderSetting("Скорость продажи (мс)", 120, 40, 400, 10);
    private final BooleanSetting spamNotify = new BooleanSetting("Спам о товаре", true);
    private final TextSetting itemList = new TextSetting("Товары для продажи", "порох, слизь");

    private final Map<String, Integer> buySlots = new HashMap<>();
    private final List<String> targets = new ArrayList<>();
    private final Map<String, Boolean> knownPresence = new HashMap<>();
    private State state = State.IDLE;
    private boolean sellRequested;
    private boolean buyerOpened;
    private long lastScanAt;
    private long sellStartAt;
    private long reopenStartAt;
    private final List<String> sellItems = new ArrayList<>();
    private String currentItem;
    private long lastSellClickAt;
    private int sellClicks;
    private BlockPos lastChestPos;
    private boolean reopenScheduled;

    public static boolean isBuyerScreen(HandledScreen<?> screen) {
        if (!(screen instanceof GenericContainerScreen gcs)) return false;
        String title = gcs.getTitle().getString().toLowerCase(Locale.ROOT);
        return title.contains("покупател") || title.contains("buyer") || title.contains("huckster");
    }

    public static boolean isBusy() {
        AutoBuyer buyer = Instance.get(AutoBuyer.class);
        return buyer != null && buyer.state != State.IDLE;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        lastScanAt = System.currentTimeMillis();
        parseTargets();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        reset();
    }

    private void parseTargets() {
        targets.clear();
        for (String raw : itemList.getValue().split(",")) {
            String name = raw.trim();
            if (!name.isEmpty()) targets.add(name.toLowerCase(Locale.ROOT));
        }
    }

    private void reset() {
        state = State.IDLE;
        sellRequested = false;
        buyerOpened = false;
        currentItem = null;
        sellItems.clear();
        reopenScheduled = false;
    }

    @Subscribe
    private void onRightClickBlock(EventRightClickBlock e) {
        if (mc.world == null || e.getHitResult() == null) return;
        BlockPos pos = e.getHitResult().getBlockPos();
        BlockState blockState = mc.world.getBlockState(pos);
        if (blockState.getBlock() instanceof ChestBlock
                || blockState.getBlock() instanceof EnderChestBlock
                || blockState.getBlock() instanceof ShulkerBoxBlock) {
            lastChestPos = pos;
        }
    }

    @Subscribe
    private void onUpdate(EventPlayerUpdate e) {
        if (mc.player == null || mc.world == null) return;
        if (state != State.IDLE) return;
        if (mc.currentScreen != null) return;
        long interval = (long) (intervalMin.getValue() * 60_000);
        if (System.currentTimeMillis() - lastScanAt >= interval) {
            lastScanAt = System.currentTimeMillis();
            state = State.SCAN;
            mc.player.networkHandler.sendChatCommand("buyer");
        }
    }

    @Subscribe
    private void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        switch (state) {
            case SCAN -> onScanTick();
            case REOPEN_CHEST -> onReopenTick();
            case SELLING -> onSellTick();
            default -> {}
        }
    }

    private void onScanTick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            if (System.currentTimeMillis() - lastScanAt > 5000) {
                state = State.IDLE;
                sellRequested = false;
                Interface.NotificationManager.postWarning("Не удалось открыть /buyer");
            }
            return;
        }

        parseTargets();
        Map<String, Boolean> presence = new HashMap<>();
        for (String target : targets) presence.put(target, false);
        var handler = gcs.getScreenHandler();
        int containerSize = handler.slots.size() - 36;
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            String target = matchTarget(stack);
            if (target != null) presence.put(target, true);
        }

        List<String> gained = new ArrayList<>();
        List<String> present = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : presence.entrySet()) {
            if (!entry.getValue()) continue;
            present.add(entry.getKey());
            Boolean prev = knownPresence.get(entry.getKey());
            if (prev == null || !prev) gained.add(entry.getKey());
        }
        knownPresence.putAll(presence);

        if (spamNotify.getValue() && !present.isEmpty()) {
            String names = String.join(", ", present);
            if (!gained.isEmpty()) {
                logDirect("У покупателя появился товар: " + String.join(", ", gained), Formatting.GREEN);
            } else {
                logDirect("У покупателя есть товар: " + names, Formatting.GREEN);
            }
        }

        lastScanAt = System.currentTimeMillis();
        mc.player.closeHandledScreen();

        if (sellRequested) {
            sellRequested = false;
            if (present.isEmpty()) {
                Interface.NotificationManager.postWarning("Покупатель сейчас не принимает ваши товары");
                state = State.IDLE;
                return;
            }
            state = State.REOPEN_CHEST;
            reopenScheduled = false;
            reopenStartAt = System.currentTimeMillis();
            SlownessManager.addTimeTask(new SlownessManager.TimeTask(120, this::reopenChest, true));
        } else {
            state = State.IDLE;
        }
    }

    private void reopenChest() {
        if (mc.player == null) return;
        if (lastChestPos == null) {
            Interface.NotificationManager.postWarning("Сундук не найден — откройте сундук и нажмите «Продать всё» ещё раз");
            state = State.IDLE;
            return;
        }
        reopenScheduled = true;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                new BlockHitResult(new Vec3d(lastChestPos.getX() + 0.5, lastChestPos.getY() + 0.5, lastChestPos.getZ() + 0.5),
                        Direction.UP, lastChestPos, false));
    }

    private void onReopenTick() {
        if (!reopenScheduled) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            if (System.currentTimeMillis() - reopenStartAt > 5000) {
                state = State.IDLE;
                Interface.NotificationManager.postWarning("Не удалось открыть сундук");
            }
            return;
        }
        if (isBuyerScreen(gcs)) return;

        var handler = gcs.getScreenHandler();
        int containerSize = handler.slots.size() - 36;
        int taken = 0;
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            if (matchTarget(stack) == null) continue;
            final int slotId = i;
            SlownessManager.addTimeTask(new SlownessManager.TimeTask((long) taken * 40L, () -> {
                if (mc.player != null) {
                    InventoryUtil.clickSlotNoSync(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
            }, true));
            taken++;
        }

        if (taken == 0) {
            Interface.NotificationManager.postWarning("В сундуке нет нужных товаров");
            state = State.IDLE;
            return;
        }

        state = State.TAKING;
        long total = (long) taken * 40L + 350;
        SlownessManager.addTimeTask(new SlownessManager.TimeTask(total, this::beginSellCycle, true));
        logDirect("Забираю " + taken + " стопок, затем продаю", Formatting.GREEN);
    }

    private void beginSellCycle() {
        if (mc.player == null) return;
        state = State.SELLING;
        buyerOpened = false;
        currentItem = null;
        sellItems.clear();
        sellItems.addAll(targets);
        lastSellClickAt = 0;
        sellStartAt = System.currentTimeMillis();
        logDirect("Продажа: открываю /buyer", Formatting.GREEN);
        mc.player.closeHandledScreen();
        SlownessManager.addTimeTask(new SlownessManager.TimeTask(250, () -> {
            if (mc.player != null) mc.player.networkHandler.sendChatCommand("buyer");
        }, true));
    }

    private void onSellTick() {
        if (!(mc.currentScreen instanceof GenericContainerScreen gcs)) {
            if (System.currentTimeMillis() - sellStartAt > 5000) finishSell();
            return;
        }
        if (!buyerOpened) {
            buyerOpened = true;
            refreshBuySlots(gcs);
            currentItem = null;
            sellClicks = 0;
        }
        if (currentItem == null || countInInventory(currentItem) <= 0 || !buySlots.containsKey(currentItem)) {
            advanceItem();
        }
        if (currentItem == null) {
            finishSell();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSellClickAt >= (long) (double) sellDelay.getValue()) {
            int slotId = buySlots.get(currentItem);
            InventoryUtil.clickSlotNoSync(gcs.getScreenHandler().syncId, slotId, 1, SlotActionType.PICKUP, mc.player);
            lastSellClickAt = now;
            if (++sellClicks > 400) advanceItem();
        }
    }

    private void refreshBuySlots(GenericContainerScreen gcs) {
        buySlots.clear();
        var handler = gcs.getScreenHandler();
        int containerSize = handler.slots.size() - 36;
        for (int i = 0; i < containerSize; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            String target = matchTarget(stack);
            if (target != null) buySlots.put(target, i);
        }
    }

    private void finishSell() {
        state = State.IDLE;
        buyerOpened = false;
        currentItem = null;
        sellItems.clear();
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
        }
        Interface.NotificationManager.postWarning("Продажа завершена");
    }

    private void advanceItem() {
        sellClicks = 0;
        for (String target : sellItems) {
            if (buySlots.containsKey(target) && countInInventory(target) > 0) {
                currentItem = target;
                logDirect("Продаю: " + target + " (" + countInInventory(target) + " шт)", Formatting.GREEN);
                return;
            }
        }
        currentItem = null;
    }

    public void sellFromChest(HandledScreen<?> chestScreen) {
        if (mc.player == null) return;
        if (state != State.IDLE) {
            Interface.NotificationManager.postWarning("Auto Buyer уже работает");
            return;
        }
        logDirect("Продажа: открываю /buyer для проверки", Formatting.GREEN);
        state = State.SCAN;
        sellRequested = true;
        mc.player.closeHandledScreen();
        SlownessManager.addTimeTask(new SlownessManager.TimeTask(200, () -> {
            if (mc.player != null) mc.player.networkHandler.sendChatCommand("buyer");
        }, true));
    }

    private int countInInventory(String target) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && target.equals(matchTarget(stack))) count += stack.getCount();
        }
        return count;
    }

    private String matchTarget(ItemStack stack) {
        String name = normalize(stack);
        for (String target : targets) {
            if (name.equals(target) || name.contains(target) || target.contains(name)) return target;
        }
        return null;
    }

    private static String normalize(ItemStack stack) {
        String name = stack.getName().getString();
        String stripped = Formatting.strip(name);
        if (stripped == null || stripped.isEmpty()) stripped = name;
        return stripped.toLowerCase(Locale.ROOT).trim();
    }
}
