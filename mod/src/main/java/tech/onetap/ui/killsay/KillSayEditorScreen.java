package tech.onetap.ui.killsay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import tech.onetap.module.list.misc.KillSay;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.killsay.KillSayRepository;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.msdf.MsdfFont;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.IMinecraft;

import java.util.ArrayList;
import java.util.List;

/** Меню настройки фраз KillSay (открывается по ПКМ). */
public class KillSayEditorScreen extends Screen implements IMinecraft {

    private static final float WIDTH = 470f;
    private static final float HEIGHT = 370f;
    private static final float ROW_HEIGHT = 18f;

    private final KillSay module;
    private boolean totemTab = false;

    private String input = "";
    private int cursorPos = 0;
    private boolean inputFocused = true;
    private String editingMessage = null;
    private float scroll = 0f;

    public KillSayEditorScreen(KillSay module) {
        super(Text.of("KillSay Editor"));
        this.module = module;
    }

    private float panelX() {
        return (mc.getWindow().getScaledWidth() - WIDTH) / 2f;
    }

    private float panelY() {
        return (mc.getWindow().getScaledHeight() - HEIGHT) / 2f;
    }

    // ---------- layout helpers ----------

    private float listX() {
        return panelX() + 12f;
    }

    private float listY() {
        return panelY() + 108f;
    }

    private float listW() {
        return WIDTH - 24f;
    }

    private float listH() {
        return HEIGHT - 108f - 62f;
    }

    private ModeSetting currentMode() {
        return totemTab ? module.totemMode : module.killMode;
    }

    private List<String> combinedMessages() {
        List<String> result = new ArrayList<>();
        String[] presets = totemTab
                ? tech.onetap.util.killsay.KillSayPresets.TOTEM_MESSAGES
                : tech.onetap.util.killsay.KillSayPresets.KILL_MESSAGES;
        result.addAll(List.of(presets));
        result.addAll(totemTab
                ? KillSayRepository.getCustomTotemMessages()
                : KillSayRepository.getCustomKillMessages());
        return result;
    }

    private boolean isCustom(String message) {
        return totemTab
                ? KillSayRepository.getCustomTotemMessages().contains(message)
                : KillSayRepository.getCustomKillMessages().contains(message);
    }

