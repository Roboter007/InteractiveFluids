package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;

public enum AssetType {

    BLOCK(BlockType.getAssetMap()),
    FLUID(Fluid.getAssetMap());


    private final AssetMapWithIndexes assetMapWithIndexes;

    AssetType(AssetMapWithIndexes assetMap) {
        BlockType.getAssetMap();
        this.assetMapWithIndexes = assetMap;
    }

}
