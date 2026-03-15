package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowPhase;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlockCollisionConfig {
    public static final BuilderCodec<BlockCollisionConfig> CODEC;

    protected Map<String, BlockCollisionConfigEntry> rawBlockSpreadCollisionMap = Collections.emptyMap();
    protected Map<String, BlockCollisionConfigEntry> rawBlockDemoteCollisionMap = Collections.emptyMap();

    @Nullable
    protected transient Object2ObjectMap<String, BlockCollisionConfigEntry> blockSpreadCollisionMap = null;
    @Nullable
    protected transient Object2ObjectMap<String, BlockCollisionConfigEntry> blockDemoteCollisionMap = null;

    @Nonnull
    public Object2ObjectMap<String, BlockCollisionConfigEntry> getSpreadCollisionMap() {
        if (this.blockSpreadCollisionMap == null) {
            Object2ObjectMap<String, BlockCollisionConfigEntry> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockSpreadCollisionMap.size());

            for(Map.Entry<String, BlockCollisionConfigEntry> entry : this.rawBlockSpreadCollisionMap.entrySet()) {
                var block = BlockType.getAssetMap().getAsset(entry.getKey());

                if(block != null) {
                    String blockId = block.getId();
                    if (blockId != null && !blockId.isEmpty()) {
                        collisionMap.put(blockId, entry.getValue());
                    }
                }
            }

            this.blockSpreadCollisionMap = collisionMap;
        }

        return this.blockSpreadCollisionMap;
    }

    @Nonnull
    public Object2ObjectMap<String, BlockCollisionConfigEntry> getDemoteCollisionMap() {
        if (this.blockDemoteCollisionMap == null) {
            Object2ObjectMap<String, BlockCollisionConfigEntry> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockDemoteCollisionMap.size());

            for(Map.Entry<String, BlockCollisionConfigEntry> entry : this.rawBlockDemoteCollisionMap.entrySet()) {
                var block = BlockType.getAssetMap().getAsset(entry.getKey());

                if(block != null) {
                    String blockId = block.getId();
                    if (blockId != null && !blockId.isEmpty()) {
                        collisionMap.put(blockId, entry.getValue());
                    }
                }
            }

            this.blockDemoteCollisionMap = collisionMap;
        }

        return this.blockDemoteCollisionMap;
    }

    @Nonnull
    public Object2ObjectMap<String, BlockCollisionConfigEntry> getCollisionMap(FlowPhase flowPhase) {
        if(flowPhase == FlowPhase.Spread) {
            return getSpreadCollisionMap();
        } else {
            return getDemoteCollisionMap();
        }
    }


    static {
        CODEC = BuilderCodec.builder(BlockCollisionConfig.class, BlockCollisionConfig::new)
                .appendInherited(new KeyedCodec<>("Spread", new MapCodec<>(BlockCollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockSpreadCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockSpreadCollisionMap, o), (ticker) -> ticker.rawBlockSpreadCollisionMap, (ticker, parent) -> ticker.rawBlockSpreadCollisionMap = parent.rawBlockSpreadCollisionMap).documentation("Defines what happens when this fluid touches a block").add()
                .appendInherited(new KeyedCodec<>("Demote", new MapCodec<>(BlockCollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockDemoteCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockDemoteCollisionMap, o), (ticker) -> ticker.rawBlockDemoteCollisionMap, (ticker, parent) -> ticker.rawBlockDemoteCollisionMap = parent.rawBlockDemoteCollisionMap).documentation("Defines what happens when this fluid stops touching a specific block").add()
                .build();
    }
}