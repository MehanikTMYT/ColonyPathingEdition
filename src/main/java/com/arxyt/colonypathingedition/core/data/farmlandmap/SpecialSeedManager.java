package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.arxyt.colonypathingedition.core.network.message.SpecialSeedSyncMessage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager of seeds with special farmland.
 */
public class SpecialSeedManager {

    private static final Map<Item, Block> SPECIAL_SEEDS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ColonyPathingEdition.LOGGER.info(
                "[SpecialSeeds] Syncing seed data to {}",
                player.getGameProfile().name()
        );

        new SpecialSeedSyncMessage(FarmlandMapLoader.getMappings())
                .syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddServerReloadListenersEvent event) {
        event.addListener(
                Identifier.fromNamespaceAndPath(AdditionalContants.MOD_ID, "farmland_map"),
                FarmlandMapLoader.INSTANCE
        );
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            new SpecialSeedSyncMessage(FarmlandMapLoader.getMappings()).syncToAll();
        }
    }

    public static void rebuildFromMappings(
            Map<Identifier, Identifier> mappings) {

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Rebuilding mappings...");

        SPECIAL_SEEDS.clear();

        for (var entry : mappings.entrySet()) {

            Item seed = BuiltInRegistries.ITEM.getValue(entry.getKey());
            Block soil = BuiltInRegistries.BLOCK.getValue(entry.getValue());

            if (seed == Items.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No seed {}", entry.getKey());
                continue;
            }
            if (soil == Blocks.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No farmland {}", entry.getValue());
                continue;
            }

            SPECIAL_SEEDS.put(seed, soil);
        }

        ColonyPathingEdition.LOGGER.info(
                "[SpecialSeeds] Active special seeds: {}", SPECIAL_SEEDS.size());
    }

    public static boolean isSpecialSeed(Item seed) {
        return SPECIAL_SEEDS.containsKey(seed);
    }

    public static Block getRequiredSoil(Item seed) {
        return SPECIAL_SEEDS.get(seed);
    }

    public static boolean isSpecialSoil(Block farmland) {
        return SPECIAL_SEEDS.containsValue(farmland);
    }
}
