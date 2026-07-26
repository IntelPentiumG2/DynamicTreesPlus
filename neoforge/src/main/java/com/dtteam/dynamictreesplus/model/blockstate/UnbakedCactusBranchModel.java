package com.dtteam.dynamictreesplus.model.blockstate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;

/**
 * The unbaked form of the cactus branch model, mirroring Dynamic Trees' UnbakedBranchModel.
 *
 * <p>Replaces the model loader and geometry pair this used before 26.1: block states now name the
 * model type directly, so a codec over the two textures is all that is needed.</p>
 */
public record UnbakedCactusBranchModel(Identifier barkTexture,
                                       Identifier ringsTexture) implements CustomUnbakedBlockStateModel {

    public static final String BARK_TEXTURE = "bark";
    public static final String RINGS_TEXTURE = "rings";
    public static final String TEXTURES = "textures";

    private record CactusTextures(Identifier bark, Identifier rings) {
        static final MapCodec<CactusTextures> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf(BARK_TEXTURE).forGetter(CactusTextures::bark),
                Identifier.CODEC.fieldOf(RINGS_TEXTURE).forGetter(CactusTextures::rings)
        ).apply(i, CactusTextures::new));
    }

    public static final MapCodec<UnbakedCactusBranchModel> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CactusTextures.CODEC.codec().fieldOf(TEXTURES)
                    .forGetter(m -> new CactusTextures(m.barkTexture(), m.ringsTexture()))
    ).apply(i, textures -> new UnbakedCactusBranchModel(textures.bark(), textures.rings())));

    @Override
    public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
        return CODEC;
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver resolver) {
    }

    @Override
    public BlockStateModel bake(ModelBaker baker) {
        Material.Baked bark = baker.materials().get(new Material(barkTexture), barkTexture::toDebugFileName);
        Material.Baked rings = baker.materials().get(new Material(ringsTexture), ringsTexture::toDebugFileName);
        return new CactusBranchBlockStateModel(baker, bark, rings);
    }

}
