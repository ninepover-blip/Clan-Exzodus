package tech.onetap.module.list.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import tech.onetap.module.Module;
import tech.onetap.module.ModuleCategory;
import tech.onetap.module.ModuleInformation;
import tech.onetap.module.list.render.custommodel.PlayerConfiguration;
import tech.onetap.module.list.render.custommodel.PlayerConfigurationManager;
import tech.onetap.module.list.render.custommodel.PlayerItemModel;

@ModuleInformation(moduleName = "CustomModel", moduleDesc = "OptiFine-модели предметов на игроках", moduleCategory = ModuleCategory.RENDER)
public class CustomModel extends Module {

    public static void renderCustomItems(PlayerEntityRenderState state, MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers, int light,
                                         PlayerEntityModel playerModel) {
        String name = state.name;
        if (name == null || name.isBlank()) return;

        PlayerConfiguration config = PlayerConfigurationManager.getConfig(name);
        if (config == null || config.isEmpty()) return;

        for (String type : config.getActiveTypes()) {
            PlayerItemModel model = PlayerConfigurationManager.getModel(type);
            if (model != null) {
                model.render(matrices, vertexConsumers, light, state, playerModel);
            }
        }
    }
}
