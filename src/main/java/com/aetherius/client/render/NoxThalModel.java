package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.math.MathHelper;

/**
 * Nox'Thal's eclipse-dragon silhouette: long neck, angular head, two crescent wings, a segmented
 * tail. Animated procedurally (no keyframed animations) so a phase change can retune the motion
 * at runtime - descending wing beats in Phase 1, heavy drooping wings in Phase 2, spread-static
 * wings in Phase 3.
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

    public NoxThalModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-7.0f, -6.0f, -18.0f, 14.0f, 12.0f, 36.0f),
                ModelTransform.pivot(0.0f, 6.0f, 0.0f));

        ModelPartData neck = body.addChild("neck",
                ModelPartBuilder.create().uv(0, 48).cuboid(-3.0f, -24.0f, -8.0f, 6.0f, 26.0f, 6.0f),
                ModelTransform.pivot(0.0f, 0.0f, -18.0f));

        neck.addChild("head",
                ModelPartBuilder.create()
                        .uv(64, 0).cuboid(-4.0f, -24.0f, -20.0f, 8.0f, 8.0f, 14.0f)
                        .uv(64, 22).cuboid(-2.0f, -22.0f, -26.0f, 4.0f, 4.0f, 6.0f)
                        .uv(64, 32).cuboid(-5.0f, -26.0f, -14.0f, 2.0f, 8.0f, 2.0f)
                        .uv(64, 32).mirrored().cuboid(3.0f, -26.0f, -14.0f, 2.0f, 8.0f, 2.0f),
                ModelTransform.pivot(0.0f, -22.0f, -6.0f));

        body.addChild("left_wing",
                ModelPartBuilder.create()
                        .uv(0, 64).cuboid(0.0f, -1.0f, -8.0f, 34.0f, 2.0f, 22.0f)
                        .uv(0, 88).cuboid(0.0f, 0.0f, 6.0f, 26.0f, 2.0f, 14.0f),
                ModelTransform.pivot(6.0f, -4.0f, 0.0f));

        body.addChild("right_wing",
                ModelPartBuilder.create()
                        .uv(0, 64).mirrored().cuboid(-34.0f, -1.0f, -8.0f, 34.0f, 2.0f, 22.0f)
                        .uv(0, 88).mirrored().cuboid(-26.0f, 0.0f, 6.0f, 26.0f, 2.0f, 14.0f),
                ModelTransform.pivot(-6.0f, -4.0f, 0.0f));

        body.addChild("tail",
                ModelPartBuilder.create()
                        .uv(0, 104).cuboid(-2.0f, -2.0f, 0.0f, 4.0f, 4.0f, 30.0f)
                        .uv(0, 138).cuboid(-6.0f, -1.0f, 28.0f, 12.0f, 2.0f, 10.0f),
                ModelTransform.pivot(0.0f, 0.0f, 18.0f));

        return TexturedModelData.of(data, 128, 160);
    }

    @Override
    public void setAngles(NoxThalEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        int phase = entity.phaseIndex();
        head.yaw = headYaw * 0.017453292f;
        head.pitch = headPitch * 0.017453292f - 0.15f;
        neck.pitch = 0.25f + MathHelper.sin(animationProgress * 0.05f) * 0.05f;

        float beat = MathHelper.sin(animationProgress * (phase == 0 ? 0.35f : phase == 1 ? 0.18f : 0.1f));
        float flap = beat * (phase == 0 ? 0.55f : 0.3f);
        leftWing.roll = flap;
        rightWing.roll = -flap;
        leftWing.yaw = 0.15f;
        rightWing.yaw = -0.15f;

        float sway = MathHelper.sin(animationProgress * 0.08f);
        tail.yaw = sway * 0.35f;
        tail.pitch = 0.1f + sway * 0.08f;

        body.pitch = phase == 2 ? 0.2f : 0.05f;
        root.y = (float) Math.sin(animationProgress * 0.06f) * 0.4f;
    }

    @Override
    public ModelPart getPart() {
        return root;
    }
}
