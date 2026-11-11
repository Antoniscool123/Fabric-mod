package com.aetherius.client.render;

import com.aetherius.entity.NoxThalEntity;
import com.aetherius.util.Aeth;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

/**
 * GeckoLib model for Nox'Thal.
 *
 * <p>{@link DefaultedEntityGeoModel} derives every asset path from one id, so the files are:</p>
 * <ul>
 *   <li>{@code assets/aetherius/geo/entity/nox_thal.geo.json}</li>
 *   <li>{@code assets/aetherius/animations/entity/nox_thal.animation.json}</li>
 *   <li>{@code assets/aetherius/textures/entity/nox_thal.png}</li>
 * </ul>
 *
 * <p>Passing {@code "head"} as the head bone turns head tracking on: GeckoLib rotates that bone
 * towards the target/player for us, which the hand-written model had to do by hand.</p>
 */
public class NoxThalGeoModel extends DefaultedEntityGeoModel<NoxThalEntity> {

    public NoxThalGeoModel() {
        super(Aeth.id("nox_thal"), "head");
    }

    @Override
    public boolean crashIfBoneMissing() {
        // An animation referencing a renamed bone should degrade, not crash a live world.
        return false;
    }
}
