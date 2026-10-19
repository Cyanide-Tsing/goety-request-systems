package com.requiem.servants.client;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** 本附属自己的模型层。 */
public final class ModModelLayers {
    public static final ModelLayerLocation CONQUEROR =
            new ModelLayerLocation(new ResourceLocation("requiem", "conqueror"), "main");

    private ModModelLayers() {
    }
}
