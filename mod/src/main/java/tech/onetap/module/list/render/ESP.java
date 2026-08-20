package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.EquipmentSlot;
import org.joml.Vector4f;
import tech.onetap.Onetap;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.combat.AntiBot;
import tech.onetap.module.list.misc.NameProtect;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.MultiSelectSetting;
import tech.onetap.module.settings.impl.Theme;
import tech.onetap.module.settings.impl.ThemeManager;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.math.ProjectionUtil;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.renderers.DrawUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleInformation(moduleName = "ESP", moduleDesc = "Полоска хп, эффекты и инвентарь вокруг сущностей", moduleCategory = ModuleCategory.RENDER)
public class ESP extends Module {

    public final MultiSelectSetting remove = new MultiSelectSetting("Убрать", "Полоску хп", "Список эффектов");
    public final MultiSelectSetting targets = new MultiSelectSetting("Отображать", "Себя", "Игроки", "Предметы", "Мобы");
    public final BooleanSetting showOffHand = new BooleanSetting("Чекать левую руку", true);

    private final Map<Entity, Vector4f> positions = new HashMap<>();

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    public ESP() {
        targets.selected("Себя", "Игроки");
    }

    @Subscribe
    private void onHud(EventHUD event) {
        if (mc.world == null || mc.player == null) return;

        positions.clear();

        float partialTicks = event.getRenderTickCounter().getTickDelta(true);
        DrawContext context = event.getDrawContext();

        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        int themeColor = theme.getStaticColorTheme(0);
        int themeDark = Theme.interpolateColorClean(themeColor, 0xFF000000, 0.6f);

        for (Entity entity : mc.world.getEntities()) {
            if (!isValid(entity)) continue;
            if (!(entity instanceof PlayerEntity && entity != mc.player && targets.isSelected("Игроки")
                    || entity instanceof ItemEntity && targets.isSelected("Предметы")
                    || (entity instanceof AnimalEntity || entity instanceof MobEntity) && targets.isSelected("Мобы")
                    || entity == mc.player && targets.isSelected("Себя") && !mc.options.getPerspective().isFirstPerson()
            )) continue;

            double x = entity.prevX + (entity.getX() - entity.prevX) * partialTicks;
            double y = entity.prevY + (entity.getY() - entity.prevY) * partialTicks;
            double z = entity.prevZ + (entity.getZ() - entity.prevZ) * partialTicks;

            Box box = entity.getBoundingBox();
            double sizeX = box.getLengthX();
            double sizeY = box.getLengthY();
            double sizeZ = box.getLengthZ();

            double minX = x - sizeX / 2f, minY = y, minZ = z - sizeZ / 2f;
            double maxX = x + sizeX / 2f, maxY = y + sizeY, maxZ = z + sizeZ / 2f;

            Vector4f position = null;
            for (int i = 0; i < 8; i++) {
                Vector2f vector = ProjectionUtil.project(
                        i % 2 == 0 ? minX : maxX,
                        (i / 2) % 2 == 0 ? minY : maxY,
                        (i / 4) % 2 == 0 ? minZ : maxZ);

                if (position == null) {
                    position = new Vector4f(vector.getX(), vector.getY(), 1, 1);
                } else {
                    position.x = Math.min(vector.getX(), position.x);
                    position.y = Math.min(vector.getY(), position.y);
                    position.z = Math.max(vector.getX(), position.z);
                    position.w = Math.max(vector.getY(), position.w);
                }
            }

            positions.put(entity, position);
        }

        for (Map.Entry<Entity, Vector4f> entry : positions.entrySet()) {
            Vector4f position = entry.getValue();
            Entity entity = entry.getKey();

            if (entity instanceof LivingEntity living) {
                boolean friend = FriendRepository.isFriend(entity.getName().getString());

                float hpOffset = 3f;
                float out = 0.5f;
                if (!remove.isSelected("Полоску хп")) {
                    float hp = getDisplayHp(living);
                    float maxHp = getDisplayMaxHp(living);
                    float barX = position.x - hpOffset;
                    float barY = position.y;
                    float barW = 1f;
                    float barH = position.w - position.y;

                    DrawUtil.drawRound(barX - out, barY - out, barW + out * 2, barH + out * 2, 0, rgba(0, 0, 0, 128));
                    DrawUtil.drawRound(barX, barY, barW, barH, 0, rgba(0, 0, 0, 128));

                    float fillY = barY + barH * (1 - MathHelper.clamp(hp / maxHp, 0, 1));
                    float fillH = barH - (fillY - barY);
                    int barTop = friend ? rgba(144, 238, 144, 255) : themeColor;
                    int barBottom = friend ? rgba(0, 139, 0, 255) : themeDark;
                    DrawUtil.drawRound(barX, fillY, barW, fillH, 0, barTop, barBottom, barBottom, barTop);
                }
            }
        }

        for (Map.Entry<Entity, Vector4f> entry : positions.entrySet()) {
            Entity entity = entry.getKey();
            Vector4f position = entry.getValue();

            if (entity instanceof LivingEntity living) {
                float width = position.z - position.x;
                float hp = getDisplayHp(living);
                float maxHp = getDisplayMaxHp(living);

                // Display name keeps the server's donation/clan prefix and the real player name.
                Text name = living.getDisplayName();
                boolean nameFriend = FriendRepository.isFriend(living.getNameForScoreboard());
                int nameColor = nameFriend ? rgba(85, 255, 85, 255) : -1;
                float nameLength = mc.textRenderer.getWidth(name);

                MatrixStack matrices = context.getMatrices();
                matrices.push();
                glCenteredScale(matrices, position.x + width / 2f - nameLength / 2f, position.y - 9, nameLength, 10, 1.0f);
                context.drawText(mc.textRenderer, name, (int) (position.x + width / 2f - nameLength / 2f), (int) (position.y - 12.5f), nameColor, false);
                matrices.pop();

                if (!remove.isSelected("Список эффектов")) {
                    drawPotions(context, living, position.z + 2, position.y);
                }
                drawItems(context, living, (int) (position.x + width / 2f), (int) (position.y - 14.5f));
            } else if (entity instanceof ItemEntity item) {
                float width = position.z - position.x;
                String displayName = item.getStack().getName().getString() + (item.getStack().getCount() < 1 ? "" : " x" + item.getStack().getCount());
                float length = mc.textRenderer.getWidth(displayName);

                MatrixStack matrices = context.getMatrices();
                matrices.push();
                glCenteredScale(matrices, position.x + width / 2f - length / 2f, position.y - 7, length, 10, 0.5f);
                context.drawText(mc.textRenderer, displayName, (int) (position.x + width / 2f - length / 2f), (int) (position.y - 7), -1, false);
                matrices.pop();
            }
        }
    }

