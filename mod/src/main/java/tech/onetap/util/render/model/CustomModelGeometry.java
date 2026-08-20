package tech.onetap.util.render.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

import java.util.function.Consumer;

public final class CustomModelGeometry {

    private CustomModelGeometry() {}

    public static ModelPart rabbit() {
        return build(CustomModelGeometry::rabbit, 64, 64);
    }

    public static ModelPart demon() {
        return build(CustomModelGeometry::demon, 64, 64);
    }

    public static ModelPart freddy() {
        return build(CustomModelGeometry::freddy, 100, 80);
    }

    public static ModelPart amogus() {
        return build(CustomModelGeometry::amogus, 64, 64);
    }

    private static ModelPart build(Consumer<ModelPartData> geometry, int textureWidth, int textureHeight) {
        ModelData data = new ModelData();
        geometry.accept(data.getRoot());
        return TexturedModelData.of(data, textureWidth, textureHeight).createModel();
    }

    private static void rabbit(ModelPartData root) {
        ModelPartData bone = root.addChild("bone",
                ModelPartBuilder.create().uv(28, 45).cuboid(-5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));

        bone.addChild("rleg",
                ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                ModelTransform.pivot(-3.0F, -2.0F, -1.0F));

        bone.addChild("larm",
                ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
                ModelTransform.of(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F));

        bone.addChild("rarm",
                ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
                ModelTransform.of(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F));

        bone.addChild("lleg",
                ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                ModelTransform.pivot(3.0F, -2.0F, -1.0F));

        bone.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F)
                        .uv(56, 0).cuboid(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
                        .uv(56, 0).mirrored().cuboid(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F).mirrored(false)
                        .uv(0, 45).cuboid(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F)
                        .uv(46, 0).cuboid(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F)
                        .uv(46, 0).cuboid(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F),
                ModelTransform.pivot(0.0F, -14.0F, -1.0F));
    }

    private static void demon(ModelPartData root) {
        ModelPartData head = root.addChild("head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.3F)),
                ModelTransform.pivot(0.0F, -6.0F, -1.0F));

        head.addChild("left_horn",
                ModelPartBuilder.create()
                        .uv(32, 8).cuboid(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
                        .uv(0, 0).cuboid(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
                ModelTransform.of(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F));

        head.addChild("right_horn",
                ModelPartBuilder.create().mirrored()
                        .uv(32, 8).cuboid(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
                        .uv(0, 0).cuboid(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
                ModelTransform.of(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F));

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F),
                ModelTransform.of(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F));

        body.addChild("left_wing",
                ModelPartBuilder.create().uv(40, 12).cuboid(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
                ModelTransform.of(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F));

        body.addChild("right_wing",
                ModelPartBuilder.create().mirrored().uv(40, 12).cuboid(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
                ModelTransform.of(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F));

        root.addChild("left_arm",
                ModelPartBuilder.create().uv(24, 16).cuboid(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
                ModelTransform.of(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F));

        root.addChild("right_arm",
                ModelPartBuilder.create().mirrored().uv(24, 16).cuboid(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
                ModelTransform.of(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F));

        ModelPartData leftLeg = root.addChild("left_leg",
                ModelPartBuilder.create().uv(48, 22).cuboid(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F),
                ModelTransform.pivot(3.0F, 10.0F, 0.0F));

        ModelPartData leftLegLower = leftLeg.addChild("left_leg_lower",
                ModelPartBuilder.create().uv(34, 34).cuboid(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
                ModelTransform.of(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        leftLegLower.addChild("left_foot",
                ModelPartBuilder.create()
                        .uv(26, 0).cuboid(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
                        .uv(40, 0).cuboid(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
                ModelTransform.of(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        leftLegLower.addChild("left_shin", ModelPartBuilder.create(),
                        ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F))
                .addChild("left_shin_part",
                        ModelPartBuilder.create()
                                .uv(16, 34).cuboid(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
                                .uv(0, 32).cuboid(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
                        ModelTransform.pivot(1.9F, 12.0F, 0.25F));

        ModelPartData rightLeg = root.addChild("right_leg",
                ModelPartBuilder.create().mirrored().uv(48, 22).cuboid(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F),
                ModelTransform.pivot(-3.0F, 10.0F, 0.0F));

        ModelPartData rightLegLower = rightLeg.addChild("right_leg_lower",
                ModelPartBuilder.create().mirrored().uv(34, 34).cuboid(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
                ModelTransform.of(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));

        rightLegLower.addChild("right_foot",
                ModelPartBuilder.create().mirrored()
                        .uv(26, 0).cuboid(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
                        .uv(40, 0).cuboid(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
                ModelTransform.of(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));

        rightLegLower.addChild("right_shin", ModelPartBuilder.create(),
                        ModelTransform.of(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F))
                .addChild("right_shin_part",
                        ModelPartBuilder.create().mirrored()
                                .uv(16, 34).cuboid(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
                                .uv(0, 32).cuboid(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
                        ModelTransform.pivot(-1.9F, 12.0F, 0.25F));
    }

    private static void freddy(ModelPartData root) {
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F),
                ModelTransform.pivot(0.0F, -9.0F, 0.0F));

        body.addChild("torso",
                ModelPartBuilder.create().uv(8, 0).cuboid(-6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F),
                ModelTransform.of(0.0F, 0.0F, 0.0F, (float) Math.PI / 180.0F, 0.0F, 0.0F));

        body.addChild("crotch",
                ModelPartBuilder.create().uv(56, 0).cuboid(-5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F),
                ModelTransform.pivot(0.0F, 9.5F, 0.0F));

        ModelPartData armRight = body.addChild("arm_right",
                ModelPartBuilder.create().uv(48, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                ModelTransform.of(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.2617994F));

        armRight.addChild("arm_right_pad",
                ModelPartBuilder.create().uv(70, 10).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData armRight2 = armRight.addChild("arm_right_2",
                ModelPartBuilder.create().uv(90, 20).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));

        armRight2.addChild("arm_right_pad_2",
                ModelPartBuilder.create().uv(0, 26).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        armRight2.addChild("hand_right",
                ModelPartBuilder.create().uv(20, 26).cuboid(-2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
                ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F));

        ModelPartData armLeft = body.addChild("arm_left",
                ModelPartBuilder.create().uv(62, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                ModelTransform.of(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2617994F));

        armLeft.addChild("arm_left_pad",
                ModelPartBuilder.create().uv(38, 54).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData armLeft2 = armLeft.addChild("arm_left_2",
                ModelPartBuilder.create().uv(90, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F));

        armLeft2.addChild("arm_left_pad_2",
                ModelPartBuilder.create().uv(0, 58).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        armLeft2.addChild("hand_left",
                ModelPartBuilder.create().uv(58, 56).cuboid(-1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F),
                ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F));

        ModelPartData legRight = body.addChild("leg_right",
                ModelPartBuilder.create().uv(90, 8).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                ModelTransform.pivot(-3.3F, 12.5F, 0.0F));

        legRight.addChild("leg_right_pad",
                ModelPartBuilder.create().uv(73, 33).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData legRight2 = legRight.addChild("leg_right_2",
                ModelPartBuilder.create().uv(20, 35).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                ModelTransform.of(0.0F, 9.6F, 0.0F, (float) Math.PI / 90.0F, 0.0F, 0.0F));

        legRight2.addChild("leg_right_pad_2",
                ModelPartBuilder.create().uv(0, 39).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        legRight2.addChild("foot_right",
                ModelPartBuilder.create().uv(22, 39).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
                ModelTransform.of(0.0F, 8.0F, 0.0F, (float) -Math.PI / 90.0F, 0.0F, 0.0F));

        ModelPartData legLeft = body.addChild("leg_left",
                ModelPartBuilder.create().uv(54, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                ModelTransform.pivot(3.3F, 12.5F, 0.0F));

        legLeft.addChild("leg_left_pad",
                ModelPartBuilder.create().uv(48, 39).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        ModelPartData legLeft2 = legLeft.addChild("leg_left_2",
                ModelPartBuilder.create().uv(72, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                ModelTransform.of(0.0F, 9.6F, 0.0F, (float) Math.PI / 90.0F, 0.0F, 0.0F));

        legLeft2.addChild("leg_left_pad_2",
                ModelPartBuilder.create().uv(16, 50).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F),
                ModelTransform.pivot(0.0F, 0.5F, 0.0F));

        legLeft2.addChild("foot_left",
                ModelPartBuilder.create().uv(72, 50).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F),
                ModelTransform.of(0.0F, 8.0F, 0.0F, (float) -Math.PI / 90.0F, 0.0F, 0.0F));

        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create().uv(39, 22).cuboid(-5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F),
                ModelTransform.pivot(0.0F, -13.0F, -0.5F));

        head.addChild("jaw",
                ModelPartBuilder.create().uv(49, 65).cuboid(-5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F),
                ModelTransform.of(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));

        head.addChild("nose",
                ModelPartBuilder.create().uv(17, 67).cuboid(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F),
                ModelTransform.pivot(0.0F, -2.0F, -4.5F));

        head.addChild("ear_right",
                        ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                        ModelTransform.of(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, -1.0471976F))
                .addChild("ear_right_pad",
                        ModelPartBuilder.create().uv(85, 0).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F),
                        ModelTransform.pivot(0.0F, -1.0F, 0.0F));

        head.addChild("ear_left",
                        ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F),
                        ModelTransform.of(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, 1.0471976F))
                .addChild("ear_left_pad",
                        ModelPartBuilder.create().uv(40, 39).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F),
                        ModelTransform.pivot(0.0F, -1.0F, 0.0F));

        head.addChild("hat",
                        ModelPartBuilder.create().uv(70, 24).cuboid(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F),
                        ModelTransform.of(0.0F, -8.4F, 0.0F, (float) -Math.PI / 180.0F, 0.0F, 0.0F))
                .addChild("hat_2",
                        ModelPartBuilder.create().uv(78, 61).cuboid(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                        ModelTransform.of(0.0F, 0.1F, 0.0F, (float) -Math.PI / 180.0F, 0.0F, 0.0F));
    }

    private static void amogus(ModelPartData root) {
        root.addChild("body",
                ModelPartBuilder.create()
                        .uv(34, 8).cuboid(-4.0F, 6.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                        .uv(15, 10).cuboid(-3.0F, 9.0F, 3.0F, 6.0F, 8.0F, 3.0F)
                        .uv(26, 0).cuboid(-3.0F, 5.0F, -3.0F, 6.0F, 1.0F, 6.0F),
                ModelTransform.NONE);

        root.addChild("eye",
                ModelPartBuilder.create().uv(0, 10).cuboid(-3.0F, 7.0F, -4.0F, 6.0F, 4.0F, 1.0F),
                ModelTransform.NONE);

        root.addChild("left_leg",
                ModelPartBuilder.create().uv(0, 0).cuboid(2.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                ModelTransform.pivot(-2.0F, 18.0F, 0.0F));

        root.addChild("right_leg",
                ModelPartBuilder.create().uv(13, 0).cuboid(-5.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                ModelTransform.pivot(2.0F, 18.0F, 0.0F));
    }
}
