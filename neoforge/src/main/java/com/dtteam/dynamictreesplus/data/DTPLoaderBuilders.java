package com.dtteam.dynamictreesplus.data;

import com.dtteam.dynamictrees.data.builder.BasicLoaderBuilder;
import com.dtteam.dynamictreesplus.DynamicTreesPlus;
import com.dtteam.dynamictreesplus.model.blockstate.UnbakedCactusBranchModel;

/**
 * Teaches Dynamic Trees' branch generator how to emit the cactus block state model type.
 *
 * <p>Kept apart from the mod class and called only while gathering data: {@link BasicLoaderBuilder}
 * descends from a client-only data generation type, and the JVM verifies a whole class when it links
 * it, so naming it from any class the mod loader touches would break a dedicated server.</p>
 */
public class DTPLoaderBuilders {

    public static void register() {
        // The cactus branch model is named directly by the block state now, so the data generator
        // builds its unbaked form rather than pointing at a model loader.
        BasicLoaderBuilder.loaderBuilders.put(
                DynamicTreesPlus.CACTUS, (textures, family) ->
                        new BasicLoaderBuilder(() -> new UnbakedCactusBranchModel(
                                textures.get("bark"), textures.get("rings"))));
    }

}
