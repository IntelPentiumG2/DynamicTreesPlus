package com.dtteam.dynamictreesplus.model.blockstate;

import com.dtteam.dynamictrees.api.network.Connections;
import com.dtteam.dynamictrees.block.branch.BranchBlock;
import com.dtteam.dynamictrees.model.BlockStateModelWithConnectionData;
import com.dtteam.dynamictrees.model.ModelConnections;
import com.dtteam.dynamictrees.model.ModelHelper;
import com.dtteam.dynamictreesplus.block.CactusBranchBlock;
import com.google.common.collect.Maps;
import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CactusBranchBlockStateModel implements DynamicBlockStateModel, BlockStateModelWithConnectionData {

    private static final float SIXTEENTH = 0.0625f;
    private static final float SPIKE_OFFSET_1 = 0.0001f;
    private static final float SPIKE_OFFSET_2 = 0.0002f;

    private final Material.Baked barkTexture;

    // Not as many parts as normal branches, although each part has more quads. Still fewer quads in total, though.
    private final QuadCollection[][] sleeves = new QuadCollection[6][3];
    private final QuadCollection[][] cores = new QuadCollection[3][3]; // 3 Cores for 3 axis with the bark texture on all 6 sides rotated appropriately.
    private final QuadCollection[] rings = new QuadCollection[3]; // 3 Cores with the ring textures on all 6 sides
    private final QuadCollection[] coreSpikes = new QuadCollection[3]; // 3 cores with only the spikey edges
    private QuadCollection sleeveTopSpikes;

    int[] radii = {4, 5, 7};

    public CactusBranchBlockStateModel(ModelBaker baker, Material.Baked barkTexture, Material.Baked ringsTexture) {
        this.barkTexture = barkTexture;
        initModels(baker, ringsTexture);
    }

    public void initModels(ModelBaker baker, Material.Baked ringsTexture) {
        for (int i = 0; i < 3; i++) {
            int radius = radii[i];

            for (Direction dir : Direction.values()) {
                sleeves[dir.get3DDataValue()][i] = bakeSleeve(baker, radius, dir, barkTexture, ringsTexture);
            }

            cores[0][i] = bakeCore(baker, radius, Axis.Y, barkTexture); //DOWN<->UP
            cores[1][i] = bakeCore(baker, radius, Axis.Z, barkTexture); //NORTH<->SOUTH
            cores[2][i] = bakeCore(baker, radius, Axis.X, barkTexture); //WEST<->EAST

            rings[i] = bakeCore(baker, radius, Axis.Y, ringsTexture);

            coreSpikes[i] = bakeCoreSpikes(radius, barkTexture);
            sleeveTopSpikes = bakeTopSleeveSpikes(barkTexture);
        }
    }

    private void putVertex(QuadBakingVertexConsumer builder, Vec3 normal, double x, double y, double z, float u, float v, Material.Baked sprite, float r, float g, float b, Direction face) {
        builder.addVertex((float)x, (float)y, (float)z);
        builder.setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        builder.setColor(r, g, b, 1.0F);
        builder.setUv(sprite.sprite().getU(u), sprite.sprite().getV(v));
        builder.setSprite(sprite);
        builder.setDirection(face);
    }

    private BakedQuad createQuad(Vec3 v1, float v1u, float v1v, Vec3 v2, float v2u, float v2v, Vec3 v3, float v3u, float v3v, Vec3 v4, float v4u, float v4v, Material.Baked sprite) {
        Vec3 normal = v3.subtract(v2).cross(v1.subtract(v2)).normalize();

        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        Direction face = Direction.getApproximateNearest(normal.x, normal.y, normal.z);
        putVertex(builder, normal, v1.x, v1.y, v1.z, v1u, v1v, sprite, 1.0f, 1.0f, 1.0f, face);
        putVertex(builder, normal, v2.x, v2.y, v2.z, v2u, v2v, sprite, 1.0f, 1.0f, 1.0f, face);
        putVertex(builder, normal, v3.x, v3.y, v3.z, v3u, v3v, sprite, 1.0f, 1.0f, 1.0f, face);
        putVertex(builder, normal, v4.x, v4.y, v4.z, v4u, v4v, sprite, 1.0f, 1.0f, 1.0f, face);
        return builder.bakeQuad();
    }

    public QuadCollection bakeSleeve(ModelBaker baker, int radius, Direction dir, Material.Baked bark, Material.Baked top) {
        // Work in double units(*2)
        int dradius = radius * 2;
        int halfSize = (16 - dradius) / 2;
        int halfSizeX = dir.getStepX() != 0 ? halfSize : dradius;
        int halfSizeY = dir.getStepY() != 0 ? halfSize : dradius;
        int halfSizeZ = dir.getStepZ() != 0 ? halfSize : dradius;
        int move = 16 - halfSize;
        int centerX = 16 + (dir.getStepX() * move);
        int centerY = 16 + (dir.getStepY() * move);
        int centerZ = 16 + (dir.getStepZ() * move);

        Vector3f posFrom = new Vector3f((centerX - halfSizeX) / 2f, (centerY - halfSizeY) / 2f, (centerZ - halfSizeZ) / 2f);
        Vector3f posTo = new Vector3f((centerX + halfSizeX) / 2f, (centerY + halfSizeY) / 2f, (centerZ + halfSizeZ) / 2f);

        boolean negative = dir.getAxisDirection() == AxisDirection.NEGATIVE;
        if (dir.getAxis() == Axis.Z) { // North/South
            negative = !negative;
        }

        Map<Direction, CuboidFace> mapFacesIn = Maps.newEnumMap(Direction.class);

        for (Direction face : Direction.values()) {
            if (dir.getOpposite() != face) { // Discard side of sleeve that faces core
                CuboidFace.UVs uvface = null;
                Quadrant uvrot = Quadrant.R0;
                if (dir == face) { // Side of sleeve that faces away from core
                    if (radius == 4 || (radius == 5 && dir == Direction.DOWN)) {
                        uvface = new CuboidFace.UVs(8 - radius, 8 - radius, 8 + radius, 8 + radius);
                    }
                } else { // UV for Bark texture
                    uvface = new CuboidFace.UVs(8 - radius, negative ? 16 - halfSize : 0, 8 + radius, negative ? 16 : halfSize);
                    uvrot = getFaceAngle(dir.getAxis(), face);
                }
                if (uvface != null) {
                    mapFacesIn.put(face, new CuboidFace(null, -1, "", uvface, uvrot));
                }
            }
        }

        CuboidModelElement part = new CuboidModelElement(posFrom, posTo, mapFacesIn, null, true, 0);
        QuadCollection.Builder builder = new QuadCollection.Builder();

        for (Map.Entry<Direction, CuboidFace> e : part.faces().entrySet()) {
            Direction face = e.getKey();
            builder.addCulledFace(face, ModelHelper.makeBakedQuad(baker, part, e.getValue(), (dir == face) ? top : bark, face));
        }
        float minV = (negative ? 16 - halfSize : 0) / 16f;
        float maxV = (negative ? 16 : halfSize) / 16f;
        switch (dir.getAxis()) {
            case X:

                builder.addCulledFace(Direction.NORTH, this.createQuad(
                        v(posTo.x() / 16f, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 16/ 16f, minV,
                        v(posTo.x() / 16f, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 14/ 16f, minV,
                        v(posFrom.x() / 16f, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 14/ 16f, maxV,
                        v(posFrom.x() / 16f, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 16/ 16f, maxV, bark));
                builder.addCulledFace(Direction.NORTH, this.createQuad(
                        v(posTo.x() / 16f, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 2/ 16f, minV,
                        v(posTo.x() / 16f, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 0, minV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 0, maxV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 2/ 16f, maxV, bark));
                builder.addCulledFace(Direction.SOUTH, this.createQuad(
                        v(posFrom.x() / 16f, posTo.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 16/ 16f, maxV,
                        v(posFrom.x() / 16f, posTo.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 14/ 16f, maxV,
                        v(posTo.x() / 16f, posTo.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 14/ 16f, minV,
                        v(posTo.x() / 16f, posTo.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 16/ 16f, minV, bark));
                builder.addCulledFace(Direction.SOUTH, this.createQuad(
                        v(posFrom.x() / 16f, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 2/ 16f, maxV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 0, maxV,
                        v(posTo.x() / 16f, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 0, minV,
                        v(posTo.x() / 16f, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 2/ 16f, minV, bark));

                builder.addCulledFace(Direction.DOWN, this.createQuad(
                        v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                        v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV, bark));
                builder.addCulledFace(Direction.DOWN, this.createQuad(
                        v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, minV,
                        v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV,
                        v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, maxV, bark));
                builder.addCulledFace(Direction.UP, this.createQuad(
                        v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                        v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                        v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                        v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, minV, bark));
                builder.addCulledFace(Direction.UP, this.createQuad(
                        v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 2/ 16f, maxV,
                        v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 0, maxV,
                        v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 0, minV,
                        v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 2/ 16f, minV, bark));

                break;
            case Y:

                builder.addCulledFace(Direction.WEST, this.createQuad(
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV, bark));
                builder.addCulledFace(Direction.WEST, this.createQuad(
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, maxV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV, bark));
                builder.addCulledFace(Direction.EAST, this.createQuad(
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, minV, bark));
                builder.addCulledFace(Direction.EAST, this.createQuad(
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, minV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV, bark));

                builder.addCulledFace(Direction.NORTH, this.createQuad(
                        v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 16/ 16f, maxV,
                        v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 14/ 16f, maxV,
                        v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 14/ 16f, minV,
                        v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 16/ 16f, minV, bark));
                builder.addCulledFace(Direction.NORTH, this.createQuad(
                        v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 2/ 16f, maxV,
                        v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 0, maxV,
                        v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 0, minV,
                        v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 2/ 16f, minV, bark));
                builder.addCulledFace(Direction.SOUTH, this.createQuad(
                        v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 16/ 16f, minV,
                        v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 14/ 16f, minV,
                        v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 14/ 16f, maxV,
                        v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 16/ 16f, maxV, bark));
                builder.addCulledFace(Direction.SOUTH, this.createQuad(
                        v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 2/ 16f, minV,
                        v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 0, minV,
                        v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 0, maxV,
                        v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 2/ 16f, maxV, bark));


                break;
            case Z:

                builder.addCulledFace(Direction.WEST, this.createQuad(
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV, bark));
                builder.addCulledFace(Direction.WEST, this.createQuad(
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f), 2/ 16f, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f), 0, minV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f), 0, maxV,
                        v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f), 2/ 16f, maxV, bark));
                builder.addCulledFace(Direction.EAST, this.createQuad(
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV, bark));
                builder.addCulledFace(Direction.EAST, this.createQuad(
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f), 2/ 16f, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f), 0, maxV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f), 0, minV,
                        v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f), 2/ 16f, minV, bark));

                builder.addCulledFace(Direction.DOWN, this.createQuad(
                        v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 16/ 16f, maxV,
                        v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 14/ 16f, maxV,
                        v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 14/ 16f, minV,
                        v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 16/ 16f, minV, bark));
                builder.addCulledFace(Direction.DOWN, this.createQuad(
                        v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 2/ 16f, maxV,
                        v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 0, maxV,
                        v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 0, minV,
                        v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 2/ 16f, minV, bark));
                builder.addCulledFace(Direction.UP, this.createQuad(
                        v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 16/ 16f, minV,
                        v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 14/ 16f, minV,
                        v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 14/ 16f, maxV,
                        v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 16/ 16f, maxV, bark));
                builder.addCulledFace(Direction.UP, this.createQuad(
                        v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 2/ 16f, minV,
                        v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 0, minV,
                        v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 0, maxV,
                        v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 2/ 16f, maxV, bark));

                break;
        }

        return builder.build();
    }

    public QuadCollection bakeCore(ModelBaker baker, int radius, Axis axis, Material.Baked icon) {

        Vector3f posFrom = new Vector3f(8 - radius, 8 - radius, 8 - radius);
        Vector3f posTo = new Vector3f(8 + radius, 8 + radius, 8 + radius);

        Map<Direction, CuboidFace> mapFacesIn = Maps.newEnumMap(Direction.class);

        for (Direction face : Direction.values()) {
            CuboidFace.UVs uvface = new CuboidFace.UVs(8 - radius, 8 - radius, 8 + radius, 8 + radius);
            mapFacesIn.put(face, new CuboidFace(null, -1, "", uvface, getFaceAngle(axis, face)));
        }

        CuboidModelElement part = new CuboidModelElement(posFrom, posTo, mapFacesIn, null, true, 0);
        QuadCollection.Builder builder = new QuadCollection.Builder();

        for (Map.Entry<Direction, CuboidFace> e : part.faces().entrySet()) {
            Direction face = e.getKey();
            builder.addCulledFace(face, ModelHelper.makeBakedQuad(baker, part, e.getValue(), icon, face));
        }

        return builder.build();
    }

    public QuadCollection bakeCoreSpikes(int radius, Material.Baked bark) {
        float minV = (8 - radius) / 16f;
        float maxV = (8 + radius) / 16f;

        Vector3f posFrom = new Vector3f(8 - radius, 8 - radius, 8 - radius);
        Vector3f posTo = new Vector3f(8 + radius, 8 + radius, 8 + radius);

        QuadCollection.Builder builder = new QuadCollection.Builder();

        // X
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posTo.x() / 16f, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 16/ 16f, minV,
                v(posTo.x() / 16f, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 14/ 16f, minV,
                v(posFrom.x() / 16f, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 14/ 16f, maxV,
                v(posFrom.x() / 16f, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.DOWN, this.createQuad(
                v(posTo.x() / 16f, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 2/ 16f, minV,
                v(posTo.x() / 16f, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 0, minV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 0, maxV,
                v(posFrom.x() / 16f, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f - SPIKE_OFFSET_2), 2/ 16f, maxV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom.x() / 16f, posTo.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 16/ 16f, maxV,
                v(posFrom.x() / 16f, posTo.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 14/ 16f, maxV,
                v(posTo.x() / 16f, posTo.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 14/ 16f, minV,
                v(posTo.x() / 16f, posTo.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.DOWN, this.createQuad(
                v(posFrom.x() / 16f, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 2/ 16f, maxV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 0, maxV,
                v(posTo.x() / 16f, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 0, minV,
                v(posTo.x() / 16f, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f + SPIKE_OFFSET_2), 2/ 16f, minV, bark));

        builder.addCulledFace(Direction.SOUTH, this.createQuad(
                v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV, bark));
        builder.addCulledFace(Direction.NORTH, this.createQuad(
                v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, minV,
                v(posTo.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV,
                v(posFrom.x() / 16f, posFrom.y() / 16f - SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, maxV, bark));
        builder.addCulledFace(Direction.SOUTH, this.createQuad(
                v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posTo.z() / 16f - 0.0625f), 14/ 16f, minV, bark));
        builder.addCulledFace(Direction.NORTH, this.createQuad(
                v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, maxV,
                v(posFrom.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV,
                v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV,
                v(posTo.x() / 16f, posTo.y() / 16f + SPIKE_OFFSET_2, posFrom.z() / 16f - 0.0625f), 0, minV, bark));

        // Y
        builder.addCulledFace(Direction.SOUTH, this.createQuad(
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.NORTH, this.createQuad(
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, maxV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV, bark));
        builder.addCulledFace(Direction.SOUTH, this.createQuad(
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f - 0.0625f), 14/ 16f, minV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posTo.z() / 16f + 0.0625f), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.NORTH, this.createQuad(
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posFrom.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f - 0.0625f), 0, minV,
                v(posTo.x() / 16f + SPIKE_OFFSET_1, posTo.y() / 16f, posFrom.z() / 16f + 0.0625f), 2/ 16f, minV, bark));

        builder.addCulledFace(Direction.EAST, this.createQuad(
                v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 16/ 16f, maxV,
                v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 14/ 16f, maxV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 14/ 16f, minV,
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.WEST, this.createQuad(
                v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 2/ 16f, maxV,
                v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 0, maxV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 0, minV,
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f, posFrom.z() / 16f - SPIKE_OFFSET_1), 2/ 16f, minV, bark));
        builder.addCulledFace(Direction.EAST, this.createQuad(
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 16/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 14/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 14/ 16f, maxV,
                v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.WEST, this.createQuad(
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 2/ 16f, minV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 0, minV,
                v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 0, maxV,
                v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f, posTo.z() / 16f + SPIKE_OFFSET_1), 2/ 16f, maxV, bark));

        // Z
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.DOWN, this.createQuad(
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f), 2/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f), 0, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f), 0, maxV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f), 2/ 16f, maxV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.DOWN, this.createQuad(
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posTo.z() / 16f), 2/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posTo.z() / 16f), 0, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f - 0.0625f, posFrom.z() / 16f), 0, minV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posFrom.y() / 16f + 0.0625f, posFrom.z() / 16f), 2/ 16f, minV, bark));

        builder.addCulledFace(Direction.EAST, this.createQuad(
                v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 16/ 16f, maxV,
                v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 14/ 16f, maxV,
                v(posTo.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 14/ 16f, minV,
                v(posTo.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.WEST, this.createQuad(
                v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 2/ 16f, maxV,
                v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posTo.z() / 16f), 0, maxV,
                v(posFrom.x() / 16f - 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 0, minV,
                v(posFrom.x() / 16f + 0.0625f, posFrom.y() / 16f - SPIKE_OFFSET_1, posFrom.z() / 16f), 2/ 16f, minV, bark));
        builder.addCulledFace(Direction.EAST, this.createQuad(
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 16/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 14/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 14/ 16f, maxV,
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.WEST, this.createQuad(
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 2/ 16f, minV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 0, minV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 0, maxV,
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 2/ 16f, maxV, bark));


        return builder.build();
    }

    public QuadCollection bakeTopSleeveSpikes(Material.Baked bark) {
        float minV = 4/ 16f;
        float maxV = 12/ 16f;

        Vector3f posFrom = new Vector3f(4, 16, 4);
        Vector3f posTo = new Vector3f(12, 16, 12);

        QuadCollection.Builder builder = new QuadCollection.Builder();


        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posTo, posTo, posFrom, 0, +0.0625f, -SPIKE_OFFSET_2), 16/ 16f, minV,
                v(posTo, posTo, posFrom, 0, -0.0625f, -SPIKE_OFFSET_2), 14/ 16f, minV,
                v(posFrom, posTo, posFrom, 0, -0.0625f, -SPIKE_OFFSET_2), 14/ 16f, maxV,
                v(posFrom, posTo, posFrom, 0, +0.0625f, -SPIKE_OFFSET_2), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom, posTo, posTo, 0, +0.0625f, +SPIKE_OFFSET_2), 16/ 16f, maxV,
                v(posFrom, posTo, posTo, 0, -0.0625f, +SPIKE_OFFSET_2), 14/ 16f, maxV,
                v(posTo, posTo, posTo, 0, -0.0625f, +SPIKE_OFFSET_2), 14/ 16f, minV,
                v(posTo, posTo, posTo, 0, +0.0625f, +SPIKE_OFFSET_2), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom, posTo, posTo, 0, +SPIKE_OFFSET_2, -0.0625f), 14/ 16f, maxV,
                v(posFrom, posTo, posTo, 0, +SPIKE_OFFSET_2, +0.0625f), 16/ 16f, maxV,
                v(posTo, posTo, posTo, 0, +SPIKE_OFFSET_2, +0.0625f), 16/ 16f, minV,
                v(posTo, posTo, posTo, 0, +SPIKE_OFFSET_2, -0.0625f), 14/ 16f, minV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom, posTo, posFrom, 0, +SPIKE_OFFSET_2, -0.0625f), 0, maxV,
                v(posFrom, posTo, posFrom, 0, +SPIKE_OFFSET_2, +0.0625f), 2/ 16f, maxV,
                v(posTo, posTo, posFrom, 0, +SPIKE_OFFSET_2, +0.0625f), 2/ 16f, minV,
                v(posTo, posTo, posFrom, 0, +SPIKE_OFFSET_2, -0.0625f), 0, minV, bark));

        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                v(posFrom.x() / 16f - SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posTo.z() / 16f), 16/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posTo.z() / 16f), 14/ 16f, maxV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f - 0.0625f, posFrom.z() / 16f), 14/ 16f, minV,
                v(posTo.x() / 16f + SPIKE_OFFSET_2, posTo.y() / 16f + 0.0625f, posFrom.z() / 16f), 16/ 16f, minV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 16/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 14/ 16f, minV,
                v(posTo.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 14/ 16f, maxV,
                v(posTo.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 16/ 16f, maxV, bark));
        builder.addCulledFace(Direction.UP, this.createQuad(
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 2/ 16f, minV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posFrom.z() / 16f), 0, minV,
                v(posFrom.x() / 16f - 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 0, maxV,
                v(posFrom.x() / 16f + 0.0625f, posTo.y() / 16f + SPIKE_OFFSET_1, posTo.z() / 16f), 2/ 16f, maxV, bark));


        return builder.build();
    }

    /**
     * A Hack to determine the UV face angle for a block column on a certain axis
     *
     * @param axis
     * @param face
     * @return
     */
    public Quadrant getFaceAngle(Axis axis, Direction face) {
        if (axis == Axis.Y) { //UP / DOWN
            return Quadrant.R0;
        } else if (axis == Axis.Z) {//NORTH / SOUTH
            switch (face) {
                case UP:
                    return Quadrant.R0;
                case WEST:
                    return Quadrant.R270;
                case DOWN:
                    return Quadrant.R180;
                default:
                    return Quadrant.R90;
            }
        } else { //EAST/WEST
            return (face == Direction.NORTH) ? Quadrant.R270 : Quadrant.R90;
        }
    }

    private Vec3 v(Vector3f xVec, Vector3f yVec, Vector3f zVec, float xOffset, float yOffset, float zOffset) {
        return v(xVec.x() / 16f + xOffset, yVec.y() / 16f + yOffset, zVec.z() / 16f + zOffset);
    }

    private Vec3 v(float x, float y, float z) {
        return new Vec3(x, y, z);
    }

    private int getRadiusIndex(int radius) {
        for (int i = 0; i < radii.length; i++) {
            if (radius == radii[i]) return i;
        }
        return 0;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        return ModelHelper.getModelConnections(level, pos, state);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        collectParts(state, parts, ModelHelper.getModelConnections(level, pos, state));
    }

    /**
     * The cactus does its own culling from the connection data, so every quad it selects is added as
     * an unculled face -- the same thing the old baked model did by only answering for a null side.
     */
    @Override
    public void collectParts(BlockState state, List<BlockStateModelPart> parts, Connections connectionsData) {
        final List<BakedQuad> quadsList = new ArrayList<>(12);

        int coreRadius = this.getRadius(state);
        // Nothing below this point holds for a state that is not a cactus branch, and the ORIGIN
        // lookup further down would throw on one. Dynamic Trees' own branch model bails the same way.
        if (coreRadius == 0) {
            return;
        }

        int[] connections = new int[]{0, 0, 0, 0, 0, 0};
        Direction forceRingDir = null;
        if (connectionsData instanceof ModelConnections modelConnections) {
            connections = modelConnections.getAllRadii();
            forceRingDir = modelConnections.getRingOnly();
        }

        //Count number of connections
        int numConnections = 0;
        for (int i : connections) {
            numConnections += (i != 0) ? 1 : 0;
        }

        if (numConnections == 0 && forceRingDir != null) {
            quadsList.addAll(rings[getRadiusIndex(coreRadius)].getQuads(forceRingDir));
        } else {
            boolean extraUpSleeve = false;
            if (coreRadius == radii[0] && numConnections == 1 && state.getValue(CactusBranchBlock.ORIGIN).getAxis().isHorizontal()) {
                connections[1] = radii[0];
                extraUpSleeve = true;
            }

            //The source direction is the biggest connection from one of the 6 directions
            Direction sourceDir = getSourceDir(coreRadius, connections);
            if (sourceDir == null) {
                sourceDir = Direction.DOWN;
            }
            int coreDir = resolveCoreDir(sourceDir);

            // This is for drawing the rings on a terminating branch
            Direction coreRingDir = (numConnections == 1) ? sourceDir.getOpposite() : null;

            for (Direction face : Direction.values()) {
                //Get quads for core model
                if (coreRadius != connections[face.get3DDataValue()]) {
                    if (coreRingDir == null || coreRingDir != face) {
                        quadsList.addAll(cores[coreDir][getRadiusIndex(coreRadius)].getQuads(face));
                    } else {
                        quadsList.addAll(rings[getRadiusIndex(coreRadius)].getQuads(face));
                    }
                }

                // Get quads for core spikes
                for (Direction dir : Direction.values()) {
                    if (coreRadius > connections[dir.get3DDataValue()]) {
                        for (BakedQuad quad : coreSpikes[getRadiusIndex(coreRadius)].getQuads(dir)) {
                            if (coreRadius > connections[quad.direction().get3DDataValue()]) {
                                quadsList.add(quad);
                            }
                        }
                    }
                }

                // Get quads for sleeves models
                for (Direction connDir : Direction.values()) {
                    int idx = connDir.get3DDataValue();
                    int connRadius = connections[idx];
                    // If the connection side matches the quadpull side then cull the sleeve face.  Don't cull radius 1 connections for leaves(which are partly transparent).
                    if (connRadius >= radii[0] && ((connDir == Direction.UP && connRadius == radii[0] && extraUpSleeve) || face != connDir || connDir == Direction.DOWN)) {
                        quadsList.addAll(sleeves[idx][getRadiusIndex(connRadius)].getQuads(face));
                    }
                }
            }

            if (extraUpSleeve) {
                quadsList.addAll(sleeveTopSpikes.getQuads(Direction.UP));
            }
        }

        if (quadsList.isEmpty()) {
            return;
        }

        final QuadCollection.Builder builder = new QuadCollection.Builder();
        quadsList.forEach(builder::addUnculledFace);
        parts.add(new CactusModelPart(builder.build(), barkTexture));
    }

    private record CactusModelPart(QuadCollection quads, Material.Baked particle) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return this.quads.getQuads(direction);
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return this.quads.materialFlags();
        }

        @Override
        public Material.Baked particleMaterial() {
            return this.particle;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }
    }

    @Override
    public Material.Baked particleMaterial() {
        return barkTexture;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return cores[0][0].materialFlags();
    }

    /**
     * Checks all neighboring tree parts to determine the connection radius for each side of this branch block.
     */
    protected Direction getSourceDir(int coreRadius, int[] connections) {
        int largestConnection = 0;
        Direction sourceDir = null;

        for (Direction dir : Direction.values()) {
            int connRadius = connections[dir.get3DDataValue()];
            if (connRadius > largestConnection) {
                largestConnection = connRadius;
                sourceDir = dir;
            }
        }

        if (largestConnection < coreRadius) {
            sourceDir = null;//Has no source node
        }
        return sourceDir;
    }

    /**
     * Converts direction DUNSWE to 3 axis numbers for Y,Z,X
     *
     * @param dir
     * @return
     */
    protected int resolveCoreDir(Direction dir) {
        return dir.get3DDataValue() >> 1;
    }

    protected int getRadius(BlockState blockState) {
        // The block answers, because branches like the cactus have no RADIUS property to read.
        // The type is checked rather than assumed: model bakers that run outside a level, such as
        // Voxy's, call collectParts with whatever state they hold, air included.
        return blockState.getBlock() instanceof CactusBranchBlock cactusBranch
                ? cactusBranch.getRadius(blockState)
                : 0;
    }

}
