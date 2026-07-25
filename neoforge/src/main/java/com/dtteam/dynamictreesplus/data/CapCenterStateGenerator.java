package com.dtteam.dynamictreesplus.data;

import com.dtteam.dynamictrees.data.Generator;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapCenterBlock;
import com.dtteam.dynamictreesplus.systems.mushroomlogic.MushroomCapDisc;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

public class CapCenterStateGenerator implements Generator<BlockModelGenerators, CapProperties> {

    public static final DependencyKey<DynamicCapCenterBlock> CAP_CENTER = new DependencyKey<>("cap_center");
    public static final DependencyKey<Block> PRIMITIVE_CAP = new DependencyKey<>("primitive_cap");

    @Override
    public void generate(BlockModelGenerators generators, CapProperties input, Dependencies dependencies) {
        final Identifier textureOutLocation = ModelLocationUtils.getModelLocation(dependencies.get(PRIMITIVE_CAP));
        final Identifier textureInLocation = Identifier.parse("block/mushroom_block_inside");

        Identifier outLocation = textureOutLocation;
        Identifier inLocation = textureInLocation;
        if (input.shouldGenerateFaceModels()) {
            outLocation = input.getRegistryName().withPath(input.getCapFaceModelName());
            inLocation = input.getRegistryName().withPath(input.getCapInsideFaceModelName());
        }

        final MultiVariant outFace = BlockModelGenerators.plainVariant(
                input.getModelPath(CapProperties.OUTSIDE_FACE).orElse(outLocation));
        final MultiVariant inFace = BlockModelGenerators.plainVariant(
                input.getModelPath(CapProperties.INSIDE_FACE).orElse(inLocation));

        final MultiVariant ageZero = BlockModelGenerators.plainVariant(
                CapModelHelper.createCapCenterAgeZeroModel(generators, input, textureOutLocation, textureInLocation));

        final Integer[] notZeroAges = new Integer[MushroomCapDisc.MAX_RADIUS - 1];
        for (int i = 2; i <= MushroomCapDisc.MAX_RADIUS; i++) {
            notZeroAges[i - 2] = i;
        }

        generators.blockStateOutput.accept(
                MultiPartGenerator.multiPart(dependencies.get(CAP_CENTER))
                        .with(new ConditionBuilder().term(DynamicCapCenterBlock.AGE, 0), ageZero)
                        .with(grown(notZeroAges), outFace.with(BlockModelGenerators.X_ROT_270).with(BlockModelGenerators.UV_LOCK))
                        .with(grown(notZeroAges), inFace)
                        .with(grown(notZeroAges), inFace.with(BlockModelGenerators.Y_ROT_90))
                        .with(grown(notZeroAges), inFace.with(BlockModelGenerators.Y_ROT_180))
                        .with(grown(notZeroAges), inFace.with(BlockModelGenerators.Y_ROT_270))
                        .with(grown(notZeroAges), inFace.with(BlockModelGenerators.X_ROT_90))
        );
    }

    private static ConditionBuilder grown(Integer[] notZeroAges) {
        return new ConditionBuilder().term(DynamicCapCenterBlock.AGE, 1, notZeroAges);
    }

    @Override
    public Dependencies gatherDependencies(CapProperties input) {
        return new Dependencies()
                .append(CAP_CENTER, input.getDynamicCapCenterBlock())
                .append(PRIMITIVE_CAP, input.getPrimitiveCapBlock());
    }

}
