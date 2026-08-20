package tech.onetap.ui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import tech.onetap.Onetap;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.list.render.ClickGuiModule;
import tech.onetap.module.settings.ActionBindSetting;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.BindSetting;
import tech.onetap.module.settings.ColorSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.MultiSelectSetting;
import tech.onetap.module.settings.Setting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.module.settings.StringSetting;
import tech.onetap.util.Inputs;
import tech.onetap.util.config.ConfigManager;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.msdf.MsdfFont;

public final class ClickGuiScreen extends Screen {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ClickGui");
	private static final String FONT_NAME = "sf_pro_regular";
	private static final String ICON_FONT_NAME = "soul_icons";
	private static final Identifier NOT_FOUND_ANIMATION_TEXTURE = Identifier.of("mre", "textures/duck_empty_atlas.png");
	private static final Identifier THEME_WAVE_MASK_TEXTURE = Identifier.of("mre", "textures/theme_wave_mask.png");
	private static final int NOT_FOUND_FRAME_COUNT = 45;
	private static final int NOT_FOUND_FRAME_COLUMNS = 9;
	private static final int NOT_FOUND_FRAME_ROWS = 5;
	private static final int NOT_FOUND_FRAME_DURATION_MS = 60;
	private static final int SIDEBAR_WIDTH = 264;
	private static final int HEADER_HEIGHT = 60;
	private static final int CARD_RADIUS = 8;
	private static final int MODULE_GAP = 2;
	private static final int THEME_CARD_HEIGHT = 78;
	private static final int THEME_GAP = 1;
	private static final float SCROLL_SPEED = 34.0f;
	private static final String MODULE_HELP_TEXT = "\u0412\u043a\u043b/\u0412\u044b\u043a\u043b \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u0438 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438";
	private static final Set<Module> REMEMBERED_EXPANDED_MODULES = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<Setting> REMEMBERED_EXPANDED_SETTINGS = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Map<String, Float> REMEMBERED_SCROLL = new HashMap<>();
	private static ModuleCategory rememberedCategory = ModuleCategory.RENDER;
	private static SidebarPage rememberedPage = SidebarPage.MODULES;
	private static final ThemePreset[] THEME_PRESETS = new ThemePreset[] {
		new ThemePreset("Ruby Flow", new Color(255, 58, 82)),
		new ThemePreset("Rose Flux", new Color(255, 82, 151)),
		new ThemePreset("Violet Beam", new Color(166, 92, 255)),
		new ThemePreset("Azure Pulse", new Color(64, 138, 255)),
		new ThemePreset("Cyan Wave", new Color(32, 210, 255)),
		new ThemePreset("Mint Glow", new Color(52, 229, 177)),
		new ThemePreset("Lime Arc", new Color(148, 226, 62)),
		new ThemePreset("Gold Spark", new Color(255, 198, 50)),
		new ThemePreset("Solar", new Color(255, 129, 60)),
		new ThemePreset("Coral Tide", new Color(255, 91, 76)),
		new ThemePreset("Pearl", new Color(221, 238, 255)),
		new ThemePreset("Graphite Neon", new Color(124, 143, 170)),
		new ThemePreset("Indigo Drift", new Color(92, 106, 255)),
		new ThemePreset("Aqua Bloom", new Color(50, 232, 218)),
		new ThemePreset("Emerald Ray", new Color(60, 220, 124)),
		new ThemePreset("Punch Pop", new Color(255, 50, 116)),
		new ThemePreset("Sky Glass", new Color(104, 184, 255)),
		new ThemePreset("Plasma Run", new Color(216, 72, 255)),
		new ThemePreset("Cherry Ice", new Color(255, 76, 126)),
		new ThemePreset("Deep Ocean", new Color(42, 118, 202)),
		new ThemePreset("Arctic", new Color(145, 235, 255)),
		new ThemePreset("Toxic Mint", new Color(103, 255, 138)),
		new ThemePreset("Amber Wave", new Color(255, 174, 55)),
		new ThemePreset("Blood Moon", new Color(196, 36, 58)),
		new ThemePreset("Cyber Lime", new Color(188, 255, 52)),
		new ThemePreset("Royal Pink", new Color(236, 74, 205)),
		new ThemePreset("Ice Violet", new Color(158, 142, 255)),
		new ThemePreset("Steel Blue", new Color(94, 153, 190)),
		new ThemePreset("Jade", new Color(46, 210, 159)),
		new ThemePreset("White Pulse", new Color(245, 248, 255))
	};

	private final ClickGuiModule owner;
	private final List<ClickZone> clickZones = new ArrayList<>();
	private final Set<Module> expandedModules = REMEMBERED_EXPANDED_MODULES;
	private final Set<Setting> expandedSettings = REMEMBERED_EXPANDED_SETTINGS;

	private MsdfFont font;
	private MsdfFont iconFont;
	private boolean fontLoadFailed;
	private boolean iconFontLoadFailed;
	private boolean renderErrorLogged;
	private ModuleCategory selectedCategory = rememberedCategory;
	private SidebarPage selectedPage = rememberedPage;
	private boolean colorPickerOpen;
	private boolean themePickerSynced;
	private boolean helpTooltipHovered;
	private boolean searchFocused;
	private boolean closing;
	private boolean closeCompleted;
	private int openedOnKeyPress;
	private float renderScale = 1.0f;
	private float guiAnimation;
	private float visualScale = 1.0f;
	private float visualAlpha = 1.0f;
	private float contentReveal = 1.0f;
	private float layerAlpha = 1.0f;
	private float layerOffsetY;
	private float helpTooltipAlpha;
	private float themeHue;
	private float themeSaturation;
	private float themeBrightness;
	private String helpTooltipText = MODULE_HELP_TEXT;
	private String contentAnimationKey = "";
	private ActionBindSetting listeningBindSetting;
	private BindSetting listeningLegacyBindSetting;
	private Module listeningModuleBind;
	private StringSetting focusedStringSetting;
	private SliderSetting draggingSliderSetting;
	private int helpTooltipX;
	private int helpTooltipY;
	private String searchQuery = "";
	private String selectedConfigName;
	private String configNameInput = "";
	private boolean creatingConfig;
	private float createConfigPopupAlpha;
	private String selectedFriendName;
	private String friendNameInput = "";
	private boolean addingFriend;
	private float addFriendPopupAlpha;
	private int animationCenterX;
	private int animationCenterY;
	private PickerDrag pickerDrag = PickerDrag.NONE;
	private int pickerX;
	private int pickerY;
	private int pickerW;
	private int pickerH;
	private int pickerSquareX;
	private int pickerSquareY;
	private int pickerSquareSize;
	private int pickerHueX;
	private int pickerHueY;
	private int pickerHueW;
	private int pickerHueH;
	private int draggingSliderX;
	private int draggingSliderW;
	private String activeScrollKey = "";
	private int activeScrollX;
	private int activeScrollY;
	private int activeScrollW;
	private int activeScrollH;
	private float activeScrollMax;
	private int clipDepth;
	private int clipX;
	private int clipY;
	private int clipW;
	private int clipH;

	public ClickGuiScreen(ClickGuiModule owner) {
		super(Text.literal("ClickGUI"));
		this.owner = owner;
		this.openedOnKeyPress = tech.onetap.util.keyboard.KeyStorage.getPressCount();
		this.helpTooltipText = MODULE_HELP_TEXT;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void applyBlur() {
	}

	@Override
	public void blur() {
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (creatingConfig) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				creatingConfig = false;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !configNameInput.isEmpty()) {
				configNameInput = configNameInput.substring(0, configNameInput.length() - 1);
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				saveCreatedConfig();
				return true;
			}
			return true;
		}

