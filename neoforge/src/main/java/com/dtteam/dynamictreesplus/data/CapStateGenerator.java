package com.dtteam.dynamictreesplus.data;

import com.dtteam.dynamictrees.data.Generator;
import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import com.dtteam.dynamictreesplus.block.mushroom.DynamicCapBlock;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class CapStateGenerator implements Generator<BlockModelGenerators, CapProperties> {

    public static final DependencyKey<DynamicCapBlock> CAP = new DependencyKey<>("cap");
    public static final DependencyKey<Block> PRIMITIVE_CAP = new DependencyKey<>("primitive_cap");

    @Override
    public void generate(BlockModelGenerators generators, CapProperties input, Dependencies dependencies) {
        final Identifier outLocation = ModelLocationUtils.getModelLocation(dependencies.get(PRIMITIVE_CAP));
        final Identifier inLocation = Identifier.parse("block/mushroom_block_inside");

        final Identifier outFaceModel;
        final Identifier inFaceModel;
        if (input.shouldGenerateFaceModels()) {
            outFaceModel = CapModelHelper.createFaceModel(generators, input, input.getCapFaceModelName(),
                    input.getFaceModelParent(), outLocation, false);
            inFaceModel = CapModelHelper.createFaceModel(generators, input, input.getCapInsideFaceModelName(),
                    input.getFaceModelParent(), inLocation, true);
        } else {
            outFaceModel = input.getModelPath(CapProperties.OUTSIDE_FACE).orElse(outLocation);
            inFaceModel = input.getModelPath(CapProperties.INSIDE_FACE).orElse(inLocation);
        }

        final MultiVariant outFace = BlockModelGenerators.plainVariant(outFaceModel);
        final MultiVariant inFace = BlockModelGenerators.plainVariant(inFaceModel);

        MultiPartGenerator generator = MultiPartGenerator.multiPart(dependencies.get(CAP));
        generator = face(generator, outFace, inFace, DynamicCapBlock.NORTH, null);
        generator = face(generator, outFace, inFace, DynamicCapBlock.EAST, BlockModelGenerators.Y_ROT_90);
        generator = face(generator, outFace, inFace, DynamicCapBlock.SOUTH, BlockModelGenerators.Y_ROT_180);
        generator = face(generator, outFace, inFace, DynamicCapBlock.WEST, BlockModelGenerators.Y_ROT_270);
        generator = face(generator, outFace, inFace, DynamicCapBlock.UP, BlockModelGenerators.X_ROT_270);
        generator = face(generator, outFace, inFace, DynamicCapBlock.DOWN, BlockModelGenerators.X_ROT_90);

        generators.blockStateOutput.accept(generator);
    }

    /**
     * A cap side shows the outside texture when it is exposed and the inside texture when it is not,
     * so each direction contributes a pair of conditioned parts. Only the outside face is uv locked,
     * matching the multipart this replaces.
     */
    private static MultiPartGenerator face(MultiPartGenerator generator, MultiVariant outFace, MultiVariant inFace,
                                           BooleanProperty property, @Nullable VariantMutator rotation) {
        final MultiVariant out = rotation == null ? outFace : outFace.with(rotation);
        final MultiVariant in = rotation == null ? inFace : inFace.with(rotation);
        return generator
                .with(new ConditionBuilder().term(property, true), out.with(BlockModelGenerators.UV_LOCK))
                .with(new ConditionBuilder().term(property, false), in);
    }

    @Override
    public Dependencies gatherDependencies(CapProperties input) {
        return new Dependencies()
                .append(CAP, input.getDynamicCapBlock())
                .append(PRIMITIVE_CAP, input.getPrimitiveCapBlock());
    }

}
