package tech.onetap.util.render.model;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

public final class CustomModelContext {

    public static PlayerEntityRenderState currentState;
    public static VertexConsumerProvider currentProvider;

    private CustomModelContext() {}
}