		if (addingFriend) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				addingFriend = false;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendNameInput.isEmpty()) {
				friendNameInput = friendNameInput.substring(0, friendNameInput.length() - 1);
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				saveCreatedFriend();
				return true;
			}
			return true;
		}

		if (focusedStringSetting != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				focusedStringSetting = null;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				focusedStringSetting.backspace();
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_DELETE) {
				focusedStringSetting.clear();
				return true;
			}
			return true;
		}

		if (listeningBindSetting != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				listeningBindSetting = null;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
				listeningBindSetting.setKeyCode(GLFW.GLFW_KEY_UNKNOWN);
			} else {
				listeningBindSetting.setKeyCode(keyCode);
			}
			listeningBindSetting = null;
			return true;
		}

		if (listeningLegacyBindSetting != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				listeningLegacyBindSetting = null;
				return true;
			}
			listeningLegacyBindSetting.setValue(
					keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
							? GLFW.GLFW_KEY_UNKNOWN : keyCode);
			listeningLegacyBindSetting = null;
			return true;
		}

		if (listeningModuleBind != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
				listeningModuleBind = null;
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
				listeningModuleBind.setKey(GLFW.GLFW_KEY_UNKNOWN);
			} else {
				listeningModuleBind.setKey(keyCode);
			}
			listeningModuleBind = null;
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT && tech.onetap.util.keyboard.KeyStorage.getPressCount() == openedOnKeyPress) {
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
			beginClose();
			return true;
		}

		if (searchFocused) {
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
				searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
				return true;
			}

			if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				searchFocused = false;
				return true;
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (creatingConfig) {
			if (!Character.isISOControl(chr) && configNameInput.length() < 32) {
				configNameInput += chr;
			}
			return true;
		}

		if (addingFriend) {
			if (!Character.isISOControl(chr) && friendNameInput.length() < 32) {
				friendNameInput += chr;
			}
			return true;
		}

		if (listeningBindSetting != null || listeningLegacyBindSetting != null || listeningModuleBind != null) {
			return true;
		}

		if (focusedStringSetting != null && !Character.isISOControl(chr)) {
			focusedStringSetting.append(chr);
			return true;
		}

		if (searchFocused && !Character.isISOControl(chr)) {
			searchQuery += chr;
			selectedPage = SidebarPage.SEARCH;
			rememberedPage = selectedPage;
			return true;
		}

		return super.charTyped(chr, modifiers);
	}

	@Override
	public void close() {
		beginClose();
	}

	@Override
	public void removed() {
		if (owner.isEnabled() && !closeCompleted) {
			owner.setEnabled(false);
		}
	}

	public void requestClose() {
		if (closing) {
			return;
		}

		closing = true;
		colorPickerOpen = false;
		listeningBindSetting = null;
		listeningLegacyBindSetting = null;
		listeningModuleBind = null;
		focusedStringSetting = null;
		draggingSliderSetting = null;
		creatingConfig = false;
		addingFriend = false;
		rememberedCategory = selectedCategory;
		rememberedPage = selectedPage;
		pickerDrag = PickerDrag.NONE;
		clickZones.clear();
	}

	private void beginClose() {
		if (owner.isEnabled()) {
			owner.setEnabled(false);
		} else {
			requestClose();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
		updateRenderScale();
		clickZones.clear();
		activeScrollKey = "";
		activeScrollMax = 0.0f;
		helpTooltipHovered = false;
		syncThemePicker();

		int designWidth = designWidth();
		int designHeight = designHeight();
		int designMouseX = toDesign(mouseX);
		int designMouseY = toDesign(mouseY);
		int panelWidth = Math.min(918, Math.max(720, designWidth - 44));
		int panelHeight = Math.min(646, Math.max(430, designHeight - 44));
		int panelX = (designWidth - panelWidth) / 2;
		int panelY = (designHeight - panelHeight) / 2;
		updateGuiAnimation(panelX + panelWidth / 2, panelY + panelHeight / 2);
		updateContentAnimation();
		if (closeCompleted) {
			return;
		}

		if (owner.getBackgroundDim() > 0) {
			drawRect(0, 0, designWidth, designHeight, 0.0f, color(0, 0, 0, owner.getBackgroundDim()));
		}
		float panelRadius = owner.getRoundness();
		Color panelColor = guiStyleColor(color(8, 9, 12, 246), color(16, 18, 24, 205), color(6, 7, 9, 252));
		drawRect(panelX, panelY, panelWidth, panelHeight, panelRadius, panelColor);
		if (owner.hasAccentGlow()) {
			drawThemeGradientRect(panelX + 1, panelY + 1, panelWidth - 2, 2, Math.min(2.0f, panelRadius), 100, 0.0f);
		}
		drawBorder(panelX, panelY, panelWidth, panelHeight, panelRadius, guiStyleColor(color(255, 255, 255, 20), color(255, 255, 255, 34), color(255, 255, 255, 12)), 1.0f);
		drawSidebar(context, panelX, panelY, panelHeight, designMouseX, designMouseY);
		drawMain(context, panelX + SIDEBAR_WIDTH, panelY, panelWidth - SIDEBAR_WIDTH, panelHeight, designMouseX, designMouseY);
		drawHelpTooltip(context);

		if (colorPickerOpen) {
			drawColorPicker(context);
		}
		drawCreateConfigPopup(context);
		drawAddFriendPopup(context);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (closing) {
			return true;
		}

		double designMouseX = toDesign(mouseX);
		double designMouseY = toDesign(mouseY);

		if (listeningBindSetting != null || listeningLegacyBindSetting != null || listeningModuleBind != null) {
			int code = Inputs.mouseButtonCode(button);
			if (listeningBindSetting != null) {
				listeningBindSetting.setKeyCode(code);
				listeningBindSetting = null;
			} else if (listeningLegacyBindSetting != null) {
				listeningLegacyBindSetting.setValue(code);
				listeningLegacyBindSetting = null;
			} else {
				listeningModuleBind.setKey(code);
				listeningModuleBind = null;
			}
			return true;
		}

		if (colorPickerOpen && handleColorPickerMouse(designMouseX, designMouseY)) {
			return true;
		}

		draggingSliderSetting = null;
		focusedStringSetting = null;
		for (int i = clickZones.size() - 1; i >= 0; i--) {
			ClickZone zone = clickZones.get(i);
			if (zone.contains(designMouseX, designMouseY)) {
				zone.action().run(designMouseX, designMouseY, button);
				return true;
			}
		}

		if (colorPickerOpen && !inside(designMouseX, designMouseY, pickerX, pickerY, pickerW, pickerH)) {
			colorPickerOpen = false;
			return true;
		}

		focusedStringSetting = null;
		searchFocused = false;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (closing) {
			return true;
		}

		double designMouseX = toDesign(mouseX);
		double designMouseY = toDesign(mouseY);

		if (pickerDrag != PickerDrag.NONE) {
			updateColorPicker(designMouseX, designMouseY);
			return true;
		}

		if (draggingSliderSetting != null) {
			updateSliderValue(designMouseX);
			return true;
		}

		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		pickerDrag = PickerDrag.NONE;
		draggingSliderSetting = null;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (closing) {
			return true;
		}

		double designMouseX = toDesign(mouseX);
		double designMouseY = toDesign(mouseY);
		if (!activeScrollKey.isEmpty()
			&& activeScrollMax > 0.0f
			&& inside(designMouseX, designMouseY, activeScrollX, activeScrollY, activeScrollW, activeScrollH)) {
			float current = REMEMBERED_SCROLL.getOrDefault(activeScrollKey, 0.0f);
			rememberScroll(activeScrollKey, current - (float) verticalAmount * SCROLL_SPEED, activeScrollMax);
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void drawSidebar(DrawContext context, int x, int y, int height, int mouseX, int mouseY) {
		drawRect(x, y, SIDEBAR_WIDTH, height, 8.0f, color(10, 10, 12, 250));
		drawRect(x + SIDEBAR_WIDTH - 1, y, 1, height, 0.0f, color(255, 255, 255, 12));
		drawRect(x, y + HEADER_HEIGHT, SIDEBAR_WIDTH, 56, 0.0f, color(22, 22, 24, 232));
		drawRect(x, y + 116, SIDEBAR_WIDTH, 1, 0.0f, color(255, 255, 255, 11));

		drawIcon(context, "A", x + 90, y + 18, 25.0f, theme());
		drawText(context, "Infinyty", x + 127, y + 22, 18.0f, color(240, 240, 244, 245));
		boolean searchActive = selectedPage == SidebarPage.SEARCH || searchFocused;
		String searchText = searchFocused ? searchQuery : (searchQuery.isEmpty() ? "Search" : searchQuery);
		if (searchActive) {
			drawRect(x, y + HEADER_HEIGHT, SIDEBAR_WIDTH, 56, 0.0f, color(27, 27, 30, 235));
		}
		drawIcon(context, "B", x + 20, y + 78, 17.0f, searchActive ? theme() : color(146, 146, 152, 230));
		drawText(context, searchFocused ? searchText + "_" : searchText, x + 46, y + 78, 15.0f, searchQuery.isEmpty() && !searchFocused ? color(144, 144, 150, 230) : color(226, 226, 232, 245));
		addZone(x, y + HEADER_HEIGHT, SIDEBAR_WIDTH, 56, (mx, my, button) -> {
			selectedPage = SidebarPage.SEARCH;
			rememberedPage = selectedPage;
			searchFocused = true;
		});

		int navY = y + 148;
		drawNavItem(context, x, navY, "Combat", "1", ModuleCategory.COMBAT, mouseX, mouseY);
		drawNavItem(context, x, navY + 44, "Movement", "2", ModuleCategory.MOVEMENT, mouseX, mouseY);
		drawNavItem(context, x, navY + 88, "Render", "3", ModuleCategory.RENDER, mouseX, mouseY);
		drawNavItem(context, x, navY + 132, "Player", "4", ModuleCategory.PLAYER, mouseX, mouseY);
		drawNavItem(context, x, navY + 176, "Misc", "5", ModuleCategory.MISC, mouseX, mouseY);

		drawRect(x + 27, navY + 216, SIDEBAR_WIDTH - 54, 1, 0.0f, color(255, 255, 255, 16));
		drawSideButton(context, x, navY + 244, "Configs", "6", "I", SidebarPage.CONFIGS, mouseX, mouseY);
		drawSideButton(context, x, navY + 288, "Friends", "7", "J", SidebarPage.FRIENDS, mouseX, mouseY);
		drawSideButton(context, x, navY + 332, "Themes", "8", "K", SidebarPage.THEMES, mouseX, mouseY);

		int profileY = y + height - 68;
		drawRect(x, profileY - 1, SIDEBAR_WIDTH, 1, 0.0f, color(255, 255, 255, 13));
		drawRect(x + 19, profileY + 16, 38, 38, 19.0f, color(29, 24, 36, 255));
		drawIcon(context, "A", x + 28, profileY + 24, 18.0f, theme());
		drawIcon(context, "G", x + 70, profileY + 20, 13.0f, theme());
		MinecraftClient client = MinecraftClient.getInstance();
		String playerName = client.player != null ? client.player.getNameForScoreboard() : client.getSession().getUsername();
		drawText(context, playerName, x + 90, profileY + 20, 14.0f, color(248, 248, 250, 245));
		drawText(context, "На сервере: " + formatPlayTime(), x + 70, profileY + 40, 13.0f, color(186, 186, 192, 230));
	}

	private String formatPlayTime() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getNetworkHandler() == null) return "00:00:00";
		long totalSeconds = Math.max(0L, client.player.age / 20L);
		long hours = totalSeconds / 3600L;
		long minutes = totalSeconds % 3600L / 60L;
		long seconds = totalSeconds % 60L;
		return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
	}

	private void drawNavItem(DrawContext context, int x, int y, String label, String index, ModuleCategory category, int mouseX, int mouseY) {
		boolean active = selectedPage == SidebarPage.MODULES && selectedCategory == category;
		boolean hovered = inside(mouseX, mouseY, x + 7, y - 1, SIDEBAR_WIDTH - 14, 42);

		if (active) {
			drawThemeGradientRect(x + 22, y + 9, 3, 18, 1.0f, 255, y * 0.006f);
			drawRect(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, 0.0f, color(29, 29, 31, 238));
		} else if (hovered) {
			drawRect(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, 0.0f, color(24, 24, 26, 185));
		}

		Color textColor = active ? color(246, 246, 248, 250) : color(123, 123, 130, 232);
		drawIcon(context, categoryIcon(category), x + 33, y + 9, 17.0f, active ? theme() : color(132, 132, 139, 230));
		drawText(context, label, x + 61, y + 10, 15.0f, textColor);
		drawIndexBadge(context, x + SIDEBAR_WIDTH - 38, y + 6, index, active);

		addZone(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, (mx, my, button) -> {
			searchFocused = false;
			selectedPage = SidebarPage.MODULES;
			selectedCategory = category;
			rememberedPage = selectedPage;
			rememberedCategory = selectedCategory;
		});
	}

	private void drawSideButton(DrawContext context, int x, int y, String label, String index, String icon, SidebarPage page, int mouseX, int mouseY) {
		boolean active = selectedPage == page;
		boolean hovered = inside(mouseX, mouseY, x + 7, y - 1, SIDEBAR_WIDTH - 14, 42);

		if (active) {
			drawRect(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, 0.0f, color(29, 29, 31, 238));
		} else if (hovered) {
			drawRect(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, 0.0f, color(24, 24, 26, 185));
		}

		drawIcon(context, icon, x + 33, y + 9, 17.0f, active ? theme() : color(132, 132, 139, 220));
		drawText(context, label, x + 61, y + 11, 15.0f, active ? color(246, 246, 248, 250) : color(123, 123, 130, 220));
		drawIndexBadge(context, x + SIDEBAR_WIDTH - 38, y + 6, index, active);
		addZone(x + 7, y - 1, SIDEBAR_WIDTH - 14, 42, (mx, my, button) -> {
			searchFocused = false;
			selectedPage = page;
			rememberedPage = selectedPage;
		});
	}

	private void drawIndexBadge(DrawContext context, int x, int y, String index, boolean active) {
		drawRect(x, y, 18, 20, 4.0f, active ? color(40, 40, 42, 220) : color(28, 28, 30, 140));
		drawTextCentered(context, index, x, y + 5, 18, 11.0f, active ? color(156, 156, 164, 235) : color(77, 77, 84, 190));
	}

	private void drawMain(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		drawHeader(context, x, y, width);
		if (selectedPage == SidebarPage.MODULES) {
			drawAnimatedContent(() -> drawModuleColumns(context, x, y, width, height, mouseX, mouseY));
		} else if (selectedPage == SidebarPage.SEARCH) {
			drawAnimatedContent(() -> drawSearchPage(context, x, y, width, height, mouseX, mouseY));
		} else if (selectedPage == SidebarPage.CONFIGS) {
			drawAnimatedContent(() -> drawConfigsPage(context, x, y, width, height, mouseX, mouseY));
		} else if (selectedPage == SidebarPage.FRIENDS) {
			drawAnimatedContent(() -> drawFriendsPage(context, x, y, width, height, mouseX, mouseY));
		} else {
			drawAnimatedContent(() -> drawThemesPage(context, x, y, width, height, mouseX, mouseY));
		}
	}

	private void drawHeader(DrawContext context, int x, int y, int width) {
		drawRect(x, y, width, HEADER_HEIGHT, 8.0f, color(12, 12, 15, 235));
		drawRect(x, y + HEADER_HEIGHT, width, 1, 0.0f, color(255, 255, 255, 10));
		String sectionName = selectedPage == SidebarPage.MODULES ? selectedCategory.getDisplayName() : selectedPage.displayName();
		String sectionIcon = selectedPage == SidebarPage.MODULES ? categoryIcon(selectedCategory) : selectedPage.icon();
		drawText(context, "Infinyty", x + 30, y + 22, 15.0f, color(242, 242, 245, 245));
		drawText(context, ">", x + 78, y + 22, 13.0f, color(86, 86, 92, 225));
		drawAnimatedContent(() -> {
			drawIcon(context, sectionIcon, x + 101, y + 22, 17.0f, theme());
			drawText(context, sectionName, x + 125, y + 23, 15.0f, color(128, 128, 136, 230));
		});
	}

	private void drawModuleColumns(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		int contentX = x + 30;
		int contentY = y + 82;
		int viewportY = contentY;
		int viewportH = contentViewportHeight(height);
		int columnGap = 16;
		int columnWidth = Math.max(250, Math.min(294, (width - 76) / 2));
		int rightX = contentX + columnWidth + columnGap;
		List<Module> modules = Onetap.getInstance().getModuleStorage().get(selectedCategory);
		int contentHeight = moduleColumnsHeight(modules);
		float scroll = prepareScroll("modules:" + selectedCategory.name(), x + 24, viewportY, width - 42, viewportH, contentHeight);
		int drawY = contentY - Math.round(scroll);

		withClip(context, x + 24, viewportY, width - 42, viewportH, () -> {
			int leftY = drawY;
			int rightY = drawY;
			for (int i = 0; i < modules.size(); i++) {
				Module module = modules.get(i);
				if (i % 2 == 0) {
					leftY = drawModuleCard(context, module, contentX, leftY, columnWidth, mouseX, mouseY) + MODULE_GAP;
				} else {
					rightY = drawModuleCard(context, module, rightX, rightY, columnWidth, mouseX, mouseY) + MODULE_GAP;
				}
			}

			if (modules.isEmpty()) {
				drawEmptyModuleList(context, contentX, drawY, columnWidth);
				drawEmptyModuleList(context, rightX, drawY, columnWidth);
			}
		});

		drawScrollbar(x + width - 8, viewportY, viewportH, contentHeight, scroll);
	}

	private void drawSearchPage(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		int contentX = x + 30;
		int contentY = y + 82;
		int viewportY = contentY;
		int viewportH = contentViewportHeight(height);
		int columnGap = 16;
		int columnWidth = Math.max(250, Math.min(294, (width - 76) / 2));
		int rightX = contentX + columnWidth + columnGap;
		String query = searchQuery.trim().toLowerCase(java.util.Locale.ROOT);
		List<Module> results = Onetap.getInstance().getModuleStorage().getModules().stream()
			.filter(module -> !query.isEmpty() && (module.getName().toLowerCase(java.util.Locale.ROOT).contains(query)
				|| owner.shouldSearchDescriptions() && module.getDesc().toLowerCase(java.util.Locale.ROOT).contains(query)))
			.toList();

		if (results.isEmpty()) {
			prepareScroll("search:" + query, x + 24, viewportY, width - 42, viewportH, viewportH);
			drawSearchEmptyState(context, x, y, width, height);
		} else {
			int contentHeight = moduleColumnsHeight(results);
			float scroll = prepareScroll("search:" + query, x + 24, viewportY, width - 42, viewportH, contentHeight);
			int drawY = contentY - Math.round(scroll);
			withClip(context, x + 24, viewportY, width - 42, viewportH, () -> {
				int leftY = drawY;
				int rightY = drawY;
				for (int i = 0; i < results.size(); i++) {
					Module module = results.get(i);
					if (i % 2 == 0) {
						leftY = drawModuleCard(context, module, contentX, leftY, columnWidth, mouseX, mouseY) + MODULE_GAP;
					} else {
						rightY = drawModuleCard(context, module, rightX, rightY, columnWidth, mouseX, mouseY) + MODULE_GAP;
					}
				}
			});
			drawScrollbar(x + width - 8, viewportY, viewportH, contentHeight, scroll);
		}
	}

	private int contentViewportHeight(int mainHeight) {
		return Math.max(80, mainHeight - 115);
	}

	private int listViewportHeight(int mainHeight) {
		return Math.max(80, mainHeight - 161);
	}

	private int moduleColumnsHeight(List<Module> modules) {
		if (modules.isEmpty()) {
			return 42;
		}

		int left = 0;
		int right = 0;
		for (int i = 0; i < modules.size(); i++) {
			int cardHeight = moduleCardHeight(modules.get(i)) + MODULE_GAP;
			if (i % 2 == 0) {
				left += cardHeight;
			} else {
				right += cardHeight;
			}
		}
		return Math.max(42, Math.max(left, right) - MODULE_GAP);
	}

	private int twoColumnFixedHeight(int count, int itemHeight) {
		if (count <= 0) {
			return itemHeight;
		}

		int leftCount = (count + 1) / 2;
		int rightCount = count / 2;
		return Math.max(oneColumnFixedHeight(leftCount, itemHeight), oneColumnFixedHeight(rightCount, itemHeight));
	}

	private int oneColumnFixedHeight(int count, int itemHeight) {
		if (count <= 0) {
			return itemHeight;
		}
		return count * itemHeight + Math.max(0, count - 1) * MODULE_GAP;
	}

	private float prepareScroll(String key, int x, int y, int width, int height, int contentHeight) {
		activeScrollKey = key;
		activeScrollX = x;
		activeScrollY = y;
		activeScrollW = width;
		activeScrollH = height;
		activeScrollMax = Math.max(0.0f, contentHeight - height);
		return rememberScroll(key, REMEMBERED_SCROLL.getOrDefault(key, 0.0f), activeScrollMax);
	}

	private float rememberScroll(String key, float value, float max) {
		float clamped = Math.max(0.0f, Math.min(max, value));
		REMEMBERED_SCROLL.put(key, clamped);
		return clamped;
	}

	private void drawScrollbar(int x, int y, int height, int contentHeight, float scroll) {
		drawRect(x, y, 2, height, 0.2f, color(255, 255, 255, 16));
		float max = Math.max(0.0f, contentHeight - height);
		if (max <= 0.0f) {
			return;
		}

		int thumbH = Math.max(34, Math.round(height * (height / (float) Math.max(height, contentHeight))));
		thumbH = Math.min(height, thumbH);
		int thumbY = y + Math.round((height - thumbH) * (scroll / max));
		drawRect(x, thumbY, 2, thumbH, 0.2f, color(255, 255, 255, 62));
	}

	private void withClip(DrawContext context, int x, int y, int width, int height, Runnable renderer) {
		pushClip(context, x, y, width, height);
		try {
			renderer.run();
		} finally {
			popClip(context);
		}
	}

	private void pushClip(DrawContext context, int x, int y, int width, int height) {
		clipDepth++;
		clipX = x;
		clipY = y;
		clipW = width;
		clipH = height;
		int screenX1 = Math.min(toScreenX(x), toScreenX(x + width));
		int screenY1 = Math.min(toScreenY(y), toScreenY(y + height));
		int screenX2 = Math.max(toScreenX(x), toScreenX(x + width));
		int screenY2 = Math.max(toScreenY(y), toScreenY(y + height));
		context.enableScissor(screenX1, screenY1, screenX2, screenY2);
	}

	private void popClip(DrawContext context) {
		if (clipDepth > 0) {
			clipDepth--;
		}
		context.disableScissor();
	}

	private void drawSearchEmptyState(DrawContext context, int x, int y, int width, int height) {
		int imageSize = 200;
		int imageX = x + (width - imageSize) / 2;
		int imageY = y + (height - imageSize) / 2 - 52;
		drawNotFoundAnimation(imageX, imageY, imageSize);
		drawTextCentered(context, "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0448\u043b\u0438 :(", x, imageY + imageSize + 20, width, 16.0f, theme());
	}

	private void drawThemesPage(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		int contentX = x + 30;
		int contentY = y + 82;
		int viewportY = contentY;
		int viewportH = contentViewportHeight(height);
		int listWidth = Math.max(250, Math.min(620, width - 68));
		int contentHeight = THEME_PRESETS.length * THEME_CARD_HEIGHT + Math.max(0, THEME_PRESETS.length - 1) * THEME_GAP;
		float scroll = prepareScroll("page:" + SidebarPage.THEMES.name(), contentX, viewportY, listWidth, viewportH, contentHeight);
		int drawY = contentY - Math.round(scroll);

		withClip(context, contentX, viewportY, listWidth, viewportH, () -> {
			int cardY = drawY;
			for (int i = 0; i < THEME_PRESETS.length; i++) {
				ThemePreset preset = THEME_PRESETS[i];
				cardY = drawThemeCard(context, preset, i, contentX, cardY, listWidth, mouseX, mouseY) + THEME_GAP;
			}
		});

		drawScrollbar(x + width - 8, viewportY, viewportH, contentHeight, scroll);
	}

	private int drawThemeCard(DrawContext context, ThemePreset preset, int index, int x, int y, int width, int mouseX, int mouseY) {
		boolean active = sameColor(owner.getThemeBaseColorValue(), preset.color());
		if (!insideClip(x, y, width, THEME_CARD_HEIGHT)) {
			return y + THEME_CARD_HEIGHT;
		}

		boolean hovered = inside(mouseX, mouseY, x, y, width, THEME_CARD_HEIGHT);
		Color[] gradient = ClickGuiModule.gradientColors(preset.color(), index * 0.08f, active ? 245 : 225);
		drawRect(x, y, width, THEME_CARD_HEIGHT, CARD_RADIUS, hovered ? color(13, 13, 17, 242) : color(9, 9, 12, 232));
		drawThemeCardLines(x, y, width);
		drawThemeWave(x, y, width, THEME_CARD_HEIGHT, preset.color(), index, active);
		if (active) {
			drawBorder(x, y, width, THEME_CARD_HEIGHT, CARD_RADIUS, withAlpha(gradient[0], 205), 1.1f);
		} else {
			drawBorder(x, y, width, THEME_CARD_HEIGHT, CARD_RADIUS, hovered ? color(255, 255, 255, 16) : color(255, 255, 255, 7), 0.45f);
		}

		drawText(context, preset.name(), x + 22, y + 14, 16.0f, active ? color(246, 246, 250, 250) : color(212, 212, 220, 238));
		drawText(context, hex(preset.color()) + " -> " + hex(gradient[2]), x + 22, y + 35, 12.0f, active ? color(190, 190, 198, 238) : color(126, 126, 136, 224));
		drawColorDot(x + width - 28, y + 25, 9, gradient[0]);
		addZone(x, y, width, THEME_CARD_HEIGHT, (mx, my, button) -> {
			owner.setThemeBaseColor(preset.color());
			themePickerSynced = false;
		});
		return y + THEME_CARD_HEIGHT;
	}

	private void drawThemeCardLines(int x, int y, int width) {
		for (int lineY = y + 48; lineY < y + THEME_CARD_HEIGHT - 8; lineY += 12) {
			drawRect(x + 16, lineY, width - 32, 1, 0.0f, color(255, 255, 255, 9));
		}
	}

	private void drawThemeWave(int x, int y, int width, int height, Color base, int index, boolean active) {
		int waveX = x + 3;
		int waveY = y + 43;
		int waveW = width - 6;
		int waveH = height - 47;
		float phase = (System.currentTimeMillis() % 7200L) / 7200.0f;
		Color[] gradient = ClickGuiModule.gradientColors(base, index * 0.08f + phase * 0.12f, active ? 248 : 228);
		drawTextureRegionGradient(
			waveX,
			waveY,
			waveW,
			waveH,
			0.0f,
			THEME_WAVE_MASK_TEXTURE,
			0.0f,
			0.0f,
			1.0f,
			1.0f,
			gradient[0],
			gradient[1],
			gradient[2],
			gradient[0]
		);
	}

	private void drawConfigsPage(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		int contentX = x + 30;
		int contentY = y + 82;
		int viewportY = contentY + 46;
		int viewportH = listViewportHeight(height);
		int listWidth = Math.max(250, Math.min(604, width - 68));
		int buttonGap = 8;
		int buttonWidth = (listWidth - buttonGap * 2) / 3;

		drawConfigButton(context, "Создать", contentX, contentY, buttonWidth, true, () -> {
			creatingConfig = true;
			configNameInput = "";
			searchFocused = false;
		});
		drawConfigButton(context, "Удалить", contentX + buttonWidth + buttonGap, contentY, buttonWidth, selectedConfigName != null, () -> {
			if (selectedConfigName != null && ConfigManager.deleteInternal(selectedConfigName)) {
				selectedConfigName = null;
			}
		});
		drawConfigButton(context, "Открыть", contentX + (buttonWidth + buttonGap) * 2, contentY, buttonWidth, true, ConfigManager::openConfigDirectory);

		List<String> configs = ConfigManager.listConfigs();
		int contentHeight = configs.isEmpty() ? 42 : oneColumnFixedHeight(configs.size(), 46);
		float scroll = prepareScroll("page:" + SidebarPage.CONFIGS.name(), contentX, viewportY, listWidth, viewportH, contentHeight);
		int cardY = viewportY - Math.round(scroll);
		pushClip(context, contentX, viewportY, listWidth, viewportH);
		try {
		if (configs.isEmpty()) {
			drawRect(contentX, cardY, listWidth, 42, CARD_RADIUS, color(9, 9, 12, 145));
			drawBorder(contentX, cardY, listWidth, 42, CARD_RADIUS, color(255, 255, 255, 9), 1.0f);
			drawIcon(context, "I", contentX + 14, cardY + 11, 16.0f, color(95, 95, 103, 210));
			drawText(context, "Пока нет конфигов", contentX + 38, cardY + 13, 14.0f, color(112, 112, 120, 220));
		} else {
			for (String config : configs) {
				cardY = drawConfigCard(context, config, contentX, cardY, listWidth, mouseX, mouseY) + MODULE_GAP;
			}
		}
		} finally {
			popClip(context);
		}

		drawScrollbar(x + width - 8, viewportY, viewportH, contentHeight, scroll);
	}

	private void drawConfigButton(DrawContext context, String label, int x, int y, int width, boolean enabled, Runnable action) {
		Color background = enabled ? color(15, 15, 19, 232) : color(12, 12, 15, 150);
		Color text = enabled ? color(234, 234, 240, 245) : color(104, 104, 112, 190);
		drawRect(x, y, width, 34, 7.0f, background);
		drawBorder(x, y, width, 34, 7.0f, enabled ? color(255, 255, 255, 8) : color(255, 255, 255, 4), 0.5f);
		drawTextCentered(context, label, x, y + 10, width, 13.0f, text);
		if (enabled) {
			addZone(x, y, width, 34, (mx, my, button) -> action.run());
		}
	}

	private int drawConfigCard(DrawContext context, String config, int x, int y, int width, int mouseX, int mouseY) {
		boolean selected = config.equals(selectedConfigName);
		drawCard(context, x, y, width, 46, mouseX, mouseY);
		if (selected) {
			drawBorder(x, y, width, 46, CARD_RADIUS, withAlpha(theme(), 205), 1.0f);
		}

		drawIcon(context, "I", x + 14, y + 14, 15.0f, selected ? theme() : color(126, 126, 134, 230));
		drawText(context, config, x + 40, y + 15, 14.0f, selected ? color(244, 244, 248, 248) : color(174, 174, 182, 235));
		int loadWidth = 92;
		int loadX = x + width - loadWidth - 9;
		if (selected) {
			drawThemeGradientRect(loadX, y + 8, loadWidth, 30, 6.0f, 210, y * 0.004f);
		} else {
			drawRect(loadX, y + 8, loadWidth, 30, 6.0f, color(20, 20, 24, 228));
		}
		drawTextCentered(context, "Загрузить", loadX, y + 17, loadWidth, 12.0f, selected ? color(255, 255, 255, 250) : color(198, 198, 206, 240));
		addZone(x, y, width, 46, (mx, my, button) -> selectedConfigName = config);
		addZone(loadX, y + 8, loadWidth, 30, (mx, my, button) -> {
			selectedConfigName = config;
			ConfigManager.load(config);
			themePickerSynced = false;
		});
		return y + 46;
	}

	private void drawFriendsPage(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		int contentX = x + 30;
		int contentY = y + 82;
		int viewportY = contentY + 46;
		int viewportH = listViewportHeight(height);
		int listWidth = Math.max(250, Math.min(604, width - 68));
		int buttonGap = 8;
		int buttonWidth = (listWidth - buttonGap) / 2;

		drawConfigButton(context, "Добавить", contentX, contentY, buttonWidth, true, () -> {
			addingFriend = true;
			friendNameInput = "";
			searchFocused = false;
		});
		drawConfigButton(context, "Удалить", contentX + buttonWidth + buttonGap, contentY, buttonWidth, selectedFriendName != null, () -> {
			if (selectedFriendName != null && FriendRepository.remove(selectedFriendName)) {
				selectedFriendName = null;
			}
		});

		List<String> friends = FriendRepository.getFriendNames();
		int contentHeight = friends.isEmpty() ? 42 : oneColumnFixedHeight(friends.size(), 46);
		float scroll = prepareScroll("page:" + SidebarPage.FRIENDS.name(), contentX, viewportY, listWidth, viewportH, contentHeight);
		int cardY = viewportY - Math.round(scroll);
		pushClip(context, contentX, viewportY, listWidth, viewportH);
		try {
		if (friends.isEmpty()) {
			drawRect(contentX, cardY, listWidth, 42, CARD_RADIUS, color(9, 9, 12, 145));
			drawBorder(contentX, cardY, listWidth, 42, CARD_RADIUS, color(255, 255, 255, 9), 1.0f);
			drawIcon(context, "J", contentX + 14, cardY + 11, 16.0f, color(95, 95, 103, 210));
			drawText(context, "Список друзей пуст", contentX + 38, cardY + 13, 14.0f, color(112, 112, 120, 220));
		} else {
			for (String friend : friends) {
				cardY = drawFriendCard(context, friend, contentX, cardY, listWidth, mouseX, mouseY) + MODULE_GAP;
			}
		}
		} finally {
			popClip(context);
		}

		drawScrollbar(x + width - 8, viewportY, viewportH, contentHeight, scroll);
	}

	private int drawFriendCard(DrawContext context, String friend, int x, int y, int width, int mouseX, int mouseY) {
		boolean selected = friend.equals(selectedFriendName);
		drawCard(context, x, y, width, 46, mouseX, mouseY);
		if (selected) {
			drawBorder(x, y, width, 46, CARD_RADIUS, withAlpha(theme(), 205), 1.0f);
		}

		drawIcon(context, "J", x + 14, y + 14, 15.0f, selected ? theme() : color(126, 126, 134, 230));
		drawText(context, friend, x + 40, y + 15, 14.0f, selected ? color(244, 244, 248, 248) : color(174, 174, 182, 235));
		int teleportWidth = 128;
		int teleportX = x + width - teleportWidth - 9;
		if (selected) {
			drawThemeGradientRect(teleportX, y + 8, teleportWidth, 30, 6.0f, 210, y * 0.004f);
		} else {
			drawRect(teleportX, y + 8, teleportWidth, 30, 6.0f, color(20, 20, 24, 228));
		}
		drawTextCentered(context, "Телепортироватся", teleportX, y + 18, teleportWidth, 10.5f, selected ? color(255, 255, 255, 250) : color(198, 198, 206, 240));
		addZone(x, y, width, 46, (mx, my, button) -> selectedFriendName = friend);
		addZone(teleportX, y + 8, teleportWidth, 30, (mx, my, button) -> {
			selectedFriendName = friend;
			if (MinecraftClient.getInstance().player != null) {
				MinecraftClient.getInstance().player.networkHandler.sendChatCommand("tpa " + friend);
			}
		});
		return y + 46;
	}

	private int drawModuleCard(DrawContext context, Module module, int x, int y, int width, int mouseX, int mouseY) {
		boolean expanded = expandedModules.contains(module) && !module.getSettings().isEmpty();
		int height = moduleCardHeight(module);
		drawCard(context, x, y, width, height, mouseX, mouseY);
		if (listeningModuleBind == module) {
			drawBorder(x, y, width, height, CARD_RADIUS, withAlpha(theme(), 210), 0.8f);
		}
		drawModuleHeader(context, module, x, y, width, mouseX, mouseY);
		if (expanded) {
			drawModuleSettings(context, module, x, y + 46, width, mouseX, mouseY);
		}
		return y + height;
	}

	private int moduleCardHeight(Module module) {
		if (!expandedModules.contains(module) || module.getSettings().isEmpty()) {
			return 42;
		}

		int height = 50;
		for (Setting setting : module.getSettings()) {
			height += settingHeight(setting);
		}
		return height;
	}

	private int settingHeight(Setting setting) {
		int height = 30;
		if (setting instanceof ModeSetting modeSetting && expandedSettings.contains(setting)) {
			height += selectorRows(modeSetting) * 30 + 4;
		} else if (setting instanceof MultiSelectSetting multiSelectSetting && expandedSettings.contains(setting)) {
			height += selectorRows(multiSelectSetting) * 30 + 4;
		} else if (setting instanceof ModeListSetting modeListSetting && expandedSettings.contains(setting)) {
			height += selectorRows(modeListSetting) * 30 + 4;
		}
		return height;
	}

	private void drawModuleHeader(DrawContext context, Module module, int x, int y, int width, int mouseX, int mouseY) {
		drawText(context, module.getName(), x + 14, y + 13, 15.0f, module.isEnabled() ? color(241, 241, 245, 248) : color(170, 170, 178, 230));
		String keyName = keyName(module);
		int helpX = x + width - 101;
		int helpY = y + 12;
		boolean helpHovered = inside(mouseX, mouseY, helpX - 2, helpY - 2, 19, 19);
		drawIcon(context, "M", helpX, helpY, 15.0f, helpHovered ? theme() : color(118, 118, 126, 230));
		if (helpHovered) {
			helpTooltipHovered = true;
			helpTooltipText = owner.shouldShowDescriptions() ? module.getDesc() : MODULE_HELP_TEXT;
			helpTooltipX = helpX + 8;
			helpTooltipY = helpY;
		}
		drawIcon(context, "L", x + width - 75, y + 12, 15.0f, theme());
		String bindText = listeningModuleBind == module
				? "Назначение" + ".".repeat((int) ((System.currentTimeMillis() / 300L) % 3L) + 1)
				: keyName;
		drawTextRight(context, bindText, x + width - 14, y + 14, 12.0f, color(238, 238, 242, 245));

		addZone(x, y, width, 42, (mx, my, button) -> {
			if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
				listeningModuleBind = module;
				listeningBindSetting = null;
				listeningLegacyBindSetting = null;
			} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
				if (!module.getSettings().isEmpty()) {
					if (expandedModules.contains(module)) {
						expandedModules.remove(module);
					} else {
						expandedModules.add(module);
					}
				}
			} else if (module != owner) {
				module.toggle();
			}
		});
	}

	private void drawModuleSettings(DrawContext context, Module module, int x, int y, int width, int mouseX, int mouseY) {
		int rowY = y;
		for (Setting setting : module.getSettings()) {
			drawSettingRow(context, setting, x + 8, rowY, width - 16, mouseX, mouseY);
			rowY += settingHeight(setting);
		}
	}

	private void drawSettingRow(DrawContext context, Setting setting, int x, int y, int width, int mouseX, int mouseY) {
		boolean hovered = inside(mouseX, mouseY, x, y, width, 26);
		drawRect(x, y, width, 26, 6.0f, hovered ? color(20, 20, 24, 218) : color(15, 15, 18, 190));
		drawText(context, setting.getName(), x + 10, y + 6, 12.0f, color(154, 154, 162, 235));

		if (setting instanceof ModeSetting modeSetting) {
			drawTextRight(context, fitText(modeSetting.getValue(), 12.0f, settingValueWidth(setting, width)), x + width - 10, y + 6, 12.0f, color(246, 246, 250, 248));
			addZone(x, y, width, 26, (mx, my, button) -> toggleSettingSelector(setting));
			if (expandedSettings.contains(setting)) {
				drawModeSelector(context, modeSetting, x, y + 30, width, mouseX, mouseY);
			}
		} else if (setting instanceof MultiSelectSetting multiSelectSetting) {
			String value = multiSelectSetting.getSelected().isEmpty() ? "None" : String.join(", ", multiSelectSetting.getSelected());
			drawTextRight(context, fitText(value, 12.0f, settingValueWidth(setting, width)), x + width - 10, y + 6, 12.0f, color(246, 246, 250, 248));
			addZone(x, y, width, 26, (mx, my, button) -> toggleSettingSelector(setting));
			if (expandedSettings.contains(setting)) {
				drawMultiSelector(context, multiSelectSetting, x, y + 30, width, mouseX, mouseY);
			}
		} else if (setting instanceof ModeListSetting modeListSetting) {
			String value = modeListSetting.getEnabledModules().isEmpty() ? "None" : String.join(", ", modeListSetting.getEnabledModules());
			drawTextRight(context, fitText(value, 12.0f, settingValueWidth(setting, width)), x + width - 10, y + 6, 12.0f, color(246, 246, 250, 248));
			addZone(x, y, width, 26, (mx, my, button) -> toggleSettingSelector(setting));
			if (expandedSettings.contains(setting)) {
				drawModeListSelector(context, modeListSetting, x, y + 30, width, mouseX, mouseY);
			}
		} else if (setting instanceof ActionBindSetting bindSetting) {
			boolean listening = listeningBindSetting == bindSetting;
			String value = listening
					? "Назначение" + ".".repeat((int) ((System.currentTimeMillis() / 300L) % 3L) + 1)
					: keyName(bindSetting.getKeyCode());
			Color valueColor = listening ? theme() : color(232, 232, 238, 245);
			int valueX = Math.round(x + width - 10 - textWidth(value, 12.0f));
			drawIcon(context, "L", valueX - 20, y + 5, 13.0f, theme());
			drawText(context, value, valueX, y + 6, 12.0f, valueColor);
			addZone(x, y, width, 26, (mx, my, button) -> {
				listeningModuleBind = null;
				listeningLegacyBindSetting = null;
				listeningBindSetting = bindSetting;
			});
		} else if (setting instanceof BindSetting bindSetting) {
			boolean listening = listeningLegacyBindSetting == bindSetting;
			String value = listening
					? "Назначение" + ".".repeat((int) ((System.currentTimeMillis() / 300L) % 3L) + 1)
					: keyName(bindSetting.getValue());
			Color valueColor = listening ? theme() : color(232, 232, 238, 245);
			int valueX = Math.round(x + width - 10 - textWidth(value, 12.0f));
			drawIcon(context, "L", valueX - 20, y + 5, 13.0f, theme());
			drawText(context, value, valueX, y + 6, 12.0f, valueColor);
			addZone(x, y, width, 26, (mx, my, button) -> {
				listeningModuleBind = null;
				listeningBindSetting = null;
				listeningLegacyBindSetting = bindSetting;
			});
		} else if (setting instanceof StringSetting stringSetting) {
			boolean focused = focusedStringSetting == stringSetting;
			int fieldW = Math.min(132, Math.max(92, width / 2));
			int fieldX = x + width - fieldW - 8;
			drawRect(fieldX, y + 4, fieldW, 18, 5.0f, focused ? color(24, 24, 29, 235) : color(18, 18, 22, 220));
			drawBorder(fieldX, y + 4, fieldW, 18, 5.0f, focused ? withAlpha(theme(), 150) : color(255, 255, 255, 14), 0.5f);
			String value = stringSetting.getValue();
			String visibleValue = value.isEmpty() && !focused ? "Введите ник" : value + (focused ? "_" : "");
			Color valueColor = value.isEmpty() && !focused ? color(106, 106, 114, 220) : color(232, 232, 238, 245);
			drawText(context, fitText(visibleValue, 11.0f, fieldW - 14), fieldX + 7, y + 7, 11.0f, valueColor);
			addZone(fieldX, y + 4, fieldW, 18, (mx, my, button) -> {
				searchFocused = false;
				focusedStringSetting = stringSetting;
			});
		} else if (setting instanceof BooleanSetting booleanSetting) {
			drawCheckbox(context, x + width - 24, y + 6, booleanSetting.getValue());
			addZone(x, y, width, 26, (mx, my, button) -> booleanSetting.toggle());
		} else if (setting instanceof SliderSetting sliderSetting) {
			drawSlider(context, sliderSetting, x, y, width);
		} else if (setting instanceof ColorSetting colorSetting) {
			drawColorDot(x + width - 17, y + 12, 6, new Color(colorSetting.getValue(), true));
		}
	}

	private void drawCheckbox(DrawContext context, int x, int y, boolean checked) {
		if (checked) {
			drawThemeGradientRect(x, y, 14, 14, 4.0f, 255, y * 0.01f);
		} else {
			drawRect(x, y, 14, 14, 4.0f, color(22, 22, 26, 230));
		}
		drawBorder(x, y, 14, 14, 4.0f, checked ? withAlpha(theme(), 170) : color(255, 255, 255, 24), 0.6f);
		if (checked) {
			Color mark = color(255, 255, 255, 250);
			drawLine(x + 3.6f, y + 7.0f, x + 6.1f, y + 9.8f, mark, 1.45f);
			drawLine(x + 5.8f, y + 9.8f, x + 10.6f, y + 4.5f, mark, 1.45f);
		}
	}

	private void drawModeSelector(DrawContext context, ModeSetting setting, int x, int y, int width, int mouseX, int mouseY) {
		List<String> modes = setting.getModes();
		int columns = selectorColumns(setting);
		int gap = 6;
		int optionWidth = (width - gap * (columns - 1)) / columns;

		for (int i = 0; i < modes.size(); i++) {
			String mode = modes.get(i);
			int column = i % columns;
			int row = i / columns;
			int optionX = x + column * (optionWidth + gap);
			int optionY = y + row * 30;
			boolean active = i == setting.getIndex();
			boolean hovered = inside(mouseX, mouseY, optionX, optionY, optionWidth, 26);
			Color textColor = active ? color(255, 255, 255, 250) : color(174, 174, 182, 235);
			if (active) {
				drawThemeGradientRect(optionX, optionY, optionWidth, 26, 6.0f, 255, i * 0.085f);
			} else {
				drawRect(optionX, optionY, optionWidth, 26, 6.0f, hovered ? color(28, 28, 32, 235) : color(18, 18, 21, 215));
			}
			drawTextCentered(context, fitText(mode, 12.0f, optionWidth - 12), optionX, optionY + 6, optionWidth, 12.0f, textColor);
			addZone(optionX, optionY, optionWidth, 26, (mx, my, button) -> {
				setting.setIndex(modes.indexOf(mode));
			});
		}
	}

	private void drawMultiSelector(DrawContext context, MultiSelectSetting setting, int x, int y, int width, int mouseX, int mouseY) {
		List<String> options = setting.getOptions();
		int columns = selectorColumns(setting);
		int gap = 6;
		int optionWidth = (width - gap * (columns - 1)) / columns;

		for (int i = 0; i < options.size(); i++) {
			String option = options.get(i);
			int column = i % columns;
			int row = i / columns;
			int optionX = x + column * (optionWidth + gap);
			int optionY = y + row * 30;
			boolean active = setting.isSelected(option);
			boolean hovered = inside(mouseX, mouseY, optionX, optionY, optionWidth, 26);
			Color textColor = active ? color(255, 255, 255, 250) : color(174, 174, 182, 235);
			if (active) {
				drawThemeGradientRect(optionX, optionY, optionWidth, 26, 6.0f, 255, i * 0.085f);
			} else {
				drawRect(optionX, optionY, optionWidth, 26, 6.0f, hovered ? color(28, 28, 32, 235) : color(18, 18, 21, 215));
			}
			drawTextCentered(context, fitText(option, 12.0f, optionWidth - 12), optionX, optionY + 6, optionWidth, 12.0f, textColor);
			addZone(optionX, optionY, optionWidth, 26, (mx, my, button) -> setting.toggle(option));
		}
	}

	private void drawModeListSelector(DrawContext context, ModeListSetting setting, int x, int y, int width, int mouseX, int mouseY) {
		List<BooleanSetting> options = setting.getSettings();
		int columns = selectorColumns(setting);
		int gap = 6;
		int optionWidth = (width - gap * (columns - 1)) / columns;
		for (int i = 0; i < options.size(); i++) {
			BooleanSetting option = options.get(i);
			int optionX = x + (i % columns) * (optionWidth + gap);
			int optionY = y + (i / columns) * 30;
			boolean active = option.getValue();
			boolean hovered = inside(mouseX, mouseY, optionX, optionY, optionWidth, 26);
			if (active) drawThemeGradientRect(optionX, optionY, optionWidth, 26, 6.0f, 255, i * 0.085f);
			else drawRect(optionX, optionY, optionWidth, 26, 6.0f, hovered ? color(28, 28, 32, 235) : color(18, 18, 21, 215));
			drawTextCentered(context, fitText(option.getName(), 12.0f, optionWidth - 12), optionX, optionY + 6, optionWidth, 12.0f,
					active ? color(255, 255, 255, 250) : color(174, 174, 182, 235));
			addZone(optionX, optionY, optionWidth, 26, (mx, my, button) -> option.setValue(!option.getValue()));
		}
	}

	private void drawSlider(DrawContext context, SliderSetting setting, int x, int y, int width) {
		int sliderW = Math.min(128, Math.max(88, width / 2));
		int sliderX = x + width - sliderW - 10;
		int sliderY = y + 12;
		double progress = (setting.getValue() - setting.getMin()) / Math.max(0.0001d, setting.getMax() - setting.getMin());
		int filled = Math.round((float) progress * sliderW);
		String value = sliderValue(setting.getValue());

		drawRect(sliderX, sliderY, sliderW, 4, 2.0f, color(28, 28, 32, 230));
		drawThemeGradientRect(sliderX, sliderY, filled, 4, 2.0f, 255, sliderY * 0.01f);
		drawRect(sliderX + filled - 3, sliderY - 3, 7, 10, 4.0f, color(236, 236, 242, 245));
		drawTextRight(context, value, sliderX - 8, y + 6, 12.0f, color(232, 232, 238, 245));
		addZone(sliderX - 4, y, sliderW + 8, 26, (mx, my, button) -> {
			draggingSliderSetting = setting;
			draggingSliderX = sliderX;
			draggingSliderW = sliderW;
			updateSliderValue(mx);
		});
	}

	private void updateSliderValue(double mouseX) {
		if (draggingSliderSetting == null) {
			return;
		}

		double progress = (mouseX - draggingSliderX) / Math.max(1.0d, draggingSliderW);
		progress = Math.max(0.0d, Math.min(1.0d, progress));
		double value = draggingSliderSetting.getMin() + (draggingSliderSetting.getMax() - draggingSliderSetting.getMin()) * progress;
		draggingSliderSetting.setValue(value);
	}

	private String sliderValue(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private void toggleSettingSelector(Setting setting) {
		if (expandedSettings.contains(setting)) {
			expandedSettings.remove(setting);
		} else {
			expandedSettings.add(setting);
		}
	}

	private int selectorColumns(ModeSetting setting) {
		return setting.getModes().size() > 3 ? 2 : Math.max(1, setting.getModes().size());
	}

	private int selectorRows(ModeSetting setting) {
		return (int) Math.ceil((double) setting.getModes().size() / selectorColumns(setting));
	}

	private int selectorColumns(MultiSelectSetting setting) {
		return Math.min(2, Math.max(1, setting.getOptions().size()));
	}

	private int selectorRows(MultiSelectSetting setting) {
		return (int) Math.ceil((double) setting.getOptions().size() / selectorColumns(setting));
	}

	private int selectorColumns(ModeListSetting setting) {
		return Math.min(2, Math.max(1, setting.getSettings().size()));
	}

	private int selectorRows(ModeListSetting setting) {
		return (int) Math.ceil((double) setting.getSettings().size() / selectorColumns(setting));
	}

	private void drawEmptyModuleList(DrawContext context, int x, int y, int width) {
		drawRect(x, y, width, 42, CARD_RADIUS, color(9, 9, 12, 145));
		drawBorder(x, y, width, 42, CARD_RADIUS, color(255, 255, 255, 9), 1.0f);
		drawIcon(context, categoryIcon(selectedCategory), x + 14, y + 11, 16.0f, color(95, 95, 103, 210));
		drawText(context, "Empty", x + 38, y + 13, 14.0f, color(112, 112, 120, 220));
	}

	private void drawCard(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		boolean hovered = inside(mouseX, mouseY, x, y, width, height);
		Color idle = guiStyleColor(color(9, 9, 12, 225), color(17, 19, 25, 184), color(7, 8, 10, 238));
		Color hover = guiStyleColor(color(13, 13, 17, 236), color(25, 28, 36, 210), color(12, 13, 16, 246));
		float radius = Math.min(owner.getRoundness(), 9.0f);
		drawRect(x, y, width, height, radius, hovered ? hover : idle);
		drawBorder(x, y, width, height, radius, hovered ? color(255, 255, 255, 13) : color(255, 255, 255, 5), 0.2f);
	}

	private void drawGroupTitle(DrawContext context, String title, int x, int y) {
		drawText(context, title, x, y, 14.0f, color(118, 118, 126, 230));
	}

	private void drawHelpTooltip(DrawContext context) {
		float target = helpTooltipHovered ? 1.0f : 0.0f;
		helpTooltipAlpha += (target - helpTooltipAlpha) * 0.22f;
		if (helpTooltipAlpha < 0.02f) {
			return;
		}

		int alpha = Math.round(235.0f * helpTooltipAlpha);
		int textAlpha = Math.round(245.0f * helpTooltipAlpha);
		int width = Math.min(310, Math.round(textWidth(helpTooltipText, 12.0f)) + 22);
		int x = Math.max(10, Math.min(designWidth() - width - 10, helpTooltipX - width + 14));
		int y = Math.max(10, helpTooltipY - 34);
		drawRect(x, y, width, 28, 6.0f, color(11, 11, 14, alpha));
		drawBorder(x, y, width, 28, 6.0f, color(255, 255, 255, Math.round(28.0f * helpTooltipAlpha)), 0f);
		drawText(context, fitText(helpTooltipText, 12.0f, width - 22), x + 11, y + 8, 12.0f, color(225, 225, 232, textAlpha));
	}

	private void drawCreateConfigPopup(DrawContext context) {
		float target = creatingConfig ? 1.0f : 0.0f;
		createConfigPopupAlpha += (target - createConfigPopupAlpha) * 0.24f;
		if (createConfigPopupAlpha < 0.02f) {
			return;
		}

		int overlayAlpha = Math.round(132.0f * createConfigPopupAlpha);
		drawRect(0, 0, designWidth(), designHeight(), 0.0f, color(0, 0, 0, overlayAlpha));
		addZone(0, 0, designWidth(), designHeight(), (mx, my, button) -> creatingConfig = false);

		int popupW = 330;
		int popupH = 162;
		int popupX = (designWidth() - popupW) / 2;
		int popupY = (designHeight() - popupH) / 2;
		drawRect(popupX, popupY, popupW, popupH, 8.0f, color(10, 10, 13, Math.round(248.0f * createConfigPopupAlpha)));
		drawBorder(popupX, popupY, popupW, popupH, 8.0f, color(255, 255, 255, Math.round(24.0f * createConfigPopupAlpha)), 0.7f);
		drawText(context, "Создать конфиг", popupX + 20, popupY + 19, 16.0f, color(242, 242, 246, Math.round(245.0f * createConfigPopupAlpha)));

		int inputX = popupX + 20;
		int inputY = popupY + 58;
		int inputW = popupW - 40;
		drawRect(inputX, inputY, inputW, 38, 7.0f, color(17, 17, 21, Math.round(240.0f * createConfigPopupAlpha)));
		drawBorder(inputX, inputY, inputW, 38, 7.0f, color(255, 255, 255, Math.round(10.0f * createConfigPopupAlpha)), 0.5f);
		String inputText = configNameInput.isEmpty() ? "Название конфига" : configNameInput + "_";
		Color inputColor = configNameInput.isEmpty()
			? color(108, 108, 116, Math.round(220.0f * createConfigPopupAlpha))
			: color(232, 232, 238, Math.round(245.0f * createConfigPopupAlpha));
		drawText(context, inputText, inputX + 12, inputY + 13, 13.0f, inputColor);
		addZone(inputX, inputY, inputW, 38, (mx, my, button) -> creatingConfig = true);

		int saveX = inputX + (inputW - 102) / 2;
		int saveY = popupY + popupH - 48;
		if (configNameInput.trim().isEmpty()) {
			drawRect(saveX, saveY, 102, 30, 6.0f, color(28, 28, 32, Math.round(185.0f * createConfigPopupAlpha)));
		} else {
			drawThemeGradientRect(saveX, saveY, 102, 30, 6.0f, Math.round(220.0f * createConfigPopupAlpha), saveY * 0.004f);
		}
		drawTextCentered(context, "Сохранить", saveX, saveY + 9, 102, 12.0f, color(255, 255, 255, Math.round(245.0f * createConfigPopupAlpha)));
		addZone(saveX, saveY, 102, 30, (mx, my, button) -> saveCreatedConfig());
	}

	private void drawAddFriendPopup(DrawContext context) {
		float target = addingFriend ? 1.0f : 0.0f;
		addFriendPopupAlpha += (target - addFriendPopupAlpha) * 0.24f;
		if (addFriendPopupAlpha < 0.02f) {
			return;
		}

		int overlayAlpha = Math.round(132.0f * addFriendPopupAlpha);
		drawRect(0, 0, designWidth(), designHeight(), 0.0f, color(0, 0, 0, overlayAlpha));
		addZone(0, 0, designWidth(), designHeight(), (mx, my, button) -> addingFriend = false);

		int popupW = 330;
		int popupH = 162;
		int popupX = (designWidth() - popupW) / 2;
		int popupY = (designHeight() - popupH) / 2;
		drawRect(popupX, popupY, popupW, popupH, 8.0f, color(10, 10, 13, Math.round(248.0f * addFriendPopupAlpha)));
		drawBorder(popupX, popupY, popupW, popupH, 8.0f, color(255, 255, 255, Math.round(24.0f * addFriendPopupAlpha)), 0.7f);
		drawText(context, "Добавить друга", popupX + 20, popupY + 19, 16.0f, color(242, 242, 246, Math.round(245.0f * addFriendPopupAlpha)));

		int inputX = popupX + 20;
		int inputY = popupY + 58;
		int inputW = popupW - 40;
		drawRect(inputX, inputY, inputW, 38, 7.0f, color(17, 17, 21, Math.round(240.0f * addFriendPopupAlpha)));
		drawBorder(inputX, inputY, inputW, 38, 7.0f, color(255, 255, 255, Math.round(10.0f * addFriendPopupAlpha)), 0.5f);
		String inputText = friendNameInput.isEmpty() ? "Ник друга" : friendNameInput + "_";
		Color inputColor = friendNameInput.isEmpty()
			? color(108, 108, 116, Math.round(220.0f * addFriendPopupAlpha))
			: color(232, 232, 238, Math.round(245.0f * addFriendPopupAlpha));
		drawText(context, inputText, inputX + 12, inputY + 13, 13.0f, inputColor);
		addZone(inputX, inputY, inputW, 38, (mx, my, button) -> addingFriend = true);

		int addX = inputX + (inputW - 102) / 2;
		int addY = popupY + popupH - 48;
		if (friendNameInput.trim().isEmpty()) {
			drawRect(addX, addY, 102, 30, 6.0f, color(28, 28, 32, Math.round(185.0f * addFriendPopupAlpha)));
		} else {
			drawThemeGradientRect(addX, addY, 102, 30, 6.0f, Math.round(220.0f * addFriendPopupAlpha), addY * 0.004f);
		}
		drawTextCentered(context, "Добавить", addX, addY + 9, 102, 12.0f, color(255, 255, 255, Math.round(245.0f * addFriendPopupAlpha)));
		addZone(addX, addY, 102, 30, (mx, my, button) -> saveCreatedFriend());
	}

	private void saveCreatedConfig() {
		String name = ConfigManager.normalizeConfigName(configNameInput);
		if (configNameInput.trim().isEmpty()) {
			return;
		}

		if (ConfigManager.saveInternal(name)) {
			selectedConfigName = name;
			configNameInput = "";
			creatingConfig = false;
		}
	}

	private void saveCreatedFriend() {
		if (friendNameInput.trim().isEmpty()) {
			return;
		}

		String name = friendNameInput.trim();
		if (FriendRepository.add(name)) {
			selectedFriendName = name;
		}
		friendNameInput = "";
		addingFriend = false;
	}

	private void drawColorDot(int centerX, int centerY, int radius, Color value) {
		drawRect(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, value);
	}

	private void drawNotFoundAnimation(int x, int y, int size) {
		long totalDuration = (long) NOT_FOUND_FRAME_COUNT * NOT_FOUND_FRAME_DURATION_MS;
		long elapsed = Math.floorMod(System.currentTimeMillis(), totalDuration);
		float frameValue = (float) elapsed / (float) NOT_FOUND_FRAME_DURATION_MS;
		int frame = (int) frameValue;
		int nextFrame = (frame + 1) % NOT_FOUND_FRAME_COUNT;
		float blend = smooth(frameValue - frame);

		drawNotFoundFrame(x, y, size, frame, 255);
		drawNotFoundFrame(x, y, size, nextFrame, Math.round(105.0f * blend));
	}

	private void drawNotFoundFrame(int x, int y, int size, int frame, int alpha) {
		int column = frame % NOT_FOUND_FRAME_COLUMNS;
		int row = frame / NOT_FOUND_FRAME_COLUMNS;
		float frameWidth = 1.0f / NOT_FOUND_FRAME_COLUMNS;
		float frameHeight = 1.0f / NOT_FOUND_FRAME_ROWS;
		drawTextureRegion(
			x,
			y,
			size,
			size,
			0.0f,
			NOT_FOUND_ANIMATION_TEXTURE,
			column * frameWidth,
			row * frameHeight,
			frameWidth,
			frameHeight,
			alpha
		);
	}

	private void drawTextureRegion(int x, int y, int width, int height, float radius, Identifier textureId, float u, float v, float texWidth, float texHeight, int alpha) {
		if (width <= 0 || height <= 0 || alpha <= 0) {
			return;
		}
		if (clipDepth > 0 && clippedRect(x, y, width, height) == null) {
			return;
		}

		int screenX = toScreenX(x);
		int screenY = toScreenY(y);
		int screenWidth = toScreenSize(width);
		int screenHeight = toScreenSize(height);
		renderPart("texture", () -> {
			AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
			Builder.texture()
				.size(new SizeState(screenWidth, screenHeight))
				.radius(new QuadRadiusState(radius * renderScale * visualScale))
				.color(new QuadColorState(animatedColor(color(255, 255, 255, alpha))))
				.smoothness(1.0f)
				.texture(u, v, texWidth, texHeight, texture)
				.build()
				.render(screenX, screenY);
		});
	}

	private void drawTextureRegionGradient(int x, int y, int width, int height, float radius, Identifier textureId, float u, float v, float texWidth, float texHeight, Color topLeft, Color bottomLeft, Color bottomRight, Color topRight) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (clipDepth > 0 && clippedRect(x, y, width, height) == null) {
			return;
		}

		int screenX = toScreenX(x);
		int screenY = toScreenY(y);
		int screenWidth = toScreenSize(width);
		int screenHeight = toScreenSize(height);
		renderPart("texture", () -> {
			AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
			Builder.texture()
				.size(new SizeState(screenWidth, screenHeight))
				.radius(new QuadRadiusState(radius * renderScale * visualScale))
				.color(new QuadColorState(animatedColor(topLeft), animatedColor(bottomLeft), animatedColor(bottomRight), animatedColor(topRight)))
				.smoothness(1.0f)
				.texture(u, v, texWidth, texHeight, texture)
				.build()
				.render(screenX, screenY);
		});
	}

	private void drawColorPicker(DrawContext context) {
		pickerW = 164;
		pickerH = 132;
		pickerX = Math.max(8, designWidth() - pickerW - 24);
		pickerY = Math.max(8, Math.min(designHeight() - pickerH - 8, designHeight() / 2 + 90));
		pickerSquareX = pickerX + 12;
		pickerSquareY = pickerY + 38;
		pickerSquareSize = 82;
		pickerHueX = pickerSquareX + pickerSquareSize + 12;
		pickerHueY = pickerSquareY;
		pickerHueW = 12;
		pickerHueH = pickerSquareSize;

		drawRect(pickerX, pickerY, pickerW, pickerH, 8.0f, color(11, 11, 14, 250));
		drawBorder(pickerX, pickerY, pickerW, pickerH, 8.0f, color(255, 255, 255, 28), 1.0f);
		drawText(context, "Theme Color", pickerX + 12, pickerY + 13, 14.0f, color(238, 238, 242, 245));
		drawColorDot(pickerX + pickerW - 25, pickerY + 20, 9, owner.getThemeBaseColorValue());

		int cell = 4;
		for (int sx = 0; sx < pickerSquareSize; sx += cell) {
			for (int sy = 0; sy < pickerSquareSize; sy += cell) {
				float saturation = clamp((float) sx / (float) (pickerSquareSize - cell));
				float brightness = clamp(1.0f - (float) sy / (float) (pickerSquareSize - cell));
				fill(context, pickerSquareX + sx, pickerSquareY + sy, pickerSquareX + sx + cell, pickerSquareY + sy + cell, Color.HSBtoRGB(themeHue, saturation, brightness));
			}
		}

		for (int hy = 0; hy < pickerHueH; hy += cell) {
			float hue = clamp((float) hy / (float) (pickerHueH - cell));
			fill(context, pickerHueX, pickerHueY + hy, pickerHueX + pickerHueW, pickerHueY + hy + cell, Color.HSBtoRGB(hue, 1.0f, 1.0f));
		}

		int selectorX = pickerSquareX + Math.round(themeSaturation * pickerSquareSize) - 3;
		int selectorY = pickerSquareY + Math.round((1.0f - themeBrightness) * pickerSquareSize) - 3;
		drawBorder(selectorX, selectorY, 7, 7, 4.0f, color(255, 255, 255, 250), 1.5f);
		int hueY = pickerHueY + Math.round(themeHue * pickerHueH) - 2;
		drawRect(pickerHueX - 2, hueY, pickerHueW + 4, 4, 2.0f, color(255, 255, 255, 245));
		drawText(context, hex(owner.getThemeBaseColorValue()), pickerX + 12, pickerY + pickerH - 22, 12.0f, color(166, 166, 174, 235));
	}

	private boolean handleColorPickerMouse(double mouseX, double mouseY) {
		if (inside(mouseX, mouseY, pickerSquareX, pickerSquareY, pickerSquareSize, pickerSquareSize)) {
			pickerDrag = PickerDrag.SQUARE;
			updateColorPicker(mouseX, mouseY);
			return true;
		}

		if (inside(mouseX, mouseY, pickerHueX - 3, pickerHueY, pickerHueW + 6, pickerHueH)) {
			pickerDrag = PickerDrag.HUE;
			updateColorPicker(mouseX, mouseY);
			return true;
		}

		return inside(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);
	}

	private void updateColorPicker(double mouseX, double mouseY) {
		if (pickerDrag == PickerDrag.SQUARE) {
			themeSaturation = clamp((float) (mouseX - pickerSquareX) / pickerSquareSize);
			themeBrightness = clamp(1.0f - (float) (mouseY - pickerSquareY) / pickerSquareSize);
		} else if (pickerDrag == PickerDrag.HUE) {
			themeHue = clamp((float) (mouseY - pickerHueY) / pickerHueH);
		}

		owner.setThemeBaseColor(new Color(Color.HSBtoRGB(themeHue, themeSaturation, themeBrightness)));
	}

	private void syncThemePicker() {
		if (themePickerSynced) {
			return;
		}

		Color base = owner.getThemeBaseColorValue();
		float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
		themeHue = hsb[0];
		themeSaturation = hsb[1];
		themeBrightness = hsb[2];
		themePickerSynced = true;
	}

	private String keyName(Module module) {
		return keyName(module.getKey());
	}

	private String keyName(int keyCode) {
		return Inputs.name(keyCode);
	}

	private String categoryIcon(ModuleCategory category) {
		return switch (category) {
			case COMBAT -> "C";
			case MOVEMENT -> "D";
			case RENDER -> "E";
			case PLAYER -> "G";
			case MISC -> "H";
			case THEMES -> "K";
		};
	}

	private Color theme() {
		return owner.getThemeColorValue();
	}

	private void drawRect(int x, int y, int width, int height, float radius, Color color) {
		if (width <= 0 || height <= 0) {
			return;
		}

		ClipRect clipped = clippedRect(x, y, width, height);
		if (clipped == null) {
			return;
		}

		boolean cropped = clipped.x != x || clipped.y != y || clipped.width != width || clipped.height != height;
		int screenX = toScreenX(clipped.x);
		int screenY = toScreenY(clipped.y);
		int screenWidth = toScreenSize(clipped.width);
		int screenHeight = toScreenSize(clipped.height);
		float drawRadius = cropped ? 0.0f : radius;
		renderPart("rectangle", () -> Builder.rectangle()
			.size(new SizeState(screenWidth, screenHeight))
			.radius(new QuadRadiusState(drawRadius * renderScale * visualScale))
			.color(new QuadColorState(animatedColor(color)))
			.smoothness(1.0f)
			.build()
			.render(screenX, screenY));
	}

	private void drawGradientRect(int x, int y, int width, int height, float radius, Color topLeft, Color bottomLeft, Color bottomRight, Color topRight) {
		if (width <= 0 || height <= 0) {
			return;
		}

		ClipRect clipped = clippedRect(x, y, width, height);
		if (clipped == null) {
			return;
		}

		boolean cropped = clipped.x != x || clipped.y != y || clipped.width != width || clipped.height != height;
		int screenX = toScreenX(clipped.x);
		int screenY = toScreenY(clipped.y);
		int screenWidth = toScreenSize(clipped.width);
		int screenHeight = toScreenSize(clipped.height);
		float drawRadius = cropped ? 0.0f : radius;
		renderPart("rectangle", () -> Builder.rectangle()
			.size(new SizeState(screenWidth, screenHeight))
			.radius(new QuadRadiusState(drawRadius * renderScale * visualScale))
			.color(new QuadColorState(animatedColor(topLeft), animatedColor(bottomLeft), animatedColor(bottomRight), animatedColor(topRight)))
			.smoothness(1.0f)
			.build()
			.render(screenX, screenY));
	}

	private void drawThemeGradientRect(int x, int y, int width, int height, float radius, int alpha, float offset) {
		Color[] gradient = owner.getThemeGradient(offset, alpha);
		drawGradientRect(x, y, width, height, radius, gradient[0], gradient[1], gradient[2], gradient[0]);
	}

	private void drawBorder(int x, int y, int width, int height, float radius, Color color, float thickness) {
		if (width <= 0 || height <= 0 || thickness <= 0.0f) {
			return;
		}
		if (!isFullyInsideClip(x, y, width, height)) {
			return;
		}

		int screenX = toScreenX(x);
		int screenY = toScreenY(y);
		int screenWidth = toScreenSize(width);
		int screenHeight = toScreenSize(height);
		renderPart("border", () -> Builder.border()
			.size(new SizeState(screenWidth, screenHeight))
			.radius(new QuadRadiusState(radius * renderScale * visualScale))
			.color(new QuadColorState(animatedColor(color)))
			.thickness(Math.max(0.05f, thickness * renderScale * visualScale))
			.smoothness(1.0f, 1.0f)
			.build()
			.render(screenX, screenY));
	}

	private void drawLine(float x1, float y1, float x2, float y2, Color color, float thickness) {
		if (clipDepth > 0
			&& (!insideClip(x1, y1, 1.0f, 1.0f) || !insideClip(x2, y2, 1.0f, 1.0f))) {
			return;
		}

		float screenX1 = toScreenX(x1);
		float screenY1 = toScreenY(y1);
		float screenX2 = toScreenX(x2);
		float screenY2 = toScreenY(y2);
		float dx = screenX2 - screenX1;
		float dy = screenY2 - screenY1;
		float length = (float) Math.sqrt(dx * dx + dy * dy);
		if (length <= 0.5f) {
			return;
		}

		float screenThickness = toScreenSize(thickness);
		Matrix4f matrix = new Matrix4f()
			.translation(screenX1, screenY1, 0.0f)
			.rotateZ((float) Math.atan2(dy, dx));
		renderPart("checkbox", () -> Builder.rectangle()
			.size(new SizeState(length, screenThickness))
			.radius(new QuadRadiusState(screenThickness * 0.5f))
			.color(new QuadColorState(animatedColor(color)))
			.smoothness(1.0f)
			.build()
			.render(matrix, 0.0f, -screenThickness * 0.5f));
	}

	private void drawText(DrawContext context, String value, int x, int y, float size, Color color) {
		if (clipDepth > 0 && !insideClip(x, y, Math.max(1.0f, textWidth(value, size)), size + 6.0f)) {
			return;
		}

		MsdfFont loadedFont = getFont();
		if (loadedFont == null) {
			context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, value, toScreenX(x), toScreenY(y), animatedColor(color).getRGB());
			return;
		}

		renderPart("text", () -> Builder.text()
			.font(loadedFont)
			.text(value)
			.size(size * renderScale * visualScale)
			.color(animatedColor(color))
			.smoothness(0.5f)
			.thickness(0.04f * renderScale * visualScale)
			.build()
			.render(toScreenX(x), toScreenY(y)));
	}

	private void drawIcon(DrawContext context, String value, int x, int y, float size, Color color) {
		if (clipDepth > 0 && !insideClip(x, y, size + 4.0f, size + 6.0f)) {
			return;
		}

		MsdfFont loadedFont = getIconFont();
		if (loadedFont == null) {
			drawText(context, value, x, y, size, color);
			return;
		}

		renderPart("icon", () -> Builder.text()
			.font(loadedFont)
			.text(value)
			.size(size * renderScale * visualScale)
			.color(animatedColor(color))
			.smoothness(0.5f)
			.thickness(0.03f * renderScale * visualScale)
			.build()
			.render(toScreenX(x), toScreenY(y)));
	}

	private void drawTextRight(DrawContext context, String value, int rightX, int y, float size, Color color) {
		drawText(context, value, Math.round(rightX - textWidth(value, size)), y, size, color);
	}

	private void drawTextCentered(DrawContext context, String value, int x, int y, int width, float size, Color color) {
		drawText(context, value, Math.round(x + (width - textWidth(value, size)) / 2.0f), y, size, color);
	}

	private float textWidth(String value, float size) {
		MsdfFont loadedFont = getFont();
		if (loadedFont != null) {
			return loadedFont.getWidth(value, size * renderScale) / renderScale;
		}

		return MinecraftClient.getInstance().textRenderer.getWidth(value) / renderScale;
	}

	private String fitText(String value, float size, int maxWidth) {
		if (textWidth(value, size) <= maxWidth) {
			return value;
		}

		String text = value;
		while (!text.isEmpty() && textWidth("..." + text, size) > maxWidth) {
			text = text.substring(1);
		}
		return "..." + text;
	}

	private int settingValueWidth(Setting setting, int rowWidth) {
		int labelWidth = Math.round(textWidth(setting.getName(), 12.0f));
		return Math.max(42, rowWidth - labelWidth - 36);
	}

	private MsdfFont getFont() {
		if (fontLoadFailed) {
			return null;
		}

		if (font == null) {
			try {
				font = MsdfFont.builder()
					.name(FONT_NAME)
					.atlas(FONT_NAME)
					.data(FONT_NAME)
					.build();
			} catch (RuntimeException ex) {
				fontLoadFailed = true;
				LOGGER.warn("Failed to load ClickGUI MSDF font {}", FONT_NAME, ex);
			}
		}

		return font;
	}

	private MsdfFont getIconFont() {
		if (iconFontLoadFailed) {
			return null;
		}

		if (iconFont == null) {
			try {
				iconFont = MsdfFont.builder()
					.name(ICON_FONT_NAME)
					.atlas(ICON_FONT_NAME)
					.data(ICON_FONT_NAME)
					.build();
			} catch (RuntimeException ex) {
				iconFontLoadFailed = true;
				LOGGER.warn("Failed to load ClickGUI MSDF icon font {}", ICON_FONT_NAME, ex);
			}
		}

		return iconFont;
	}

	private void renderPart(String name, Runnable renderer) {
		try {
			renderer.run();
		} catch (RuntimeException | LinkageError ex) {
			if (!renderErrorLogged) {
				renderErrorLogged = true;
				LOGGER.warn("ClickGUI render failed at {}", name, ex);
			}
		}
	}

	private void addZone(int x, int y, int width, int height, MouseAction action) {
		if (clipDepth > 0) {
			int clippedX = Math.max(x, clipX);
			int clippedY = Math.max(y, clipY);
			int clippedRight = Math.min(x + width, clipX + clipW);
			int clippedBottom = Math.min(y + height, clipY + clipH);
			if (clippedRight <= clippedX || clippedBottom <= clippedY) {
				return;
			}
			clickZones.add(new ClickZone(clippedX, clippedY, clippedRight - clippedX, clippedBottom - clippedY, action));
			return;
		}
		clickZones.add(new ClickZone(x, y, width, height, action));
	}

	private void fill(DrawContext context, int x1, int y1, int x2, int y2, int color) {
		ClipRect clipped = clippedRect(x1, y1, x2 - x1, y2 - y1);
		if (clipped == null) {
			return;
		}

		Color fillColor = new Color(color, true);
		context.fill(
			toScreenX(clipped.x),
			toScreenY(clipped.y),
			toScreenX(clipped.x + clipped.width),
			toScreenY(clipped.y + clipped.height),
			animatedColor(fillColor).getRGB()
		);
	}

	private ClipRect clippedRect(int x, int y, int width, int height) {
		if (clipDepth <= 0) {
			return new ClipRect(x, y, width, height);
		}

		int clippedX = Math.max(x, clipX);
		int clippedY = Math.max(y, clipY);
		int clippedRight = Math.min(x + width, clipX + clipW);
		int clippedBottom = Math.min(y + height, clipY + clipH);
		if (clippedRight <= clippedX || clippedBottom <= clippedY) {
			return null;
		}
		return new ClipRect(clippedX, clippedY, clippedRight - clippedX, clippedBottom - clippedY);
	}

	private boolean isFullyInsideClip(int x, int y, int width, int height) {
		return clipDepth <= 0 || (x >= clipX && y >= clipY && x + width <= clipX + clipW && y + height <= clipY + clipH);
	}

	private boolean insideClip(float x, float y, float width, float height) {
		return clipDepth <= 0 || (x + width > clipX && y + height > clipY && x < clipX + clipW && y < clipY + clipH);
	}

	private void updateRenderScale() {
		double windowScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
		renderScale = (float) (1.0d / Math.max(1.0d, windowScale));
	}

	private void updateGuiAnimation(int centerX, int centerY) {
		animationCenterX = centerX;
		animationCenterY = centerY;
		float target = closing ? 0.0f : 1.0f;
		guiAnimation += (target - guiAnimation) * owner.getAnimationSpeed();

		if (!closing && guiAnimation > 0.995f) {
			guiAnimation = 1.0f;
		}

		if (closing && guiAnimation < 0.025f) {
			closeCompleted = true;
			MinecraftClient.getInstance().setScreen(null);
			return;
		}

		float eased = smooth(guiAnimation);
		visualScale = 0.965f + 0.035f * eased;
		visualAlpha = eased;
	}

	private void updateContentAnimation() {
		String nextKey = contentAnimationKey();
		if (!nextKey.equals(contentAnimationKey)) {
			contentAnimationKey = nextKey;
			contentReveal = 0.0f;
		}

		contentReveal += (1.0f - contentReveal) * 0.22f;
		if (contentReveal > 0.995f) {
			contentReveal = 1.0f;
		}
	}

	private String contentAnimationKey() {
		if (selectedPage == SidebarPage.MODULES) {
			return "modules:" + selectedCategory.name();
		}

		if (selectedPage == SidebarPage.SEARCH) {
			return "search:" + searchQuery.trim().toLowerCase(java.util.Locale.ROOT);
		}

		return "page:" + selectedPage.name();
	}

	private void drawAnimatedContent(Runnable renderer) {
		float previousAlpha = layerAlpha;
		float previousOffsetY = layerOffsetY;
		float eased = smooth(contentReveal);
		layerAlpha *= eased;
		layerOffsetY += (1.0f - eased) * 8.0f;
		try {
			renderer.run();
		} finally {
			layerAlpha = previousAlpha;
			layerOffsetY = previousOffsetY;
		}
	}

	private int designWidth() {
		return Math.round(this.width / renderScale);
	}

	private int designHeight() {
		return Math.round(this.height / renderScale);
	}

	private int toScreenX(int value) {
		float animated = animationCenterX + (value - animationCenterX) * visualScale;
		return Math.round(animated * renderScale);
	}

	private int toScreenY(int value) {
		float offsetValue = value + layerOffsetY;
		float animated = animationCenterY + (offsetValue - animationCenterY) * visualScale;
		return Math.round(animated * renderScale);
	}

	private int toScreenSize(int value) {
		return Math.max(1, Math.round(value * renderScale * visualScale));
	}

	private float toScreenX(float value) {
		float animated = animationCenterX + (value - animationCenterX) * visualScale;
		return animated * renderScale;
	}

	private float toScreenY(float value) {
		float offsetValue = value + layerOffsetY;
		float animated = animationCenterY + (offsetValue - animationCenterY) * visualScale;
		return animated * renderScale;
	}

	private float toScreenSize(float value) {
		return Math.max(1.0f, value * renderScale * visualScale);
	}

	private int toDesign(double value) {
		return Math.round((float) (value / renderScale));
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX <= x + width && mouseY <= y + height;
	}

	private static float clamp(float value) {
		return Math.max(0.0f, Math.min(1.0f, value));
	}

	private static String hex(Color color) {
		return String.format(java.util.Locale.ROOT, "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static boolean sameColor(Color first, Color second) {
		return first.getRed() == second.getRed() && first.getGreen() == second.getGreen() && first.getBlue() == second.getBlue();
	}

	private Color animatedColor(Color color) {
		int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * visualAlpha * layerAlpha)));
		return new Color(
			color.getRed(),
			color.getGreen(),
			color.getBlue(),
			alpha
		);
	}

	private static float smooth(float value) {
		float clamped = clamp(value);
		return clamped * clamped * (3.0f - 2.0f * clamped);
	}

	private static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private Color guiStyleColor(Color soul, Color glass, Color compact) {
		return "Glass".equals(owner.getGuiStyle()) ? glass : soul;
	}

	private static Color color(int red, int green, int blue, int alpha) {
		return new Color(red, green, blue, alpha);
	}

	private record ThemePreset(String name, Color color) {
	}

	private enum SidebarPage {
		MODULES("", ""),
		SEARCH("Search", "B"),
		CONFIGS("Configs", "I"),
		FRIENDS("Friends", "J"),
		THEMES("Themes", "K");

		private final String displayName;
		private final String icon;

		SidebarPage(String displayName, String icon) {
			this.displayName = displayName;
			this.icon = icon;
		}

		private String displayName() {
			return displayName;
		}

		private String icon() {
			return icon;
		}
	}

	private record ClickZone(int x, int y, int width, int height, MouseAction action) {
		private boolean contains(double mouseX, double mouseY) {
			return inside(mouseX, mouseY, x, y, width, height);
		}
	}

	private record ClipRect(int x, int y, int width, int height) {
	}

	@FunctionalInterface
	private interface MouseAction {
		void run(double mouseX, double mouseY, int button);
	}

	private enum PickerDrag {
		NONE,
		SQUARE,
		HUE
	}
}
