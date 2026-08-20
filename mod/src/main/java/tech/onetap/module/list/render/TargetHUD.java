package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.AutoFlyMace;
import tech.onetap.module.list.combat.BoatAura;
import tech.onetap.module.list.combat.KillAura;
import tech.onetap.module.list.combat.TpAura;
import tech.onetap.module.list.misc.ScoreboardHealth;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeListSetting;
import tech.onetap.module.settings.SliderSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.builders.Builder;
import tech.onetap.util.render.builders.states.QuadColorState;
import tech.onetap.util.render.builders.states.QuadRadiusState;
import tech.onetap.util.render.builders.states.SizeState;
import tech.onetap.util.render.math.Animation;
import tech.onetap.util.render.math.Easing;
import tech.onetap.util.render.math.Scissor;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;
import tech.onetap.util.render.stencil.StencilUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** HUD цели: HP, броня, эффекты. Отдельный модуль вместо "Активный таргет" в Interface. */
@ModuleInformation(moduleName = "TargetHUD", moduleDesc = "HUD цели: HP, броня, эффекты", moduleCategory = ModuleCategory.RENDER)
public class TargetHUD extends Module {

    private static final Identifier TARGET_HUD_GLOW_TEXTURE = Identifier.of("mre", "images/glow.png");

    public final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ник", true),
            new BooleanSetting("HP и дистанция", true),
            new BooleanSetting("Полоса здоровья", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Прочность брони", true),
            new BooleanSetting("Эффекты", true),
            new BooleanSetting("Топ текст", true),
            new BooleanSetting("Таргет худ от темы", false),
            new BooleanSetting("Блюр фона", true),
            new BooleanSetting("Задний фон от темы", false)
    );

    public final SliderSetting backgroundIntensity =
            new SliderSetting("Интенсивность фона", 0.15f, 0.05f, 1.0f, 0.01f);

    private final Draggable targetHUDDrag = DragManager.installDrag(this, "TargetHUD", 130, 130);

    private final Animation animation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation armorAnim = new Animation(Easing.EXPO_OUT, 300);

    private Entity lastTarget;

    private float trailHealthPercent = 1f;
    private float lastHealthPercent = 1f;
    private float lastAbsorptionPercent = 0f;
    private float lastHpRaw = -1f;
    private final List<DamageParticle> damageParticles = new ArrayList<>();

    private final Map<Integer, RecentUse> recentUses = new ConcurrentHashMap<>();
    private record RecentUse(String itemName, long time) {}

    public void drawBackground(float x, float y, float w, float h, float radius, int alpha) {
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.rgba(15, 15, 15, (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, color);
        } else {
            int color = ColorProvider.rgba(15, 15, 15, (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRound(x, y, w, h, radius, color);
        }

        if (elements.isEnabled("Задний фон от темы")) {
            DrawUtil.drawRound(x, y, w, h, radius, getThemeTint(alpha));
        }
    }

    public int getThemeTint(int alpha) {
        int themeColor = ColorProvider.getThemeColor();
        return ColorProvider.setAlpha(themeColor, (int) (100 * (alpha / 255f) * backgroundIntensity.getFloatValue()));
    }

    @Subscribe
    public void onEventHUD(EventHUD e) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        renderTargetHUDMoonward(e.getDrawContext());
    }

    private LivingEntity getTargetHudTarget() {
        KillAura killAura = Instance.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.getTarget() != null && killAura.getTarget().isAlive()) {
            return killAura.getTarget();
        }

        AutoFlyMace autoFlyMaceHud = Instance.get(AutoFlyMace.class);
        if (autoFlyMaceHud != null && autoFlyMaceHud.isEnabled() && autoFlyMaceHud.getTarget() != null && autoFlyMaceHud.getTarget().isAlive()) {
            return autoFlyMaceHud.getTarget();
        }

        BoatAura boatAura = Instance.get(BoatAura.class);
        if (boatAura != null && boatAura.isEnabled() && boatAura.getTarget() != null && boatAura.getTarget().isAlive()) {
            return boatAura.getTarget();
        }

        TpAura tpAura = Instance.get(TpAura.class);
        if (tpAura != null && tpAura.isEnabled() && tpAura.getTarget() != null && tpAura.getTarget().isAlive()) {
            return tpAura.getTarget();
        }

        if (mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }

        return null;
    }

