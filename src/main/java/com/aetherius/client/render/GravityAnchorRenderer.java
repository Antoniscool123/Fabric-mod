package com.aetherius.client.render;

import com.aetherius.entity.GravityAnchorEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Gravity Anchors are rendered as a slowly rotating crying-obsidian core. Rendering a block
 * through {@code BlockRenderManager} avoids needing a bespoke model/texture for a 1-block
 * objective entity.
 */
public class GravityAnchorRenderer extends EntityRenderer<GravityAnchorEntity> {

    public GravityAnchorRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.9f;
    }

    @Override
    public void render(GravityAnchorEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0, 0.35, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((entity.age + tickDelta) * 2.0f));
        matrices.scale(1.15f, 1.15f, 1.15f);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(
                Blocks.CRYING_OBSIDIAN.getDefaultState(), matrices, vertexConsumers, light,
                OverlayTexture.DEFAULT_UV);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(GravityAnchorEntity entity) {
        return new Identifier("minecraft", "textures/block/crying_obsidian.png");
    }
}
