package tech.onetap.module.list.render.custommodel;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** OptiFine-модель предмета: части, прикреплённые к голове или телу игрока. */
public class PlayerItemModel {

    private final String type;
    private final String texturePath;
    private final List<ModelPart> headItems = new ArrayList<>();
    private final List<ModelPart> bodyItems = new ArrayList<>();

    public PlayerItemModel(String type, String texturePath) {
        this.type = type;
        this.texturePath = texturePath;
    }

    public String getType() {
        return type;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void addHeadItem(ModelPart part) {
        headItems.add(part);
    }

    public void addBodyItem(ModelPart part) {
        bodyItems.add(part);
    }

    public boolean isEmpty() {
        return headItems.isEmpty() && bodyItems.isEmpty();
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                       PlayerEntityRenderState state, PlayerEntityModel contextModel) {
        Identifier textureId = PlayerConfigurationManager.getTexture(type);
        if (textureId == null || isEmpty()) return;

        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(textureId));

        for (ModelPart part : headItems) {
            matrices.push();
            contextModel.head.rotate(matrices);
            part.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        for (ModelPart part : bodyItems) {
            matrices.push();
            contextModel.body.rotate(matrices);
            part.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}
