package tech.onetap.module.list.render;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.settings.BooleanSetting;
import tech.onetap.module.settings.ModeSetting;
import tech.onetap.util.base.Instance;
import tech.onetap.util.friend.FriendRepository;
import tech.onetap.util.render.model.CustomPlayerModel;

@ModuleInformation(moduleName = "CustomModels", moduleDesc = "Кастомные модели игроков", moduleCategory = ModuleCategory.RENDER)
public final class CustomModels extends Module {

    private final ModeSetting models = new ModeSetting("Модель", "Crazy Rabbit",
            "Crazy Rabbit", "Freddy Bear", "White Demon", "Red Demon", "Amogus");
    private final BooleanSetting friends = new BooleanSetting("Применять на друзей", true);

    public static CustomPlayerModel modelFor(PlayerEntityRenderState state) {
        CustomModels module = Instance.get(CustomModels.class);
        if (module == null || !module.isEnabled() || !module.shouldApplyTo(state)) return null;
        return CustomPlayerModel.of(module.models.getValue());
    }

    public static boolean appliesTo(PlayerEntityRenderState state) {
        CustomModels module = Instance.get(CustomModels.class);
        return module != null && module.isEnabled() && module.shouldApplyTo(state);
    }

    private boolean shouldApplyTo(PlayerEntityRenderState state) {
        if (state == null || mc.player == null) return false;
        if (state.id == mc.player.getId()) return true;
        return friends.getValue() && state.name != null && FriendRepository.isFriend(state.name);
    }
}
