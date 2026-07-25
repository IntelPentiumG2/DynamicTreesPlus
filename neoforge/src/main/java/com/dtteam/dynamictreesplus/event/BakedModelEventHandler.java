package com.dtteam.dynamictreesplus.event;

import com.dtteam.dynamictreesplus.DynamicTreesPlus;
import com.dtteam.dynamictreesplus.model.blockstate.UnbakedCactusBranchModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterBlockStateModels;

/**
 * @author Harley O'Connor
 */
@EventBusSubscriber(modid = DynamicTreesPlus.MOD_ID, value = Dist.CLIENT)
public final class BakedModelEventHandler {

    @SubscribeEvent
    public static void onModelRegistryEvent(RegisterBlockStateModels event) {
        event.registerModel(DynamicTreesPlus.CACTUS, UnbakedCactusBranchModel.CODEC);
    }

}
