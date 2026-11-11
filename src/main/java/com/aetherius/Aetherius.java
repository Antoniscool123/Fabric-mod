package com.aetherius;

import com.aetherius.api.chronicle.ChronicleManager;
import com.aetherius.api.chronicle.ChronicleRegistry;
import com.aetherius.api.magic.MagicComponents;
import com.aetherius.api.spell.SpellRegistry;
import com.aetherius.command.AetheriusCommand;
import com.aetherius.dimension.ShatteredSkyChunkGenerator;
import com.aetherius.dimension.TemporalStormManager;
import com.aetherius.network.AetherNetworking;
import com.aetherius.registry.ModBlockEntities;
import com.aetherius.registry.ModBlocks;
import com.aetherius.registry.ModEntities;
import com.aetherius.registry.ModItemGroups;
import com.aetherius.registry.ModItems;
import com.aetherius.registry.ModParticles;
import com.aetherius.registry.ModSounds;
import com.aetherius.registry.ModStatusEffects;
import com.aetherius.world.StarfallManager;
import com.aetherius.world.blight.CelestialBlightManager;
import com.aetherius.worldgen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>Aetherius: Echoes of the Shattered Sky</b> - mod entry point.
 *
 * <h2>Initialisation order (matters)</h2>
 * <ol>
 *   <li>Sounds/particles/effects (no dependencies).</li>
 *   <li>Blocks -> entities -> items -> block entities (items need blocks &amp; entity types).</li>
 *   <li>Worldgen, which registers the Shattered Sky chunk generator codec.</li>
 *   <li>Content bootstraps: {@link SpellRegistry} and {@link ChronicleRegistry}.</li>
 *   <li>Networking receivers.</li>
 *   <li>Server events: capability ticking, Chronicle evaluation, star-fall, storms, blight.</li>
 * </ol>
 */
public final class Aetherius implements ModInitializer {

    public static final String MOD_ID = "aetherius";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final TemporalStormManager TEMPORAL_STORMS = new TemporalStormManager();

    @Override
    public void onInitialize() {
        long start = System.currentTimeMillis();

        ModSounds.register();
        ModParticles.register();
        ModStatusEffects.register();

        ModBlocks.register();
        ModEntities.register();
        ModItems.register();
        ModBlockEntities.register();
        ModItemGroups.register();
        ModEntities.registerAttributes();
        ModEntities.registerSpawnRestrictions();
        ModWorldGen.register();

        SpellRegistry.bootstrap();
        ChronicleRegistry.bootstrap();
        AetherNetworking.registerServerReceivers();

        registerServerEvents();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                AetheriusCommand.register(dispatcher));

        LOGGER.info("[Aetherius] Initialised in {} ms - {} spells, {} quests, {} lore entries",
                System.currentTimeMillis() - start,
                SpellRegistry.all().size(),
                ChronicleRegistry.allQuests().size(),
                ChronicleRegistry.allLore().size());
    }

    private void registerServerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ShatteredSkyChunkGenerator.setWorldSeed(server.getOverworld().getSeed());
            LOGGER.info("[Aetherius] World seed {} bound to the Shattered Sky generator",
                    server.getOverworld().getSeed());
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Send the full state once; subsequent updates ride the dirty flag.
            AetherNetworking.syncMagic(player, MagicComponents.get(player));
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                AetherNetworking.syncMagic(newPlayer, MagicComponents.get(newPlayer)));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                ChronicleManager.get().onEntityKilled(player, entity);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                MagicComponents.get(player).tick();
            }
            ChronicleManager.get().onServerTick(server);
            StarfallManager.get().tick(server);
            TEMPORAL_STORMS.tick(server);
            if (server.getTicks() % 20 == 0) {
                CelestialBlightManager.get().tick(server.getOverworld());
            }
        });
    }
}
