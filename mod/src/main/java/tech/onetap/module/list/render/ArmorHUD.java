package tech.onetap.module.list.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import tech.onetap.event.list.EventHUD;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.draggable.DragManager;
import tech.onetap.util.draggable.Draggable;
import tech.onetap.util.render.msdf.Fonts;
import tech.onetap.util.render.providers.ColorProvider;
import tech.onetap.util.render.renderers.DrawUtil;

@ModuleInformation(moduleName = "ArmorHUD", moduleDesc = "Прочность надетой брони", moduleCategory = ModuleCategory.RENDER)
public class ArmorHUD extends Module {
    private final Draggable drag = DragManager.installDrag(this, "ArmorHUD", 12, 90);

    @Subscribe
    public void onEventHUD(EventHUD event) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        DrawContext context = event.getDrawContext();
        float x = drag.getX(), y = drag.getY();
        float width = 112f, height = 61f;
        DrawUtil.drawRoundBlur(x, y, width, height, 5f, ColorProvider.rgba(0, 0, 0, 135), 12f);
        DrawUtil.drawRound(x, y, width, height, 5f, ColorProvider.rgba(13, 14, 18, 215));
        DrawUtil.drawText(Fonts.SFBOLD.get(), "Armor", x + 8f, y + 6f,
                ColorProvider.rgba(245, 245, 250, 255), 7.5f);

        ItemStack[] armor = {
                mc.player.getInventory().armor.get(3),
                mc.player.getInventory().armor.get(2),
                mc.player.getInventory().armor.get(1),
                mc.player.getInventory().armor.get(0)
        };
        for (int i = 0; i < armor.length; i++) {
            ItemStack stack = armor[i];
            float rowY = y + 17f + i * 10f;
            int percent = stack.isEmpty() || stack.getMaxDamage() <= 0 ? 0 :
                    MathHelper.clamp(Math.round((stack.getMaxDamage() - stack.getDamage()) * 100f / stack.getMaxDamage()), 0, 100);
            context.getMatrices().push();
            context.getMatrices().translate(x + 5f, rowY - 3f, 0);
            context.getMatrices().scale(0.55f, 0.55f, 1f);
            if (!stack.isEmpty()) context.drawItem(stack, 0, 0);
            context.getMatrices().pop();

            float barX = x + 18f, barW = 70f;
            DrawUtil.drawRound(barX, rowY, barW, 3.5f, 1.7f, ColorProvider.rgba(42, 43, 51, 255));
            float fill = barW * percent / 100f;
            if (fill > 0) {
                DrawUtil.drawRound(barX, rowY, fill, 3.5f, 1.7f,
                        percent < 25 ? ColorProvider.rgba(255, 78, 92, 255) : ColorProvider.getThemeColor());
            }
            DrawUtil.drawText(Fonts.SFMEDIUM.get(), percent + "%", x + 91f, rowY - 2f,
                    ColorProvider.rgba(235, 235, 240, 255), 6.5f);
        }
        drag.setWidth(width);
        drag.setHeight(height);
    }
}
