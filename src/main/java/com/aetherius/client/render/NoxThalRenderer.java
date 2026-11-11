package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * Renders Nox'Thal through GeckoLib: geometry and animation live in
 * {@code geo/entity/nox_thal.geo.json} and {@code animations/entity/nox_thal.animation.json}, so the
 * model can be edited in Blockbench without touching Java.
 *
 * <p>Phase 3 (Supernova) adds a slow menacing roll so the silhouette still reads while the client is
 * wrapped in the boss's darkness overlay.</p>
 */
public class NoxThalRenderer extends GeoEntityRenderer<NoxThalEntity> {

    public NoxThalRenderer(EntityRendererFactory.Context context) {
        super(context, new NoxThalGeoModel());
        this.shadowRadius = 2.2f;
    }

    @Override
    public void render(NoxThalEntity entity, float entityYaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        if (entity.phaseIndex() == 2) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(
                    (entity.age + tickDelta) * 0.05f) * 3.0f));
        }
        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
        matrices.pop();
    }
}
