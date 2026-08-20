package tech.onetap.module.list.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.util.friend.FriendRepository;

@ModuleInformation(moduleName = "FriendClick", moduleDesc = "Добавляет игрока под прицелом в друзья", moduleCategory = ModuleCategory.MISC)
public class FriendClick extends Module {
    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity player) {
            FriendRepository.addFriend(player.getNameForScoreboard());
        }
        mc.execute(() -> setEnabled(false));
    }
}
