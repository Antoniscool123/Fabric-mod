package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Nox'Thal, the eclipse dragon.
 *
 * <p>Every part is anchored in <b>absolute model space</b>: a part's cuboid offsets are relative
 * to that part's own pivot, and the pivot sits on the actual joint (shoulder, neck base, neck
 * top, tail root). The previous revision mixed body-space and part-space offsets, which is why the
 * head floated ~46 pixels above the neck and the body looked like a bag of loose boxes.</p>
 *
 * <p>Layout (y grows downwards, -z is forward):
 * body y 1..15, z -18..18 · neck y -12..1, z -20..-12 · head y -26..-14, z -38..-18 ·
 * wings x ±7..±39, z -12..10 · tail z 18..52.</p>
 */
public class NoxThalModel extends EntityModel<NoxThalEntity> {

    public static final EntityModelLayer LAYER = new EntityModelLayer(Aeth.id("nox_thal"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public NoxThalModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = root.getChild("neck");
        this.head = root.getChild("head");
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
        this.tail = root.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        // ---- torso: 2 blocks long, the visual anchor of the whole silhouette
        root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-8.0f, -7.0f, -18.0f, 16.0f, 14.0f, 36.0f),
                ModelTransform.pivot(0.0f, 8.0f, 0.0f));

        // ---- neck: straight column standing on the front-top of the torso (pivot = its base)
        root.addChild("neck",
                ModelPartBuilder.create().uv(0, 52).cuboid(-4.0f, -13.0f, -4.0f, 8.0f, 13.0f, 8.0f),
                ModelTransform.pivot(0.0f, 1.0f, -16.0f));

        // ---- head: skull + snout + two swept horns, all in one part so rotation cannot split them
        //      pivot = neck top (0, -12, -16) in model space
        root.addChild("head",
                ModelPartBuilder.create()
                        .uv(0, 76).cuboid(-6.0f, -14.0f, -16.0f, 12.0f, 12.0f, 14.0f)
                        .uv(56, 76).cuboid(-3.0f, -11.0f, -22.0f, 6.0f, 6.0f, 8.0f)
                        .uv(56, 92).cuboid(-7.0f, -19.0f, -18.0f, 2.0f, 5.0f, 2.0f)
                        .uv(56, 92).mirrored().cuboid(5.0f, -19.0f, -18.0f, 2.0f, 5.0f, 2.0f),
                ModelTransform.pivot(0.0f, -12.0f, -16.0f));

        // ---- wings: primary membrane + inner vane, pivot at the shoulder
        root.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 106).cuboid(0.0f, -1.0f, -6.0f, 32.0f, 2.0f, 22.0f)
                        .uv(112, 106).cuboid(4.0f, 1.0f, 8.0f, 20.0f, 2.0f, 12.0f),
                ModelTransform.pivot(7.0f, 4.0f, -6.0f));

        root.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(0, 106).mirrored().cuboid(-32.0f, -1.0f, -6.0f, 32.0f, 2.0f, 22.0f)
                        .uv(112, 106).mirrored().cuboid(-24.0f, 1.0f, 8.0f, 20.0f, 2.0f, 12.0f),
                ModelTransform.pivot(-7.0f, 4.0f, -6.0f));

        // ---- tail: long whip plus the eclipse fin at the tip, pivot at the tail root
        root.addChild("tail",
                ModelPartBuilder.create()
                        .uv(0, 132).cuboid(-3.0f, -3.0f, 0.0f, 6.0f, 6.0f, 26.0f)
                        .uv(66, 132).cuboid(-8.0f, -1.0f, 24.0f, 16.0f, 2.0f, 10.0f),
                ModelTransform.pivot(0.0f, 6.0f, 18.0f));

        // ---- tucked legs, one pair
        root.addChild("left_leg",
                ModelPartBuilder.create().uv(120, 150).cuboid(-2.0f, 0.0f, -3.0f, 4.0f, 10.0f, 6.0f),
                ModelTransform.pivot(6.0f, 14.0f, 8.0f));
        root.addChild("right_leg",
                ModelPartBuilder.create().uv(120, 150).mirrored().cuboid(-2.0f, 0.0f, -3.0f, 4.0f, 10.0f, 6.0f),
                ModelTransform.pivot(-6.0f, 14.0f, 8.0f));

        return TexturedModelData.of(data, 256, 256);
    }

    @Override
    public void setAngles(NoxThalEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        int phase = entity.phaseIndex();

        float bob = MathHelper.sin(animationProgress * 0.06f);
        body.pitch = phase == 2 ? 0.16f : 0.03f;
        body.roll = bob * 0.02f;

        neck.pitch = 0.12f + MathHelper.sin(animationProgress * 0.05f) * 0.04f;
        head.yaw = headYaw * 0.017453292f;
        head.pitch = headPitch * 0.017453292f - 0.1f;

        float beat = MathHelper.sin(animationProgress * (phase == 0 ? 0.35f : phase == 1 ? 0.18f : 0.1f));
        float flap = beat * (phase == 0 ? 0.55f : phase == 2 ? 0.42f : 0.3f);
        leftWing.roll = -flap;
        rightWing.roll = flap;
        leftWing.yaw = 0.18f;
        rightWing.yaw = -0.18f;

        float sway = MathHelper.sin(animationProgress * 0.08f);
        tail.yaw = sway * 0.3f;
        tail.pitch = 0.08f + sway * 0.06f;

        leftLeg.pitch = 0.35f;
        rightLeg.pitch = 0.35f;

        // Gentle idle hover: the model sits ~1 block up so the wings clear the ground.
        root.pivotY = bob * 0.4f;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