    private void renderTargetHUDMoonward(DrawContext context) {
        LivingEntity target = getTargetHudTarget();

        if (target != null) {
            lastTarget = target;
            animation.run(1);
            armorAnim.run(1);
        } else {
            animation.run(0);
            armorAnim.run(0);
        }

        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null || !(lastTarget instanceof LivingEntity)) return;

        LivingEntity livingEntity = (LivingEntity) lastTarget;
        float x = targetHUDDrag.getX();
        float y = targetHUDDrag.getY();
        float width = 132f;
        float height = 47f;
        float panelRadius = 6f;

        drawBackground(x, y, width, height, panelRadius, (int) (255 * animAlpha));

        float headSize = 28f;
        float headX = x + width - headSize - 4f;
        float headY = y + (height - headSize) / 2f;
        float headRadius = headSize / 2f;

        context.draw();
        StencilUtil.push();
        DrawUtil.drawRound(headX, headY, headSize, headSize, headRadius, -1);
        StencilUtil.read(1);

        float currentAnimScale = (float) armorAnim.getValue();
        float entityScale = (headSize / 1.3f) * currentAnimScale;

        if (entityScale > 0.1f) {
            float entityX = headX + headSize / 2f;
            float entityY = headY + headSize + 15f * currentAnimScale;
            float elytra = livingEntity.isGliding() ? -10f : 0f;
            if (livingEntity.isGliding()) entityY -= 20f * currentAnimScale;
            drawEntity(entityX - elytra, entityY + elytra, entityScale, -33.0F, 0.0F, livingEntity);
        }

        context.draw();
        StencilUtil.pop();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();

        Builder.border()
                .size(new SizeState(headSize + 1.5f, headSize + 1.5f))
                .radius(new QuadRadiusState(headRadius))
                .color(new QuadColorState(ColorProvider.rgba(60, 60, 60, (int) (255 * animAlpha))))
                .thickness(1f)
                .smoothness(1f, 0.5f)
                .build()
                .render(context.getMatrices().peek().getPositionMatrix(), headX - 0.75f, headY - 0.75f);

        float textX = x + 6f;
        float textY = y + 7f;
        int textColor = ColorProvider.rgba(222, 222, 222, (int) (255 * animAlpha));
        float rightTextLimit = headX - 3f;

