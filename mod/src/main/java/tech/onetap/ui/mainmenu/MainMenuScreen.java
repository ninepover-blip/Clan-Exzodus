package tech.onetap.ui.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import tech.onetap.Onetap;
import tech.onetap.util.IMinecraft;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MainMenuScreen extends Screen implements IMinecraft {

    private static final Identifier MENU_BG = Identifier.of("mre", "images/infynyty-menu.png");
    private static final int INFINYTY_COLOR_1 = ColorProvider.rgba(74, 144, 226, 255);
    private static final int INFINYTY_COLOR_2 = ColorProvider.rgba(144, 19, 254, 255);

    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private final Animation fadeIn = new Animation(Easing.QUINTIC_OUT, 700);

    private long initTime;

    public MainMenuScreen() {
        super(Text.of(""));
    }

    @Override
    protected void init() {
        super.init();
        initTime = System.currentTimeMillis();
        fadeIn.setValue(0f);
        fadeIn.run(1f);

        buttons.clear();

        float widthButton = 160f;
        float heightButton = 24f;
        float spacing = 6f;
        float totalHeight = heightButton * 4 + spacing * 3;
        float x = (mc.getWindow().getScaledWidth() - widthButton) / 2f;
        float y = (mc.getWindow().getScaledHeight() - totalHeight) / 2f + 10;

        buttons.add(new MenuButton(x, y, widthButton, heightButton, "Одиночная игра",
                () -> mc.setScreen(new SelectWorldScreen(this))));
        y += heightButton + spacing;
        buttons.add(new MenuButton(x, y, widthButton, heightButton, "Сервера",
                () -> mc.setScreen(new MultiplayerScreen(this))));
        y += heightButton + spacing;
        buttons.add(new MenuButton(x, y, widthButton, heightButton, "Настройки",
                () -> mc.setScreen(new OptionsScreen(this, mc.options))));
        y += heightButton + spacing;
        buttons.add(new MenuButton(x, y, widthButton, heightButton, "Выход",
                () -> mc.scheduleStop()));

        if (particles.isEmpty()) {
            for (int i = 0; i < 40; i++) {
                particles.add(new Particle(
                        ThreadLocalRandom.current().nextFloat() * mc.getWindow().getScaledWidth(),
                        ThreadLocalRandom.current().nextFloat() * mc.getWindow().getScaledHeight(),
                        ThreadLocalRandom.current().nextFloat() * 1.5f + 0.5f,
                        ThreadLocalRandom.current().nextFloat() * 0.4f + 0.1f
                ));
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int width = mc.getWindow().getScaledWidth();
        int height = mc.getWindow().getScaledHeight();

        try {
            AbstractTexture bgTex = mc.getTextureManager().getTexture(MENU_BG);
            Builder.texture()
                .size(new SizeState(width, height))
                .radius(new QuadRadiusState(0))
                .color(new QuadColorState(ColorProvider.rgba(255, 255, 255, 200)))
                .texture(0, 0, 1, 1, bgTex)
                .smoothness(1f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), 0, 0, 0);
        } catch (Exception e) {
            // fallback to gradient
        }

        DrawUtil.drawRound(0, 0, width, height, 0,
                ColorProvider.rgba(10, 5, 22, 180),
                ColorProvider.rgba(22, 8, 40, 180));

        super.render(context, mouseX, mouseY, delta);

        fadeIn.run();
        float fade = fadeIn.getValue();

        renderParticles(context, delta);

        float logoScale = 1.0f + (float) Math.sin((System.currentTimeMillis() - initTime) / 800.0) * 0.02f;
        renderLogo(context, width, height, fade, logoScale);

        for (MenuButton button : buttons) {
            try {
                button.render(context, mouseX, mouseY, delta);
            } catch (Exception e) {
                // skip broken button
            }
        }

        altWidget().render(context, mouseX, mouseY);

        renderBottomInfo(context, width, height, fade);
    }

    private AltWidget altWidget() {
        return Onetap.getInstance().getAltWidget();
    }

    private void renderParticles(DrawContext context, float delta) {
        for (Particle p : particles) {
            p.y -= p.speed;
            if (p.y < -10) {
                p.y = mc.getWindow().getScaledHeight() + 10;
                p.x = ThreadLocalRandom.current().nextFloat() * mc.getWindow().getScaledWidth();
            }
            int color = ColorProvider.rgba(255, 255, 255, (int) (p.alpha * 255));
            DrawUtil.drawRound(p.x, p.y, p.size, p.size, 1, color);
        }
    }

    private void renderLogo(DrawContext context, int width, int height, float fade, float scale) {
        String logo = "Infynyty";
        float logoSize = 52f;
        float logoWidth = Fonts.SFBOLD.get().getWidth(logo, logoSize);
        float logoX = (width - logoWidth) / 2f;
        float logoY = height / 2f - 140;

        float glowAlpha = 0.3f + 0.1f * (float) Math.sin((System.currentTimeMillis() - initTime) / 600.0);
        DrawUtil.drawRoundBlur(logoX - 40, logoY - 20, logoWidth + 80, logoSize + 40, 0,
                ColorProvider.setAlpha(INFINYTY_COLOR_1, (int) (glowAlpha * 255 * fade)), 40f);

        context.getMatrices().push();
        context.getMatrices().translate(logoX + logoWidth / 2f, logoY + logoSize / 2f, 0);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-(logoX + logoWidth / 2f), -(logoY + logoSize / 2f), 0);

        DrawUtil.drawText(Fonts.SFBOLD.get(), logo, logoX, logoY,
                ColorProvider.setAlpha(INFINYTY_COLOR_1, (int) (255 * fade)), logoSize);

        DrawUtil.drawText(Fonts.SFBOLD.get(), logo, logoX + 0.5f, logoY + 0.5f,
                ColorProvider.setAlpha(INFINYTY_COLOR_2, (int) (120 * fade)), logoSize);

        context.getMatrices().pop();

        String subtitle = "LayF Infynyty \u2022 Minecraft 1.21.4";
        float subSize = 12f;
        float subWidth = Fonts.SFREGULAR.get().getWidth(subtitle, subSize);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), subtitle, (width - subWidth) / 2f, logoY + logoSize + 8,
                ColorProvider.setAlpha(INFINYTY_COLOR_1, (int) (180 * fade)), subSize);
    }

    private void renderBottomInfo(DrawContext context, int width, int height, float fade) {
        String versionText = "v1.0.0";
        float versionSize = 8f;
        DrawUtil.drawText(Fonts.SFREGULAR.get(), versionText, 8, height - 14,
                ColorProvider.rgba(140, 140, 160, (int) (180 * fade)), versionSize);

        String userText = "User: " + mc.getSession().getUsername();
        float userSize = 8f;
        float userWidth = Fonts.SFREGULAR.get().getWidth(userText, userSize);
        DrawUtil.drawText(Fonts.SFREGULAR.get(), userText, width - userWidth - 8, height - 14,
                ColorProvider.rgba(140, 140, 160, (int) (180 * fade)), userSize);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        buttons.forEach(b -> b.click((int) mouseX, (int) mouseY, button));
        altWidget().click((int) mouseX, (int) mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        altWidget().updateScroll((int) mouseX, (int) mouseY, (float) verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        altWidget().onChar(chr);
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        altWidget().onKey(keyCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private static class Particle {
        float x, y, speed, alpha, size;

        Particle(float x, float y, float speed, float alpha) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.alpha = alpha;
            this.size = 1.0f;
        }
    }
}
