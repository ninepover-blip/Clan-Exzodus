package tech.onetap.module.list.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import tech.onetap.event.list.EventPlayerUpdate;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.math.StopWatch;
import tech.onetap.util.player.other.InventoryUtil;

@ModuleInformation(moduleName = "Auto Armor", moduleDesc = "Автоматически экипирует броню", moduleCategory = ModuleCategory.COMBAT)
public class AutoArmor extends Module {

    private static final double REPLACE_AT_DURABILITY = 0.07;

    private final StopWatch equipArmorCooldownHelmet = new StopWatch();
    private final StopWatch equipArmorCooldownChestplate = new StopWatch();
    private final StopWatch equipArmorCooldownLeggings = new StopWatch();
    private final StopWatch equipArmorCooldownBoots = new StopWatch();

    @Subscribe
    private void onUpdate(final EventPlayerUpdate ignored) {
        if (mc.player == null) return;

        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;

        checkArmor(EquipmentSlot.HEAD);
        checkArmor(EquipmentSlot.CHEST);
        checkArmor(EquipmentSlot.LEGS);
        checkArmor(EquipmentSlot.FEET);
    }

    private void checkArmor(EquipmentSlot equipmentSlot) {
        ItemStack equipped = mc.player.getEquippedStack(equipmentSlot);
        if (equipped.isEmpty() || durabilityRatio(equipped) <= REPLACE_AT_DURABILITY) {
            swapArmor(equipmentSlot);
        }
    }

    private double durabilityRatio(ItemStack stack) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) return 1.0;
        return (stack.getMaxDamage() - stack.getDamage()) / (double) stack.getMaxDamage();
    }

    private void swapArmor(EquipmentSlot equipmentSlot) {
        if (equipmentSlot == EquipmentSlot.HEAD && !equipArmorCooldownHelmet.isReached(50)
                || equipmentSlot == EquipmentSlot.CHEST && !equipArmorCooldownChestplate.isReached(50)
                || equipmentSlot == EquipmentSlot.LEGS && !equipArmorCooldownLeggings.isReached(50)
                || equipmentSlot == EquipmentSlot.FEET && !equipArmorCooldownBoots.isReached(50)) return;

        var slot = InventoryUtil.getBestArmorSlot(equipmentSlot);

        if (slot == -1) return;

        if (slot < 9) slot += 36;

        int finalSlot = slot;
        int armorSlot = getArmorScreenSlot(equipmentSlot);

        InventoryUtil.clickWithGuiBypass(() -> {
            // Pick up the healthy replacement, swap it with the equipped piece,
            // then put the worn piece back into the replacement's inventory slot.
            mc.interactionManager.clickSlot(0, finalSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, armorSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(0, finalSlot, 0, SlotActionType.PICKUP, mc.player);
        });

        if (equipmentSlot == EquipmentSlot.HEAD) equipArmorCooldownHelmet.reset();
        if (equipmentSlot == EquipmentSlot.CHEST) equipArmorCooldownChestplate.reset();
        if (equipmentSlot == EquipmentSlot.LEGS) equipArmorCooldownLeggings.reset();
        if (equipmentSlot == EquipmentSlot.FEET) equipArmorCooldownBoots.reset();
    }

    private int getArmorScreenSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> throw new IllegalArgumentException("Unsupported armor slot: " + slot);
        };
    }
}
