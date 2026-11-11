package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders Nox'Thal. The model is authored at true size (feet on the standard y = 24 ground plane,
 * ~2.8 blocks tall, ~5.5 block wingspan), so the renderer applies no scale: what you see matches
 * the 3.0 x 3.2 hitbox, the nameplate and the shadow radius.
 */
public class NoxThalRenderer extends MobEntityRenderer<NoxThalEntity, NoxThalModel> {

    private static final Identifier TEXTURE = Aeth.id("textures/entity/nox_thal.png");

    public NoxThalRenderer(EntityRendererFactory.Context context) {
        super(context, new NoxThalModel(context.getPart(NoxThalModel.LAYER)), 2.2f);
    }

    @Override
    public Identifier getTexture(NoxThalEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(NoxThalEntity entity, MatrixStack matrices, float amount) {
        // Phase 2 sinks the dragon towards the ground as its anchors are destroyed.
        if (entity.phaseIndex() == 1 && entity.isGrounded()) {
            matrices.scale(1.05f, 0.82f, 1.05f);
        }
    }

    @Override
    public void render(NoxThalEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        if (entity.phaseIndex() == 2) {
            // Slow menacing roll so the silhouette reads in near-total darkness.
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(
                    (entity.age + tickDelta) * 0.05f) * 3.0f));
        }
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
