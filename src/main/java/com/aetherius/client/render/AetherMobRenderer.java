package com.aetherius.client.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;

/**
 * Shared renderer for the humanoid Aetherius mobs (Sentinel, Specter, Abomination, Wardens).
 *
 * <p>All four reuse vanilla biped model layers ({@code PLAYER} / {@code ZOMBIE}) with custom
 * 64x64 textures. That keeps the asset budget tiny and guarantees correct hitbox/proportion
 * alignment without shipping four bespoke model classes.</p>
 */
public class AetherMobRenderer<T extends MobEntity> extends MobEntityRenderer<T, BipedEntityModel<T>> {

    private final Identifier texture;

    public AetherMobRenderer(EntityRendererFactory.Context context, EntityModelLayer layer,
                             Identifier texture, float shadowRadius) {
        super(context, new BipedEntityModel<>(context.getPart(layer)), shadowRadius);
        this.texture = texture;
    }

    @Override
    public Identifier getTexture(T entity) {
        return texture;
    }
}
