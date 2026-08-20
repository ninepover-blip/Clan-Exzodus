package tech.onetap.util.gui;

import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.module.list.misc.AutoBuyer;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.base.Instance;
import tech.onetap.util.cursor.CursorManager;
import tech.onetap.util.player.other.InventoryUtil;
import tech.onetap.util.player.other.SlownessManager;
import tech.onetap.util.render.helper.HoverUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class InventoryButtons implements IMinecraft {
    private static final long CLICK_DELAY = 40;
    private static final float BTN_H = 21f;
    private static final float GAP = 6f;
    private static long busyUntil;

    private record Btn(String label, Runnable action) {}

    public static boolean isChestScreen(HandledScreen<?> screen) {
        return screen instanceof GenericContainerScreen || screen instanceof ShulkerBoxScreen;
    }

    public static void render(HandledScreen<?> screen, net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, int screenY) {
        if (!(screen instanceof InventoryScreen) && !isChestScreen(screen)) return;
        if (AutoBuyer.isBuyerScreen(screen)) return;
        List<Btn> buttons = buildButtons(screen);
        if (buttons.isEmpty()) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        List<float[]> bounds = layout(screen, buttons, w, h, screenY);

        for (int i = 0; i < buttons.size(); i++) {
            float[] b = bounds.get(i);
            boolean hovered = HoverUtil.isHovered(mouseX, mouseY, b[0], b[1], b[2], BTN_H);
            DrawUtil.drawRoundBlur(b[0] - 2f, b[1] - 2f, b[2] + 4f, BTN_H + 4f, 7f,
                    ColorProvider.setAlpha(ColorProvider.getThemeColor(), hovered ? 210 : 150), 10f);
            DrawUtil.drawRound(b[0], b[1], b[2], BTN_H, 6f, ColorProvider.rgba(9, 10, 16, 235));
            DrawUtil.drawRound(b[0] - 0.5f, b[1] - 0.5f, b[2] + 1f, BTN_H + 1f, 4.5f,
                    hovered ? ColorProvider.setAlpha(ColorProvider.getThemeColor(), 200) : ColorProvider.setAlpha(ColorProvider.getThemeColor(), 90));
            if (hovered) {
                DrawUtil.drawRound(b[0], b[1], b[2], BTN_H, 4f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), 60));
                CursorManager.requestHand();
            }
            float textW = Fonts.SFBOLD.get().getWidth(buttons.get(i).label, 8.5f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), buttons.get(i).label,
                    b[0] + (b[2] - textW) / 2f, b[1] + 5.4f,
                    ColorProvider.rgba(255, 255, 255, 255), 8.5f);
        }
    }

    public static boolean onMouseClicked(HandledScreen<?> screen, double mouseX, double mouseY, int button, int screenY) {
        if (button != 0) return false;
        if (!(screen instanceof InventoryScreen) && !isChestScreen(screen)) return false;
        if (AutoBuyer.isBuyerScreen(screen)) return false;
        List<Btn> buttons = buildButtons(screen);
        if (buttons.isEmpty()) return false;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        List<float[]> bounds = layout(screen, buttons, w, h, screenY);

        for (int i = 0; i < buttons.size(); i++) {
            float[] b = bounds.get(i);
            if (HoverUtil.isHovered(mouseX, mouseY, b[0], b[1], b[2], BTN_H)) {
                buttons.get(i).action().run();
                return true;
            }
        }
        return false;
    }

    private static List<Btn> buildButtons(HandledScreen<?> screen) {
        List<Btn> out = new ArrayList<>();
        if (screen instanceof InventoryScreen) {
            out.add(new Btn("Выбросить всё", () -> dropAll(screen)));
            return out;
        }
        if (!isChestScreen(screen)) return out;

        out.add(new Btn("Выбросить всё", () -> dropAll(screen)));
        out.add(new Btn("Забрать всё", () -> takeAll(screen)));
        out.add(new Btn("Сложить всё", () -> storeAll(screen)));

        AutoBuyer buyer = Instance.get(AutoBuyer.class);
        if (buyer != null && buyer.isEnabled()) {
            out.add(new Btn("Продать всё", () -> buyer.sellFromChest(screen)));
        }
        return out;
    }

    private static List<float[]> layout(HandledScreen<?> screen, List<Btn> buttons, int w, int h, int screenY) {
        List<List<float[]>> rows = new ArrayList<>();
        List<float[]> row = new ArrayList<>();
        float rowW = 0;
        for (Btn b : buttons) {
            float bw = Fonts.SFBOLD.get().getWidth(b.label, 8.5f) + 24f;
            if (!row.isEmpty() && rowW + GAP + bw > w - 24f) {
                rows.add(row);
                row = new ArrayList<>();
                rowW = 0;
            }
            row.add(new float[]{bw});
            rowW += (row.size() == 1 ? 0 : GAP) + bw;
        }
        if (!row.isEmpty()) rows.add(row);

        float totalH = rows.size() * BTN_H + Math.max(0, rows.size() - 1) * GAP;
        float y = h - 10f - totalH;
        if (isChestScreen(screen) && !screen.getScreenHandler().slots.isEmpty()) {
            y = Math.max(6f, screenY - totalH - 14f);
        }
        List<float[]> out = new ArrayList<>();
        for (List<float[]> r : rows) {
            float totalW = -GAP;
            for (float[] f : r) totalW += f[0] + GAP;
            float x = (w - totalW) / 2f;
            for (float[] f : r) {
                out.add(new float[]{x, y, f[0]});
                x += f[0] + GAP;
            }
            y += BTN_H + GAP;
        }
        return out;
    }

    private static int playerRegionStart(HandledScreen<?> screen) {
        if (screen instanceof InventoryScreen) return 9;
        return screen.getScreenHandler().slots.size() - 36;
    }

    private static void dropAll(HandledScreen<?> screen) {
        if (System.currentTimeMillis() < busyUntil) return;
        var handler = screen.getScreenHandler();
        int start;
        if (screen instanceof InventoryScreen) {
            start = 9;
        } else {
            start = 0;
        }
        int count = 0;
        for (int i = start; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isEmpty()) continue;
            final int slotId = i;
            queueClick(handler.syncId, slotId, 1, SlotActionType.THROW, (long) count * CLICK_DELAY);
            count++;
        }
        if (count > 0) busyUntil = System.currentTimeMillis() + count * CLICK_DELAY + 400;
    }

    private static void takeAll(HandledScreen<?> screen) {
        if (System.currentTimeMillis() < busyUntil) return;
        var handler = screen.getScreenHandler();
        int containerSize = handler.slots.size() - 36;
        int count = 0;
        for (int i = 0; i < containerSize; i++) {
            if (handler.getSlot(i).getStack().isEmpty()) continue;
            queueClick(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, (long) count * CLICK_DELAY);
            count++;
        }
        if (count > 0) busyUntil = System.currentTimeMillis() + count * CLICK_DELAY + 400;
    }

    private static void storeAll(HandledScreen<?> screen) {
        if (System.currentTimeMillis() < busyUntil) return;
        var handler = screen.getScreenHandler();
        int start = playerRegionStart(screen);
        int count = 0;
        for (int i = start; i < handler.slots.size(); i++) {
            if (handler.getSlot(i).getStack().isEmpty()) continue;
            queueClick(handler.syncId, i, 0, SlotActionType.QUICK_MOVE, (long) count * CLICK_DELAY);
            count++;
        }
        if (count > 0) busyUntil = System.currentTimeMillis() + count * CLICK_DELAY + 400;
    }

    private static void queueClick(int syncId, int slotId, int button, SlotActionType type, long delayMs) {
        SlownessManager.addTimeTask(new SlownessManager.TimeTask(delayMs, () -> {
            if (mc.player == null) return;
            InventoryUtil.clickSlotNoSync(syncId, slotId, button, type, mc.player);
        }, true));
    }
}