        if (elements.isEnabled("Ник")) {
            String rawName = livingEntity.getName().getString();
            String name = transliterate(rawName);

            Scissor.push();
            Scissor.setFromComponentCoordinates(textX, y, rightTextLimit - textX, height);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), name, textX, textY - 2f, textColor, 8.25f);
            Scissor.unset();
            Scissor.pop();
        }

        float currentHp = Math.max(0f, livingEntity.getHealth());
        ScoreboardHealth sbh = Onetap.getInstance().getModuleStorage().get(ScoreboardHealth.class);
        if (sbh != null && sbh.isEnabled() && livingEntity instanceof AbstractClientPlayerEntity playerEntity) {
            float scoreboardHp = sbh.getRealHp(playerEntity);
            if (scoreboardHp != -1) {
                currentHp = scoreboardHp;
            }
        }

        float absorptionHP = Math.max(0f, livingEntity.getAbsorptionAmount());
        float maxHealth = Math.max(1f, livingEntity.getMaxHealth());

        String hpText = String.format(Locale.US, "%.1f", currentHp);
        String absorpText = String.format(Locale.US, "%.1f", absorptionHP);

        if (elements.isEnabled("HP и дистанция")) {
            float distance = mc.player.distanceTo(livingEntity);
            String absorptionText = absorptionHP > 0f ? " +" + absorpText : "";
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "HP: " + hpText + absorptionText + "  |  "
                            + String.format(Locale.US, "%.1fm", distance),
                    textX, textY + 10f, textColor, 7.5f);
        }

        if (elements.isEnabled("Топ текст")) {
            float myTotalHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float targetTotalHp = currentHp + absorptionHP;
            float damage = 1.0f;
            ItemStack weapon = mc.player.getMainHandStack();

            if (weapon != null && !weapon.isEmpty()) {
                String itemName = net.minecraft.registry.Registries.ITEM.getId(weapon.getItem()).getPath();
                if (itemName.contains("netherite_sword")) damage += 7.0f;
                else if (itemName.contains("diamond_sword")) damage += 6.0f;
                else if (itemName.contains("iron_sword")) damage += 5.0f;
                else if (itemName.contains("stone_sword")) damage += 4.0f;
                else if (itemName.contains("golden_sword") || itemName.contains("wooden_sword")) damage += 3.0f;
                else if (itemName.contains("netherite_axe")) damage += 9.0f;
                else if (itemName.contains("diamond_axe") || itemName.contains("iron_axe") || itemName.contains("stone_axe")) damage += 8.0f;
                else if (itemName.contains("golden_axe") || itemName.contains("wooden_axe")) damage += 6.0f;
                if (weapon.hasGlint()) damage += 3.0f;
            }

            if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
                damage += 3.0f * (mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
            }
            if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
                damage -= 4.0f * (mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1);
            }

            float potentialDamage = damage * 1.5f;
            float targetArmor = livingEntity.getArmor();
            float targetToughness = (float) livingEntity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);
            float f = 2.0F + targetToughness / 4.0F;
            float g = MathHelper.clamp(targetArmor - potentialDamage / f, targetArmor * 0.2F, 20.0F);
            potentialDamage = potentialDamage * (1.0F - g / 25.0F);

            int epf = 0;
            for (ItemStack armorPiece : livingEntity.getArmorItems()) {
                if (!armorPiece.isEmpty() && armorPiece.hasGlint()) epf += 4;
            }
            epf = Math.min(20, epf);
            if (epf > 0) potentialDamage = potentialDamage * (1.0F - (epf * 0.04F));

            String topText;
            int topColor;
            if (targetTotalHp <= potentialDamage - 1 && targetTotalHp > 0) {
                topText = "ONETAP";
                topColor = ColorProvider.rgba(255, 75, 75, (int) (255 * animAlpha));
            } else {
                topText = myTotalHp >= targetTotalHp ? "WINNING" : "LOSING";
                topColor = ColorProvider.rgba(255, 255, 255, (int) (255 * animAlpha));
            }

            float topTextWidth = Fonts.SFMEDIUM.get().getWidth(topText, 7.0f);
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), topText, x + (width / 2f) - (topTextWidth / 2f), y - 41f, topColor, 8.0f);
        }

        float barX = textX - 1f;
        RecentUse recentUse = recentUses.get(livingEntity.getId());
        if (recentUse != null && System.currentTimeMillis() - recentUse.time() <= 10_000L) {
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), "10с: " + recentUse.itemName(),
                    textX, textY + 20f, textColor, 6.5f);
        } else if (recentUse != null) {
            recentUses.remove(livingEntity.getId());
        }

        if (lastHpRaw == -1f || lastTarget != livingEntity) {
            lastHpRaw = currentHp;
            damageParticles.clear();
        }

        if (currentHp < lastHpRaw) {
            int count = MathHelper.clamp((int) ((lastHpRaw - currentHp) * 4), 10, 25);
            java.awt.Color pColor = getHealthBarColor(currentHp, maxHealth);
            float lostHpWidth = barWidth() * MathHelper.clamp((lastHpRaw - currentHp) / maxHealth, 0f, 1f);
            float currentHpWidth = barWidth() * MathHelper.clamp(currentHp / maxHealth, 0f, 1f);

            for (int i = 0; i < count; i++) {
                float spawnX = barX + currentHpWidth + (float) (Math.random() * lostHpWidth);
                float spawnY = barY() + barHeight() / 2f;
                damageParticles.add(new DamageParticle(spawnX, spawnY, pColor.getRGB()));
            }
            lastHpRaw = currentHp;
        } else if (currentHp > lastHpRaw) {
            lastHpRaw = currentHp;
        }

        damageParticles.removeIf(p -> p.getAlpha() <= 0);
        if (!damageParticles.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, TARGET_HUD_GLOW_TEXTURE);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

            for (DamageParticle p : damageParticles) {
                p.update();
                float pAlpha = p.getAlpha() * animAlpha;
                int c = ColorProvider.setAlpha(p.color, (int) (pAlpha * 255));
                float half = p.getSize() / 2f;

                buffer.vertex(matrix, p.x - half, p.y - half, 0).texture(0, 0).color(c);
                buffer.vertex(matrix, p.x - half, p.y + half, 0).texture(0, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y + half, 0).texture(1, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y - half, 0).texture(1, 0).color(c);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }

        float barWidthValue = barWidth();
        float barYValue = barY();
        float barHeightValue = barHeight();

        DrawUtil.drawRound(barX, barYValue, barWidthValue, barHeightValue, 1.5f, ColorProvider.rgba(60, 60, 60, (int) (255 * animAlpha)));

        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
        float absorptionPercent = MathHelper.clamp(absorptionHP / maxHealth, 0f, 1f);

        lastHealthPercent += (hpPercent - lastHealthPercent) * 0.25f;
        lastAbsorptionPercent += (absorptionPercent - lastAbsorptionPercent) * 0.15f;
        trailHealthPercent += (lastHealthPercent - trailHealthPercent) * 0.008f;

        float hpWidth = barWidthValue * lastHealthPercent;
        float trailWidth = barWidthValue * trailHealthPercent;
        float absWidth = barWidthValue * lastAbsorptionPercent;

        int hpLeft, hpRight;
        if (elements.isEnabled("Таргет худ от темы")) {
            hpRight = ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (255 * animAlpha));
            hpLeft = ColorProvider.setAlpha(ColorProvider.getThemeColorTwo(), (int) (255 * animAlpha));
        } else {
            java.awt.Color hpCol = getHealthBarColor(currentHp, maxHealth);
            hpLeft = ColorProvider.rgba((int) (hpCol.getRed() * 0.5), (int) (hpCol.getGreen() * 0.5), (int) (hpCol.getBlue() * 0.5), (int) (255 * animAlpha));
            hpRight = ColorProvider.rgba(hpCol.getRed(), hpCol.getGreen(), hpCol.getBlue(), (int) (255 * animAlpha));
        }

        if (trailWidth > hpWidth) {
            DrawUtil.drawRound(barX, barYValue, trailWidth, barHeightValue, 1.5f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (135 * animAlpha)));
        }
        if (hpWidth > 0) {
            DrawUtil.drawRound(barX, barYValue, hpWidth, barHeightValue, 1.5f, hpLeft, hpLeft, hpRight, hpRight);
        }
        if (absWidth > 0) {
            int absBase = ColorProvider.rgba(255, 222, 0, (int) (255 * animAlpha));
            int absLeft = ColorProvider.rgba(180, 155, 0, (int) (255 * animAlpha));
            DrawUtil.drawRound(barX, barYValue, absWidth, barHeightValue, 1.5f, absLeft, absLeft, absBase, absBase);
        }

        float armorAlpha = (float) armorAnim.getValue();
        if (armorAlpha > 0.05f && elements.isEnabled("Броня")) {
            List<ItemStack> items = new ArrayList<>();
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET));
            items.add(livingEntity.getMainHandStack());
            items.add(livingEntity.getOffHandStack());
            items.removeIf(ItemStack::isEmpty);

            if (!items.isEmpty()) {
                float itemScale = 0.7f;
                float slotSize = 14f * itemScale;
                float padding = 2f;
                float totalArmorWidth = (items.size() * slotSize) + ((items.size() - 1) * padding);
                float itemX = x + (width - totalArmorWidth) / 2f - 18f;
                float itemY = y - slotSize - 20f;

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                for (ItemStack stack : items) {
                    context.getMatrices().push();
                    context.getMatrices().translate(itemX, itemY, 0);
                    context.getMatrices().scale(armorAlpha * itemScale, armorAlpha * itemScale, 1f);
                    context.drawItem(stack, 0, 0);
                    context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                    if (elements.isEnabled("Прочность брони") && stack.isDamageable() && stack.getMaxDamage() > 0) {
                        int durability = Math.round((stack.getMaxDamage() - stack.getDamage()) * 100f / stack.getMaxDamage());
                        context.drawText(mc.textRenderer, durability + "%", 0, 17, 0xFFFFFFFF, true);
                    }
                    context.getMatrices().pop();
                    itemX += slotSize + padding;
                }
                context.getMatrices().pop();
            }
        }

        if (elements.isEnabled("Эффекты")) {
            renderEffects(context, livingEntity, x, y + height + 3f, animAlpha);
        }

        targetHUDDrag.setWidth(width);
        targetHUDDrag.setHeight(height);
    }

    private float barWidth() {
        return 132f - 28f - 12f;
    }

    private float barY() {
        return targetHUDDrag.getY() + 38f;
    }

    private float barHeight() {
        return 5f;
    }

    private void renderEffects(DrawContext context, LivingEntity livingEntity, float x, float y, float animAlpha) {
        List<StatusEffectInstance> effects;
        try {
            effects = new ArrayList<>(livingEntity.getStatusEffects());
        } catch (Exception ignored) {
            return;
        }
        if (effects.isEmpty()) return;

        float rowH = 8f;
        float panelW = 132f;
        float totalH = effects.size() * rowH + 4f;

        DrawUtil.drawRoundBlur(x, y, panelW, totalH, 5f, ColorProvider.rgba(0, 0, 0, 120), 10f);
        DrawUtil.drawRound(x, y, panelW, totalH, 5f, ColorProvider.rgba(14, 15, 20, (int) (225 * animAlpha)));

        float ty = y + 3f;
        for (StatusEffectInstance inst : effects) {
            try {
                String name = inst.getEffectType().value().getName().getString();
                String amp = inst.getAmplifier() > 0 ? " " + (inst.getAmplifier() + 1) : "";

                String time;
                if (inst.getDuration() == StatusEffectInstance.INFINITE) {
                    time = "**:**";
                } else {
                    int totalSec = Math.max(0, inst.getDuration() / 20);
                    time = String.format("%d:%02d", totalSec / 60, totalSec % 60);
                }
                String text = name + amp + " " + time;

                int color = inst.getEffectType().value().getColor();
                DrawUtil.drawRound(x + 2.5f, ty + 1.5f, 1.5f, 5f, 0.5f, color);

                Scissor.push();
                Scissor.setFromComponentCoordinates(x, y, panelW, totalH);
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), text, x + 6f, ty,
                        ColorProvider.rgba(230, 235, 245, (int) (255 * animAlpha)), 6.5f);
                Scissor.unset();
                Scissor.pop();

                ty += rowH;
            } catch (Exception ignored) {
            }
        }
    }

    private String transliterate(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = switch (c) {
                case 'а', 'А' -> c == 'А' ? "A" : "a";
                case 'б', 'Б' -> c == 'Б' ? "B" : "b";
                case 'в', 'В' -> c == 'В' ? "V" : "v";
                case 'г', 'Г' -> c == 'Г' ? "G" : "g";
                case 'д', 'Д' -> c == 'Д' ? "D" : "d";
                case 'е', 'Е' -> c == 'Е' ? "E" : "e";
                case 'ё', 'Ё' -> c == 'Ё' ? "Yo" : "yo";
                case 'ж', 'Ж' -> c == 'Ж' ? "Zh" : "zh";
                case 'з', 'З' -> c == 'З' ? "Z" : "z";
                case 'и', 'И' -> c == 'И' ? "I" : "i";
                case 'й', 'Й' -> c == 'Й' ? "Y" : "y";
                case 'к', 'К' -> c == 'К' ? "K" : "k";
                case 'л', 'Л' -> c == 'Л' ? "L" : "l";
                case 'м', 'М' -> c == 'М' ? "M" : "m";
                case 'н', 'Н' -> c == 'Н' ? "N" : "n";
                case 'о', 'О' -> c == 'О' ? "O" : "o";
                case 'п', 'П' -> c == 'П' ? "P" : "p";
                case 'р', 'Р' -> c == 'Р' ? "R" : "r";
                case 'с', 'С' -> c == 'С' ? "S" : "s";
                case 'т', 'Т' -> c == 'Т' ? "T" : "t";
                case 'у', 'У' -> c == 'У' ? "U" : "u";
                case 'ф', 'Ф' -> c == 'Ф' ? "F" : "f";
                case 'х', 'Х' -> c == 'Х' ? "Kh" : "kh";
                case 'ц', 'Ц' -> c == 'Ц' ? "Ts" : "ts";
                case 'ч', 'Ч' -> c == 'Ч' ? "Ch" : "ch";
                case 'ш', 'Ш' -> c == 'Ш' ? "Sh" : "sh";
                case 'щ', 'Щ' -> c == 'Щ' ? "Shch" : "shch";
                case 'ъ', 'Ъ' -> "";
                case 'ы', 'Ы' -> c == 'Ы' ? "Y" : "y";
                case 'ь', 'Ь' -> "";
                case 'э', 'Э' -> c == 'Э' ? "E" : "e";
                case 'ю', 'Ю' -> c == 'Ю' ? "Yu" : "yu";
                case 'я', 'Я' -> c == 'Я' ? "Ya" : "ya";
                default -> String.valueOf(c);
            };
            result.append(replacement);
        }
        return result.toString();
    }

    public void drawEntity(float x, float y, float scale, float yawAngle, float pitchAngle, net.minecraft.entity.LivingEntity entity) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(-scale, scale, scale);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawAngle));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchAngle));

        float bodyYaw = entity.bodyYaw;
        float prevBodyYaw = entity.prevBodyYaw;
        float headYaw = entity.headYaw;
        float prevHeadYaw = entity.prevHeadYaw;
        float yaw = entity.getYaw();
        float prevYaw = entity.prevYaw;
        float pitch = entity.getPitch();
        float prevPitch = entity.prevPitch;

        entity.bodyYaw = 0;
        entity.prevBodyYaw = 0;
        entity.headYaw = 0;
        entity.prevHeadYaw = 0;
        entity.setYaw(0);
        entity.prevYaw = 0;
        entity.setPitch(0);
        entity.prevPitch = 0;

        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
        net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, tickDelta, matrices, immediate, 0x00F000F0);

        immediate.draw();
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

        entity.bodyYaw = bodyYaw;
        entity.prevBodyYaw = prevBodyYaw;
        entity.headYaw = headYaw;
        entity.prevHeadYaw = prevHeadYaw;
        entity.setYaw(yaw);
        entity.prevYaw = prevYaw;
        entity.setPitch(pitch);
        entity.prevPitch = prevPitch;

        matrices.pop();
    }

    private static class DamageParticle {
        float x, y, vx, vy, baseSize;
        long spawnTime, maxLife;
        int color;

        DamageParticle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            double angle = Math.random() * Math.PI * 2;
            double speed = Math.random() * 2.0 + 0.5;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.baseSize = (float) (Math.random() * 7 + 6);
            this.spawnTime = System.currentTimeMillis();
            this.maxLife = (long) (Math.random() * 700 + 800);
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.85f;
            vy *= 0.85f;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            if (elapsed >= maxLife) return 0;
            return 1f - ((float) elapsed / maxLife);
        }

        float getSize() {
            return baseSize * getAlpha();
        }
    }

    private java.awt.Color getHealthBarColor(float currentHp, float maxHp) {
        float ratio = MathHelper.clamp(currentHp / maxHp, 0.0f, 1.0f);
        java.awt.Color colorAtMax = new java.awt.Color(44, 246, 53);
        java.awt.Color colorAt56 = new java.awt.Color(160, 228, 69);
        java.awt.Color colorAt38 = new java.awt.Color(222, 191, 79);
        java.awt.Color colorAt32 = new java.awt.Color(233, 150, 87);
        java.awt.Color colorAt11 = new java.awt.Color(255, 125, 98);

        if (ratio >= 0.56f) {
            float t = MathHelper.clamp((1.0f - ratio) / (1.0f - 0.56f), 0.0f, 1.0f);
            return lerpColor(colorAtMax, colorAt56, t);
        } else if (ratio >= 0.38f) {
            float t = MathHelper.clamp((0.56f - ratio) / (0.56f - 0.38f), 0.0f, 1.0f);
            return lerpColor(colorAt56, colorAt38, t);
        } else if (ratio >= 0.32f) {
            float t = MathHelper.clamp((0.38f - ratio) / (0.38f - 0.32f), 0.0f, 1.0f);
            return lerpColor(colorAt38, colorAt32, t);
        } else if (ratio >= 0.11f) {
            float t = MathHelper.clamp((0.32f - ratio) / (0.32f - 0.11f), 0.0f, 1.0f);
            return lerpColor(colorAt32, colorAt11, t);
        } else {
            return colorAt11;
        }
    }

    private java.awt.Color lerpColor(java.awt.Color a, java.awt.Color b, float t) {
        return new java.awt.Color(
                (int) (a.getRed() + t * (b.getRed() - a.getRed())),
                (int) (a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()))
        );
    }
}