    private Text buildNameplate(LivingEntity living, boolean friend, float hp, float maxHp) {
        Text name;
        NameProtect nameProtect = Onetap.getInstance().getModuleStorage().get(NameProtect.class);
        if (friend && nameProtect != null && nameProtect.isEnabled()) {
            name = Text.literal("[F] ").formatted(Formatting.GREEN)
                    .append(Text.literal("protected").formatted(Formatting.RED));
        } else {
            name = friend
                    ? Text.literal("[F] ").formatted(Formatting.GREEN).append(living.getDisplayName())
                    : living.getDisplayName();
        }

        if (living instanceof PlayerEntity && ((PlayerEntity) living).isCreative()) {
            name = name.copy().append(Text.literal(" [").formatted(Formatting.GRAY))
                    .append(Text.literal("GM").formatted(Formatting.RED))
                    .append(Text.literal("]").formatted(Formatting.GRAY));
        } else {
            int showHp = isFuntime() ? (int) hp : (int) hp + (int) living.getAbsorptionAmount();
            name = name.copy().append(Text.literal(" [").formatted(Formatting.GRAY))
                    .append(Text.literal(String.valueOf(showHp)).formatted(Formatting.RED))
                    .append(Text.literal("]").formatted(Formatting.GRAY));
        }
        return name;
    }

    private float getDisplayHp(LivingEntity living) {
        if (living instanceof PlayerEntity && isFuntime()) {
            Scoreboard scoreboard = living.getWorld().getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (objective != null) {
                var score = scoreboard.getScore(living, objective);
                if (score != null) {
                    return score.getScore();
                }
            }
        }
        return living.getHealth();
    }

