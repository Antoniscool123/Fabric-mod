package com.aetherius.compat;

import com.aetherius.Aetherius;
import com.aetherius.util.Aeth;
import net.fabricmc.api.ModInitializer;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

/**
 * Entry point for TerraBlender (<code>"terrablender"</code> in {@code fabric.mod.json}).
 *
 * <p>TerraBlender calls {@link #onTerraBlenderInitialized()} once, after it has loaded, and only
 * if the mod is installed - the class is never touched otherwise, which is what keeps the
 * dependency soft. Registering here rather than in the main initialiser also guarantees the
 * overworld's parameter list has not been built yet.</p>
 */
public class AetheriusTerraBlender implements ModInitializer, TerraBlenderApi {

    @Override
    public void onInitialize() {
        // Nothing: the work happens in onTerraBlenderInitialized.
    }

    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new AetheriusOverworldRegion(Aeth.id("overworld_meadows"),
                AetheriusOverworldRegion.Window.MEADOWS,
                AetheriusOverworldRegion.key("starlit_meadows"), 2));
        Regions.register(new AetheriusOverworldRegion(Aeth.id("overworld_barrens"),
                AetheriusOverworldRegion.Window.BARRENS,
                AetheriusOverworldRegion.key("blighted_barrens"), 1));
        Aetherius.LOGGER.info("[Aetherius] TerraBlender present - Starlit Meadows and Blighted "
                + "Barrens are now overworld biomes.");
    }
}
