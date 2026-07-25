package com.dtteam.dynamictreesplus.worldgen.canceller;

import com.dtteam.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.dtteam.dynamictrees.api.worldgen.FeatureCanceller;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;

/**
 * This class cancels any feature whose config is a {@link BlockColumnConfiguration} placing a block that
 * extends the cactus block class given (by default {@link CactusBlock}).
 *
 * @author Harley O'Connor
 */
public class CactusFeatureCanceller<T extends Block> extends FeatureCanceller {

    private static final RandomSource PLACEHOLDER_RANDOM = RandomSource.create(0L);

    private final Class<T> cactusBlockClass;

    public CactusFeatureCanceller(final Identifier registryName, Class<T> cactusBlockClass) {
        super(registryName);
        this.cactusBlockClass = cactusBlockClass;
    }

    @Override
    public boolean shouldCancel(ConfiguredFeature<?, ?> configuredFeature, BiomePropertySelectors.NormalFeatureCancellation featureCancellations) {
        Identifier featureResLoc = BuiltInRegistries.FEATURE.getKey(configuredFeature.feature());
        if (featureResLoc == null)
            return false;

        // Up to 1.21.1 the cactus patch was a random_patch wrapping a block_column, so the inner
        // placed feature had to be unwrapped first. Since 26.1 the configured feature is the
        // block_column itself and the scattering lives in the placement modifiers.
        FeatureConfiguration featureConfig = configuredFeature.config();

        if (!(featureConfig instanceof BlockColumnConfiguration blockColumnConfiguration) || !featureCancellations.shouldCancelNamespace(featureResLoc.getNamespace())) {
            return false;
        }

        for (BlockColumnConfiguration.Layer layer : blockColumnConfiguration.layers()) {
            final BlockStateProvider stateProvider = layer.state();
            if (!(stateProvider instanceof SimpleStateProvider)) {
                continue;
            }

            // SimpleStateProvider ignores the level, random and position, so a null level is safe here;
            // the instanceof check above guarantees we never reach a provider that would read it.
            if (this.cactusBlockClass.isInstance(stateProvider.getState(null, PLACEHOLDER_RANDOM, BlockPos.ZERO).getBlock())) {
                return true;
            }
        }

        return false;
    }
}
