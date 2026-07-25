package com.dtteam.dynamictreesplus.data;

import com.dtteam.dynamictreesplus.block.mushroom.CapProperties;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bridges the cap properties' texture consumers onto 26.1's model templates.
 *
 * <p>Before 26.1 these models were assembled with NeoForge's BlockModelBuilder, which took texture
 * keys as plain strings. The replacement wants a {@link ModelTemplate} with declared
 * {@link TextureSlot}s, so the slots are derived from whatever keys the cap properties emit.</p>
 */
public final class CapModelHelper {

    private CapModelHelper() {
    }

    static Identifier createFaceModel(BlockModelGenerators generators, CapProperties properties, String modelName,
                                      Identifier parent, Identifier texture, boolean inside) {
        final Map<String, Identifier> textures = new HashMap<>();
        properties.addCapFaceTextures(textures::put, texture, inside);
        return create(generators, properties, modelName, parent, textures);
    }

    static Identifier createCapCenterAgeZeroModel(BlockModelGenerators generators, CapProperties properties,
                                                  Identifier outTexture, Identifier inTexture) {
        final Map<String, Identifier> textures = new HashMap<>();
        properties.addCapCenterAgeZeroTextures(textures::put, outTexture, inTexture);
        return create(generators, properties, properties.getCapCenterAgeZeroModelName(),
                properties.getCapCenterAgeZeroModelParent(), textures);
    }

    private static Identifier create(BlockModelGenerators generators, CapProperties properties, String modelName,
                                     Identifier parent, Map<String, Identifier> textures) {
        final TextureMapping mapping = new TextureMapping();
        final TextureSlot[] slots = new TextureSlot[textures.size()];

        int i = 0;
        for (Map.Entry<String, Identifier> entry : textures.entrySet()) {
            final TextureSlot slot = TextureSlot.create(entry.getKey());
            mapping.put(slot, new Material(entry.getValue()));
            slots[i++] = slot;
        }

        final Identifier target = properties.getRegistryName().withPath(modelName);
        return new ModelTemplate(Optional.of(parent), Optional.empty(), slots)
                .create(target, mapping, generators.modelOutput);
    }

}
