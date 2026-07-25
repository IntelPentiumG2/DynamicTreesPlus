package com.dtteam.dynamictreesplus.event;

import com.dtteam.dynamictrees.event.DataGenerationStreamEvent;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;

@EventBusSubscriber
public class GenerateLootTableHandler {

    @SubscribeEvent
    public static void onLootTableProviderGenerate(final DataGenerationStreamEvent event) {
        CapProperties.REGISTRY.dataGenerationStream(event.getModId()).forEach(capProperties -> {
            addCapBlockTable(capProperties, event.getMap(), event.getRegistries());
            addCapTable(capProperties, event.getMap(), event.getRegistries());
        });
    }

    // ExistingFileHelper is gone in 26.1, so hand-authored tables are no longer skipped here.
    // putIfAbsent keeps whatever the data pack already contributed instead.
    private static void addCapBlockTable(CapProperties capProperties, Map<ResourceKey<LootTable>, LootTable.Builder> map, HolderLookup.Provider registries) {
        if (capProperties.shouldGenerateBlockDrops()) {
            final Identifier capBlockTablePath = capProperties.getBlockLootTableName();
            map.putIfAbsent(ResourceKey.create(Registries.LOOT_TABLE, capBlockTablePath), capProperties.createBlockDrops(registries));
        }
    }

    private static void addCapTable(CapProperties capProperties, Map<ResourceKey<LootTable>, LootTable.Builder> map, HolderLookup.Provider registries) {
        if (capProperties.shouldGenerateDrops()) {
            final Identifier capTablePath = capProperties.getLootTableName();
            map.putIfAbsent(ResourceKey.create(Registries.LOOT_TABLE, capTablePath), capProperties.createDrops(registries));
        }
    }

}
