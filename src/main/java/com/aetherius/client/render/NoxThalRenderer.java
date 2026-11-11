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
 * Renders Nox'Thal at 1.6x scale with a phase-reactive alpha tint: Phase 3 (Supernova) renders
 * it almost as a silhouette so the darkness overlay reads as total.
 */
public class NoxThalRenderer extends MobEntityRenderer<NoxThalEntity, NoxThalModel> {

    private static final Identifier TEXTURE = Aeth.id("textures/entity/nox_thal.png");

    public NoxThalRenderer(EntityRendererFactory.Context context) {
        super(context, new NoxThalModel(context.getPart(NoxThalModel.LAYER)), 3.0f);
    }

    @Override
    public Identifier getTexture(NoxThalEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(NoxThalEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(1.6f, 1.6f, 1.6f);
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
