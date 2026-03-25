package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowPhase;
import de.Roboter007.interactiveFluids.ticker.utils.IFTOperators;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BlockCollisionConfig {
    public static final BuilderCodec<BlockCollisionConfig> CODEC;

    protected Map<String, BCConfigEntry> rawBlockSpreadCollisionMap = Collections.emptyMap();
    protected Map<String, BCConfigEntry> rawBlockDemoteCollisionMap = Collections.emptyMap();

    @Nullable
    protected transient Object2ObjectMap<BCConditionConfig, BCResultConfig> blockSpreadCollisionMap = null;
    @Nullable
    protected transient Object2ObjectMap<BCConditionConfig, BCResultConfig> blockDemoteCollisionMap = null;

    @Nonnull
    public Object2ObjectMap<BCConditionConfig, BCResultConfig> getSpreadCollisionMap() {
        if (this.blockSpreadCollisionMap == null) {
            Object2ObjectMap<BCConditionConfig, BCResultConfig> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockSpreadCollisionMap.size());

            for(Map.Entry<String, BCConfigEntry> entry : this.rawBlockSpreadCollisionMap.entrySet()) {
                BCConditionConfig conditionConfig = entry.getValue().getConditionConfig();
                BCResultConfig resultConfig = entry.getValue().getResultConfig();

                if (conditionConfig.getBlock().equals(IFTOperators.ALL_BLOCKS)) {
                    collisionMap.put(conditionConfig, resultConfig);
                    continue;
                }
                var block = BlockType.getAssetMap().getAsset(conditionConfig.getBlock());

                if (block != null) {
                    String blockId = block.getId();
                    if (blockId != null && !blockId.isEmpty()) {
                        collisionMap.put(conditionConfig, resultConfig);
                    }
                }
            }

            this.blockSpreadCollisionMap = collisionMap;
        }

        return this.blockSpreadCollisionMap;
    }

    @Nonnull
    public Object2ObjectMap<BCConditionConfig, BCResultConfig> getDemoteCollisionMap() {
        if (this.blockDemoteCollisionMap == null) {
            Object2ObjectMap<BCConditionConfig, BCResultConfig> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockDemoteCollisionMap.size());

            for(Map.Entry<String, BCConfigEntry> entry : this.rawBlockDemoteCollisionMap.entrySet()) {
                BCConditionConfig conditionConfig = entry.getValue().getConditionConfig();
                BCResultConfig resultConfig = entry.getValue().getResultConfig();

                if (conditionConfig.getBlock().equals(IFTOperators.ALL_BLOCKS)) {
                    collisionMap.put(conditionConfig, resultConfig);
                    continue;
                }

                var block = BlockType.getAssetMap().getAsset(conditionConfig.getBlock());

                if (block != null) {
                    String blockId = block.getId();
                    if (blockId != null && !blockId.isEmpty()) {
                        collisionMap.put(conditionConfig, resultConfig);
                    }
                }
            }

            this.blockDemoteCollisionMap = collisionMap;
        }

        return this.blockDemoteCollisionMap;
    }

    @Nonnull
    public Object2ObjectMap<BCConditionConfig, BCResultConfig> getCollisionMap(FlowPhase flowPhase) {
        if(flowPhase == FlowPhase.Spread) {
            return getSpreadCollisionMap();
        } else {
            return getDemoteCollisionMap();
        }
    }

    @Nullable
    public BCConfigEntry getCollision(FlowPhase flowPhase, String blockID, @Nullable String blockState) {

        for(Map.Entry<BCConditionConfig, BCResultConfig> entry : this.getCollisionMap(flowPhase).entrySet()) {
            if(entry.getKey().block.equals(blockID)) {
                if(blockState != null) {
                    if(entry.getKey().blockState.equals(blockState)) {
                        return new BCConfigEntry(entry.getKey(), entry.getValue());
                    }
                } else {
                    return new BCConfigEntry(entry.getKey(), entry.getValue());
                }
            }
        }
        return getCollisionForAllBlocks(flowPhase);
    }

    @Nullable
    public BCConfigEntry getCollisionForAllBlocks(FlowPhase flowPhase) {
        for(Map.Entry<BCConditionConfig, BCResultConfig> entry : this.getCollisionMap(flowPhase).entrySet()) {
            if(entry.getKey().block.equals(IFTOperators.ALL_BLOCKS)) {
                return new BCConfigEntry(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }


    static {
        CODEC = BuilderCodec.builder(BlockCollisionConfig.class, BlockCollisionConfig::new)
                .appendInherited(new KeyedCodec<>("Spread", new MapCodec<>(BCConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockSpreadCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockSpreadCollisionMap, o), (ticker) -> ticker.rawBlockSpreadCollisionMap, (ticker, parent) -> ticker.rawBlockSpreadCollisionMap = parent.rawBlockSpreadCollisionMap).documentation("Defines what happens when this fluid touches a block").add()
                .appendInherited(new KeyedCodec<>("Demote", new MapCodec<>(BCConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockDemoteCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockDemoteCollisionMap, o), (ticker) -> ticker.rawBlockDemoteCollisionMap, (ticker, parent) -> ticker.rawBlockDemoteCollisionMap = parent.rawBlockDemoteCollisionMap).documentation("Defines what happens when this fluid stops touching a specific block").add()
                .build();
    }
}