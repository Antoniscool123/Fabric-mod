package com.aetherius.client.render;

import com.aetherius.entity.AetherMantaEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.math.MathHelper;

/**
 * Aether-Manta: a flat diamond body with two long ray wings and a whip tail. The wing roll is
 * driven by {@code animationProgress}, which produces the slow underwater-style undulation that
 * makes the manta read as "swimming" rather than flying.
 */
public class AetherMantaModel extends EntityModel<AetherMantaEntity> {

    public static final EntityModelLayer LAYER = new EntityModelLayer(Aeth.id("aether_manta"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public AetherMantaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-6.0f, -2.0f, -10.0f, 12.0f, 4.0f, 20.0f),
                ModelTransform.pivot(0.0f, 16.0f, 0.0f));
        body.addChild("left_wing",
                ModelPartBuilder.create().uv(0, 26).cuboid(0.0f, -1.0f, -8.0f, 26.0f, 2.0f, 16.0f),
                ModelTransform.pivot(6.0f, 0.0f, 0.0f));
        body.addChild("right_wing",
                ModelPartBuilder.create().uv(0, 26).mirrored().cuboid(-26.0f, -1.0f, -8.0f, 26.0f, 2.0f, 16.0f),
                ModelTransform.pivot(-6.0f, 0.0f, 0.0f));
        body.addChild("tail",
                ModelPartBuilder.create().uv(0, 46).cuboid(-1.0f, -1.0f, 0.0f, 2.0f, 2.0f, 22.0f),
                ModelTransform.pivot(0.0f, 0.0f, 10.0f));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(AetherMantaEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        float wave = MathHelper.sin(animationProgress * 0.18f);
        leftWing.roll = 0.28f + wave * 0.22f;
        rightWing.roll = -0.28f - wave * 0.22f;
        tail.yaw = MathHelper.sin(animationProgress * 0.12f) * 0.4f;
        body.pitch = headPitch * 0.017453292f * 0.3f;
        body.yaw = headYaw * 0.017453292f * 0.2f;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
