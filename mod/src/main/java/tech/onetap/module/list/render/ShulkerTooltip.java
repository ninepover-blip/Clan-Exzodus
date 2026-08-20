package tech.onetap.module.list.render;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleInformation(moduleName = "ShulkerTooltip", moduleDesc = "Содержимое шалкера в тултипе", moduleCategory = ModuleCategory.RENDER)
public class ShulkerTooltip extends Module {

    private final BooleanSetting compact = new BooleanSetting("Компактно", true);
    private final BooleanSetting showEmpty = new BooleanSetting("Пустые слоты", false);

    public boolean canPreview(ItemStack stack) {
        return isEnabled() && stack.getItem() instanceof BlockItem blockItem && isShulker(blockItem);
    }

    public List<Text> getPreviewLines(ItemStack stack) {
        List<Text> lines = new ArrayList<>();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null || container.equals(ContainerComponent.DEFAULT)) return lines;

        List<ItemStack> slots = container.stream().toList();
        boolean any = false;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty()) {
                if (!compact.getValue() && showEmpty.getValue()) {
                    lines.add(Text.literal("[" + i + "]").formatted(Formatting.DARK_GRAY));
                }
                continue;
            }
            any = true;
            if (compact.getValue()) {
                lines.add(Text.literal(slot.getCount() + "x ")
                        .formatted(Formatting.GRAY)
                        .append(slot.getName().copy().formatted(Formatting.WHITE)));
            } else {
                lines.add(Text.literal("[" + i + "] " + slot.getCount() + "x ")
                        .formatted(Formatting.GRAY)
                        .append(slot.getName().copy().formatted(Formatting.WHITE)));
            }
        }
        if (!any && !showEmpty.getValue()) {
            return new ArrayList<>();
        }
        return lines;
    }

    private boolean isShulker(BlockItem blockItem) {
        return blockItem.asItem() == Items.SHULKER_BOX
                || blockItem.asItem() == Items.WHITE_SHULKER_BOX
                || blockItem.asItem() == Items.ORANGE_SHULKER_BOX
                || blockItem.asItem() == Items.MAGENTA_SHULKER_BOX
                || blockItem.asItem() == Items.LIGHT_BLUE_SHULKER_BOX
                || blockItem.asItem() == Items.YELLOW_SHULKER_BOX
                || blockItem.asItem() == Items.LIME_SHULKER_BOX
                || blockItem.asItem() == Items.PINK_SHULKER_BOX
                || blockItem.asItem() == Items.GRAY_SHULKER_BOX
                || blockItem.asItem() == Items.LIGHT_GRAY_SHULKER_BOX
                || blockItem.asItem() == Items.CYAN_SHULKER_BOX
                || blockItem.asItem() == Items.PURPLE_SHULKER_BOX
                || blockItem.asItem() == Items.BLUE_SHULKER_BOX
                || blockItem.asItem() == Items.BROWN_SHULKER_BOX
                || blockItem.asItem() == Items.GREEN_SHULKER_BOX
                || blockItem.asItem() == Items.RED_SHULKER_BOX
                || blockItem.asItem() == Items.BLACK_SHULKER_BOX;
    }
}