    private float contentHeight() {
        return combinedMessages().size() * ROW_HEIGHT;
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight() - listH());
    }

    // ---------- input text ----------

    private void insertChar(char chr) {
        if (cursorPos > input.length()) cursorPos = input.length();
        input = input.substring(0, cursorPos) + chr + input.substring(cursorPos);
        cursorPos++;
    }

    private void deleteBeforeCursor() {
        if (cursorPos <= 0) return;
        input = input.substring(0, cursorPos - 1) + input.substring(cursorPos);
        cursorPos--;
    }

    private void deleteAtCursor() {
        if (cursorPos >= input.length()) return;
        input = input.substring(0, cursorPos) + input.substring(cursorPos + 1);
    }

    private void commitInput() {
        String text = input.trim();
        if (text.isEmpty()) return;
        if (editingMessage != null) {
            if (totemTab) {
                KillSayRepository.editTotemMessage(editingMessage, text);
            } else {
                KillSayRepository.editKillMessage(editingMessage, text);
            }
            editingMessage = null;
        } else {
            if (totemTab) {
                KillSayRepository.addTotemMessage(text);
            } else {
                KillSayRepository.addKillMessage(text);
            }
        }
        input = "";
        cursorPos = 0;
    }

    private void startEdit(String message) {
        editingMessage = message;
        input = message;
        cursorPos = message.length();
        inputFocused = true;
    }

    // ---------- input events ----------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float x = (float) mouseX;
        float y = (float) mouseY;

        if (inside(x, y, panelX() + WIDTH - 28f, panelY() + 10f, 18f, 18f)) {
            mc.setScreen(null);
            return true;
        }

        // tabs
        if (inside(x, y, panelX() + 12f, panelY() + 42f, 200f, 22f)) {
            totemTab = false;
            scroll = 0f;
            editingMessage = null;
            return true;
        }
        if (inside(x, y, panelX() + 222f, panelY() + 42f, 200f, 22f)) {
            totemTab = true;
            scroll = 0f;
            editingMessage = null;
            return true;
        }

        // mode buttons
        float modeX = panelX() + 86f;
        for (String mode : List.of("Пресеты", "Кастомные", "Смешанные")) {
            if (inside(x, y, modeX, panelY() + 76f, 92f, 22f)) {
                currentMode().setValue(mode);
                return true;
            }
            modeX += 96f;
        }

        // input row buttons
        float inputY = panelY() + HEIGHT - 46f;
        if (inside(x, y, panelX() + WIDTH - 104f, inputY, 92f, 24f)) {
            commitInput();
            return true;
        }
        if (editingMessage != null && inside(x, y, panelX() + WIDTH - 120f, inputY - 24f, 56f, 16f)) {
            editingMessage = null;
            input = "";
            cursorPos = 0;
            return true;
        }
        if (inside(x, y, panelX() + 12f, inputY, WIDTH - 128f, 24f)) {
            inputFocused = true;
            return true;
        }

        // messages list
        float rowY = listY() - scroll;
        int index = 0;
        for (String message : combinedMessages()) {
            if (rowY + ROW_HEIGHT < listY() || rowY > listY() + listH()) {
                rowY += ROW_HEIGHT;
                index++;
                continue;
            }
            boolean custom = isCustom(message);
            float rowX = listX();
            if (inside(x, y, rowX, rowY, listW(), ROW_HEIGHT)) {
                if (custom) {
                    float editX = rowX + listW() - 46f;
                    float delX = rowX + listW() - 23f;
                    if (inside(x, y, editX, rowY + 1f, 20f, 16f)) {
                        startEdit(message);
                        return true;
                    }
                    if (inside(x, y, delX, rowY + 1f, 20f, 16f)) {
                        if (totemTab) KillSayRepository.removeTotemMessage(message);
                        else KillSayRepository.removeKillMessage(message);
                        if (message.equals(editingMessage)) {
                            editingMessage = null;
                            input = "";
                            cursorPos = 0;
                        }
                        return true;
                    }
                }
                if (custom) {
                    startEdit(message);
                }
                return true;
            }
            rowY += ROW_HEIGHT;
            index++;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside((float) mouseX, (float) mouseY, listX(), listY(), listW(), listH())) {
            float delta = (float) (-verticalAmount * 14.0);
            scroll = Math.max(0f, Math.min(maxScroll(), scroll + delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (inputFocused && chr >= ' ' && chr != 127) {
            insertChar(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            mc.setScreen(null);
            return true;
        }
        if (!inputFocused) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> deleteBeforeCursor();
            case GLFW.GLFW_KEY_DELETE -> deleteAtCursor();
            case GLFW.GLFW_KEY_LEFT -> cursorPos = Math.max(0, cursorPos - 1);
            case GLFW.GLFW_KEY_RIGHT -> cursorPos = Math.min(input.length(), cursorPos + 1);
            case GLFW.GLFW_KEY_HOME -> cursorPos = 0;
            case GLFW.GLFW_KEY_END -> cursorPos = input.length();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> commitInput();
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    // ---------- render ----------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        DrawUtil.drawRound(0, 0, sw, sh, 0,
                ColorProvider.rgba(6, 8, 12, 150),
                ColorProvider.rgba(10, 12, 18, 150));

        float x = panelX();
        float y = panelY();

        DrawUtil.drawRoundBlur(x, y, WIDTH, HEIGHT, 8f, ColorProvider.rgba(0, 0, 0, 150), 14f);
        DrawUtil.drawRound(x, y, WIDTH, HEIGHT, 8f, ColorProvider.rgba(14, 16, 22, 245));
        DrawUtil.drawRound(x, y, WIDTH, HEIGHT, 8f, ColorProvider.rgba(255, 255, 255, 12));
        DrawUtil.drawRound(x, y + 6f, 2.5f, HEIGHT - 12f, 1.5f, ColorProvider.getThemeColor());

        MsdfFont bold = Fonts.SFBOLD.get();
        MsdfFont medium = Fonts.SFMEDIUM.get();
        MsdfFont regular = Fonts.SFREGULAR.get();

        // header
        DrawUtil.drawText(bold, "KillSay", x + 14f, y + 8f,
                ColorProvider.rgba(245, 247, 255, 255), 13f);
        DrawUtil.drawText(regular, "Настройка фраз (открыто по ПКМ)", x + 74f, y + 14f,
                ColorProvider.rgba(150, 157, 172, 255), 7f);
        drawButton(x + WIDTH - 28f, y + 10f, 18f, 18f, "✕", 7f, isHover(mouseX, mouseY, x + WIDTH - 28f, y + 10f, 18f, 18f));

        // tabs
        drawTab(x + 12f, y + 42f, 200f, 22f, "Убийство", !totemTab);
        drawTab(x + 222f, y + 42f, 200f, 22f, "Тотем", totemTab);

        // mode row
        DrawUtil.drawText(regular, "Источник:", x + 14f, y + 80f,
                ColorProvider.rgba(160, 167, 182, 255), 7.5f);
        float modeX = x + 86f;
        String selectedMode = currentMode().getValue();
        for (String mode : List.of("Пресеты", "Кастомные", "Смешанные")) {
            drawModeButton(modeX, y + 76f, 92f, 22f, mode, mode.equals(selectedMode));
            modeX += 96f;
        }

        // messages list
        List<String> messages = combinedMessages();
        DrawUtil.drawRound(listX() - 3f, listY() - 3f, listW() + 6f, listH() + 6f, 6f,
                ColorProvider.rgba(8, 9, 13, 120));

        Scissor.push();
        Scissor.setFromComponentCoordinates(listX(), listY(), listW(), listH());

        float rowY = listY() - scroll;
        float textScale = 7f;
        for (String message : messages) {
            if (rowY + ROW_HEIGHT < listY() || rowY > listY() + listH()) {
                rowY += ROW_HEIGHT;
                continue;
            }
            boolean custom = isCustom(message);
            float rowX = listX();
            if (rowY >= listY() && rowY + ROW_HEIGHT <= listY() + listH()) {
                DrawUtil.drawRound(rowX, rowY + 1f, listW(), ROW_HEIGHT - 2f, 3f,
                        ColorProvider.rgba(255, 255, 255, custom ? 18 : 8));
            }

            String prefix = custom ? "[C] " : "[П] ";
            String display = message;
            float prefixW = regular.getWidth(prefix, textScale);
            int textColor = custom
                    ? ColorProvider.rgba(235, 235, 245, 255)
                    : ColorProvider.rgba(175, 182, 198, 255);
            DrawUtil.drawText(regular, prefix, rowX + 3f, rowY + 4.5f,
                    custom ? ColorProvider.rgba(94, 214, 128, 255) : ColorProvider.rgba(120, 128, 148, 255), textScale);

            float availableWidth = custom ? listW() - 54f : listW() - 8f;
            if (message.equals(editingMessage)) {
                DrawUtil.drawText(medium, display, rowX + 3f + prefixW, rowY + 4.5f,
                        ColorProvider.rgba(255, 210, 80, 255), textScale);
            } else {
                DrawUtil.drawText(medium, display, rowX + 3f + prefixW, rowY + 4.5f, textColor, textScale);
            }

            if (custom) {
                float editX = rowX + listW() - 46f;
                float delX = rowX + listW() - 23f;
                drawButton(editX, rowY + 1f, 20f, 16f, "✎", 6.5f, isHover(mouseX, mouseY, editX, rowY + 1f, 20f, 16f));
                drawButton(delX, rowY + 1f, 20f, 16f, "✕", 6.5f, isHover(mouseX, mouseY, delX, rowY + 1f, 20f, 16f));
            }
            rowY += ROW_HEIGHT;
        }

        Scissor.unset();
        Scissor.pop();

        DrawUtil.drawText(regular, "Всего: " + messages.size(), x + 14f, y + HEIGHT - 66f,
                ColorProvider.rgba(140, 147, 162, 255), 6.5f);

        // input row
        float inputY = y + HEIGHT - 46f;
        boolean editing = editingMessage != null;
        DrawUtil.drawRound(x + 12f, inputY, WIDTH - 128f, 24f, 5f,
                inputFocused ? ColorProvider.rgba(30, 33, 43, 255) : ColorProvider.rgba(22, 24, 32, 255));
        DrawUtil.drawRound(x + 12f, inputY, WIDTH - 128f, 24f, 5f,
                ColorProvider.rgba(255, 255, 255, inputFocused ? 18 : 8));

        String displayInput = input;
        if (displayInput.isEmpty()) {
            DrawUtil.drawText(regular, editing ? "Введите изменённую фразу..." : "Введите свою фразу...", x + 18f, inputY + 7.5f,
                    ColorProvider.rgba(110, 117, 132, 255), 7f);
        } else {
            String beforeCursor = input.substring(0, Math.min(cursorPos, input.length()));
            float textX = x + 18f;
            float caretW = medium.getWidth(beforeCursor, 7f);
            DrawUtil.drawText(medium, displayInput, textX, inputY + 7.5f,
                    ColorProvider.rgba(240, 242, 250, 255), 7f);
            if (inputFocused && (System.currentTimeMillis() / 500L) % 2 == 0) {
                DrawUtil.drawRound(textX + caretW + 1f, inputY + 5.5f, 1f, 13f, 0.5f,
                        ColorProvider.rgba(255, 255, 255, 200));
            }
        }

        String actionText = editing ? "Сохранить" : "Добавить";
        drawButton(x + WIDTH - 104f, inputY, 92f, 24f, actionText, 7.5f,
                isHover(mouseX, mouseY, x + WIDTH - 104f, inputY, 92f, 24f), true);

        if (editing) {
            DrawUtil.drawText(regular, "Отмена [✕]", x + WIDTH - 120f, inputY - 26f,
                    ColorProvider.rgba(255, 96, 96, 255), 6.5f);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private boolean isHover(int mouseX, int mouseY, float rx, float ry, float rw, float rh) {
        return inside(mouseX, mouseY, rx, ry, rw, rh);
    }

    private void drawTab(float x, float y, float w, float h, String label, boolean active) {
        DrawUtil.drawRound(x, y, w, h, 6f,
                active ? ColorProvider.rgba(38, 42, 54, 255) : ColorProvider.rgba(20, 22, 30, 255));
        DrawUtil.drawRound(x, y + h - 2f, w, 2f, 1f,
                active ? ColorProvider.getThemeColor() : ColorProvider.rgba(40, 44, 56, 255));
        MsdfFont font = active ? Fonts.SFBOLD.get() : Fonts.SFMEDIUM.get();
        float textW = font.getWidth(label, 8f);
        DrawUtil.drawText(font, label, x + (w - textW) / 2f, y + (h - 8f) / 2f + 1f,
                active ? ColorProvider.rgba(245, 247, 255, 255) : ColorProvider.rgba(140, 147, 162, 255), 8f);
    }

    private void drawModeButton(float x, float y, float w, float h, String label, boolean active) {
        DrawUtil.drawRound(x, y, w, h, 5f,
                active ? ColorProvider.setAlpha(ColorProvider.getThemeColor(), 150) : ColorProvider.rgba(24, 26, 35, 255));
        DrawUtil.drawRound(x, y, w, h, 5f,
                ColorProvider.rgba(255, 255, 255, active ? 20 : 8));
        MsdfFont font = active ? Fonts.SFBOLD.get() : Fonts.SFMEDIUM.get();
        float textW = font.getWidth(label, 7f);
        DrawUtil.drawText(font, label, x + (w - textW) / 2f, y + (h - 7f) / 2f + 1f,
                active ? ColorProvider.rgba(255, 255, 255, 255) : ColorProvider.rgba(175, 182, 198, 255), 7f);
    }

    private void drawButton(float x, float y, float w, float h, String label, float scale, boolean hovered) {
        drawButton(x, y, w, h, label, scale, hovered, false);
    }

    private void drawButton(float x, float y, float w, float h, String label, float scale, boolean hovered, boolean accent) {
        int bg = accent
                ? (hovered ? ColorProvider.rgba(120, 60, 140, 255) : ColorProvider.setAlpha(ColorProvider.getThemeColor(), 200))
                : (hovered ? ColorProvider.rgba(52, 57, 72, 255) : ColorProvider.rgba(30, 33, 43, 255));
        DrawUtil.drawRound(x, y, w, h, 5f, bg);
        float textW = Fonts.SFMEDIUM.get().getWidth(label, scale);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), label, x + (w - textW) / 2f, y + (h - scale) / 2f,
                ColorProvider.rgba(245, 247, 255, 255), scale);
    }

    private boolean inside(float px, float py, float rx, float ry, float rw, float rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void applyBlur() {
    }

    @Override
    public void blur() {
    }
}