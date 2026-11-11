package com.aetherius.client.render;

import com.aetherius.entity.AetherMantaEntity;
import com.aetherius.util.Aeth;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renders the Aether-Manta with its dedicated model and a soft 1.2x scale. */
public class AetherMantaRenderer extends MobEntityRenderer<AetherMantaEntity, AetherMantaModel> {

    private static final Identifier TEXTURE = Aeth.id("textures/entity/aether_manta.png");

    public AetherMantaRenderer(EntityRendererFactory.Context context) {
        super(context, new AetherMantaModel(context.getPart(AetherMantaModel.LAYER)), 0.8f);
    }

    @Override
    public Identifier getTexture(AetherMantaEntity entity) {
        return TEXTURE;
    }
}
