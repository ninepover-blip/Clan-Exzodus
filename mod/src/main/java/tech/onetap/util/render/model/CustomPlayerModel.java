package tech.onetap.util.render.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class CustomPlayerModel {

    private static final Map<String, CustomPlayerModel> CACHE = new HashMap<>();

    private final ModelPart root;
    private final Identifier texture;

    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    private final float leftArmRoll;
    private final float rightArmRoll;

    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;

    private CustomPlayerModel(ModelPart root, String texture,
                              ModelPart head, ModelPart leftArm, ModelPart rightArm,
                              ModelPart leftLeg, ModelPart rightLeg,
                              float leftArmRoll, float rightArmRoll,
                              float scaleX, float scaleY, float scaleZ,
                              float offsetX, float offsetY, float offsetZ) {
        this.root = root;
        this.texture = Identifier.of("mre", "textures/models/" + texture);
        this.head = head;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
        this.leftArmRoll = leftArmRoll;
        this.rightArmRoll = rightArmRoll;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    public static CustomPlayerModel of(String style) {
        return CACHE.computeIfAbsent(style, CustomPlayerModel::create);
    }

    private static CustomPlayerModel create(String style) {
        return switch (style) {
            case "Freddy Bear" -> freddy();
            case "White Demon" -> demon("whitedemon.png");
            case "Red Demon" -> demon("reddemon.png");
            case "Amogus" -> amogus();
            default -> rabbit();
        };
    }

    private static CustomPlayerModel rabbit() {
        ModelPart root = CustomModelGeometry.rabbit();
        ModelPart bone = root.getChild("bone");
        return new CustomPlayerModel(root, "rabbit.png",
                bone.getChild("head"), bone.getChild("larm"), bone.getChild("rarm"),
                bone.getChild("lleg"), bone.getChild("rleg"),
                -0.0873F, 0.0873F,
                1.25F, 1.25F, 1.25F,
                0.0F, -0.3F, 0.0F);
    }

    private static CustomPlayerModel demon(String texture) {
        ModelPart root = CustomModelGeometry.demon();
        return new CustomPlayerModel(root, texture,
                root.getChild("head"), root.getChild("left_arm"), root.getChild("right_arm"),
                root.getChild("left_leg"), root.getChild("right_leg"),
                0.0F, 0.0F,
                1.0F, 1.0F, 1.0F,
                0.0F, 0.0F, 0.0F);
    }

    private static CustomPlayerModel freddy() {
        ModelPart root = CustomModelGeometry.freddy();
        ModelPart body = root.getChild("body");
        return new CustomPlayerModel(root, "freddy.png",
                body.getChild("head"), body.getChild("arm_left"), body.getChild("arm_right"),
                body.getChild("leg_left"), body.getChild("leg_right"),
                0.0F, 0.0F,
                0.75F, 0.65F, 0.75F,
                0.0F, 0.85F, 0.0F);
    }

    private static CustomPlayerModel amogus() {
        return new CustomPlayerModel(CustomModelGeometry.amogus(), "amogus.png",
                null, null, null, null, null,
                0.0F, 0.0F,
                1.0F, 1.0F, 1.0F,
                0.0F, -0.5F, 0.0F);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay, int color, PlayerEntityModel source) {
        copyAngles(source);

        matrices.push();
        matrices.scale(scaleX, scaleY, scaleZ);
        matrices.translate(offsetX, offsetY, offsetZ);
        root.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(texture)), light, overlay, color);
        matrices.pop();
    }

    private void copyAngles(PlayerEntityModel source) {
        if (head != null) {
            head.pitch = source.head.pitch;
            head.yaw = source.head.yaw;
            head.roll = source.head.roll;
        }
        if (leftArm != null) {
            leftArm.pitch = source.leftArm.pitch;
            leftArm.yaw = source.leftArm.yaw;
            leftArm.roll = source.leftArm.roll + leftArmRoll;
        }
        if (rightArm != null) {
            rightArm.pitch = source.rightArm.pitch;
            rightArm.yaw = source.rightArm.yaw;
            rightArm.roll = source.rightArm.roll + rightArmRoll;
        }
        if (leftLeg != null) {
            leftLeg.pitch = source.leftLeg.pitch;
            leftLeg.yaw = source.leftLeg.yaw;
            leftLeg.roll = source.leftLeg.roll;
        }
        if (rightLeg != null) {
            rightLeg.pitch = source.rightLeg.pitch;
            rightLeg.yaw = source.rightLeg.yaw;
            rightLeg.roll = source.rightLeg.roll;
        }
    }
}