    private float getDisplayMaxHp(LivingEntity living) {
        return living instanceof PlayerEntity && isFuntime() ? 20 : living.getMaxHealth();
    }

    private boolean isFuntime() {
        return mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.contains("funtime");
    }

    private void drawPotions(DrawContext context, LivingEntity entity, float posX, float posY) {
        for (StatusEffectInstance effectInstance : entity.getStatusEffects()) {
            int amp = effectInstance.getAmplifier() + 1;
            String ampStr = "";
            if (amp >= 1 && amp <= 9) {
                ampStr = " " + amp;
            }

            String text = getDurationString(effectInstance) + " - "
                    + Text.translatable(effectInstance.getEffectType().value().getTranslationKey()).getString() + ampStr;

            try {
                String path = effectInstance.getEffectType().getKey().orElseThrow().getValue().getPath();
                Identifier icon = Identifier.ofVanilla("textures/mob_effect/" + path + ".png");
                context.drawTexture(RenderLayer::getGuiTextured, icon, (int) posX, (int) (posY - 0.5f), 0, 0, 8, 8, 18, 18);
            } catch (Exception ignored) {
            }

            DrawUtil.drawText(Fonts.SFREGULAR.get(), text, posX + 8, posY, -1, 6);
            posY += 7;
        }
    }

    private String getDurationString(StatusEffectInstance effect) {
        if (effect.getDuration() == StatusEffectInstance.INFINITE) {
            return "**:**";
        }
        int total = (int) Math.ceil(effect.getDuration() / 20.0);
        int minutes = total / 60;
        int seconds = total % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void drawItems(DrawContext context, LivingEntity entity, int posX, int posY) {
        int size = 8;
        int padding = 6;

        List<ItemStack> items = new ArrayList<>();
        ItemStack mainStack = entity.getMainHandStack();
        if (!mainStack.isEmpty()) {
            items.add(mainStack);
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack itemStack = entity.getEquippedStack(slot);
            if (!itemStack.isEmpty()) {
                items.add(itemStack);
            }
        }
        ItemStack offStack = entity.getOffHandStack();
        if (!offStack.isEmpty() && showOffHand.getValue()) {
            items.add(offStack);
        }

        posX -= (items.size() * (size + padding)) / 2f;

        for (ItemStack itemStack : items) {
            if (itemStack.isEmpty()) continue;

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            glCenteredScale(matrices, posX, posY - 5, size / 2f, size / 2f, 0.5f);
            context.drawItem(itemStack, posX, posY - 5);
            matrices.pop();

            int textPosX = posX + size + padding;

            if (itemStack == offStack) {
                Text itemNameComponent = itemStack.getName();
                int textColor = itemNameComponent.getStyle().getColor() != null ? itemNameComponent.getStyle().getColor().getRgb() : 0xFFFFFF;

                float length = mc.textRenderer.getWidth(itemNameComponent);
                matrices.push();
                glCenteredScale(matrices, textPosX + 25, posY, length, 7, 0.8f);
                context.drawText(mc.textRenderer, itemNameComponent, (int) (textPosX - 75F - length / 2f), (int) (posY - 16.5F), textColor, false);
                matrices.pop();
            }

            posX += size + padding;
        }
    }

    public boolean isValid(Entity e) {
        AntiBot antiBot = Onetap.getInstance().getModuleStorage().get(AntiBot.class);
        if (e instanceof PlayerEntity && antiBot != null && antiBot.isBot((PlayerEntity) e)) {
            return false;
        }
        return isInView(e);
    }

    public boolean isInView(Entity ent) {
        if (mc.worldRenderer == null || mc.worldRenderer.frustum == null) {
            return true;
        }
        return mc.worldRenderer.frustum.isVisible(ent.getBoundingBox());
    }

    public void glCenteredScale(MatrixStack matrices, final float x, final float y, final float w, final float h, final float f) {
        matrices.translate(x + w / 2, y + h / 2, 0);
        matrices.scale(f, f, 1);
        matrices.translate(-x - w / 2, -y - h / 2, 0);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
