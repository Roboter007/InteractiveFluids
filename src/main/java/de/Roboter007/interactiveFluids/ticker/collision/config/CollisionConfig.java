package de.Roboter007.interactiveFluids.ticker.collision.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import de.Roboter007.interactiveFluids.ticker.collision.AssetType;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowPhase;
import de.Roboter007.interactiveFluids.ticker.utils.IFOperators;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

//ToDo: boolean value that defines if a block gets turned back in its original block when the fluid has demoted & save them in a list or something like that
public class CollisionConfig {
    public static final BuilderCodec<CollisionConfig> CODEC;

    protected Map<String, CollisionConfigEntry> rawBlockSpreadCollisionMap = Collections.emptyMap();
    protected Map<String, CollisionConfigEntry> rawBlockDemoteCollisionMap = Collections.emptyMap();

    @Nullable
    protected transient Object2ObjectOpenHashMap<String, CollisionConfigEntry> spreadCollisionMap = null;
    @Nullable
    protected transient Object2ObjectOpenHashMap<String, CollisionConfigEntry> demoteCollisionMap = null;

    @Nonnull
    public Object2ObjectOpenHashMap<String, CollisionConfigEntry> getCollision(FlowPhase flowPhase) {
        if (this.spreadCollisionMap == null || this.demoteCollisionMap == null) {
            Object2ObjectOpenHashMap<String, CollisionConfigEntry> nameToCollisionMap;
            Map<String, CollisionConfigEntry> rawCollisionMap;

            if(flowPhase == FlowPhase.Spread) {
                rawCollisionMap = this.rawBlockSpreadCollisionMap;
                nameToCollisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockSpreadCollisionMap.size());
            } else {
                rawCollisionMap = this.rawBlockDemoteCollisionMap;
                nameToCollisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockDemoteCollisionMap.size());
            }

            for(Map.Entry<String, CollisionConfigEntry> entry : rawCollisionMap.entrySet()) {
                CollisionConfigEntry configEntry = entry.getValue();
                CollisionSourceConfig sourceConfig = configEntry.getSourceConfig();
                String name = sourceConfig.getAssetID();

                if (name.equals(IFOperators.ANYTHING) || name.equals(IFOperators.ALL_FROM_CURRENT_ASSET_TYPE)) {
                    nameToCollisionMap.put(name, configEntry);
                    continue;
                }

                if(sourceConfig.getAssetType() == AssetType.Block) {
                    BlockType block = BlockType.getAssetMap().getAsset(name);

                    if (block != null) {
                        String blockId = block.getId();
                        if (blockId != null && !blockId.isEmpty()) {
                            nameToCollisionMap.put(name, configEntry);
                        }
                    }
                } else {
                    Fluid fluid = Fluid.getAssetMap().getAsset(name);
                    if (fluid != null) {
                        String fluidId = fluid.getId();
                        if (fluidId != null && !fluidId.isEmpty()) {
                            nameToCollisionMap.put(name, configEntry);
                        }
                    }
                }
            }

            if(flowPhase == FlowPhase.Spread) {
                spreadCollisionMap = nameToCollisionMap;
            } else {
                demoteCollisionMap = nameToCollisionMap;
            }
        }
        if(flowPhase == FlowPhase.Spread) {
            return this.spreadCollisionMap;
        } else {
            return this.demoteCollisionMap;
        }
    }

    @Nonnull
    public Object2ObjectMap<CollisionSourceConfig, CollisionResultConfig> getCollisionMap(FlowPhase flowPhase) {
        Object2ObjectMap<String, CollisionConfigEntry> entryMap = getCollision(flowPhase);
        Object2ObjectMap<CollisionSourceConfig, CollisionResultConfig> sourceToResultMap = new Object2ObjectOpenHashMap<>(entryMap.size());
        for(Map.Entry<String, CollisionConfigEntry> entry : entryMap.entrySet()) {
            sourceToResultMap.put(entry.getValue().getSourceConfig(), entry.getValue().getResultConfig());
        }
        return sourceToResultMap;
    }


    @Nullable
    public CollisionConfigEntry getBlockBlockCollision(FlowPhase flowPhase, String assetId, @Nullable String blockState) {
        for (Map.Entry<CollisionSourceConfig, CollisionResultConfig> entry : this.getCollisionMap(flowPhase).entrySet()) {
            CollisionSourceConfig condition = entry.getKey();
            CollisionResultConfig result = entry.getValue();

            if (condition == null || result == null) {
                continue;
            }

            if(condition.getAssetType() == AssetType.Block && result.getAssetType() == AssetType.Block) {

                boolean isWildcard = condition.assetID.equals(IFOperators.ANYTHING);
                if (!isWildcard && !condition.assetID.equals(assetId)) {
                    continue;
                }

                String requiredState = condition.blockState;
                if (requiredState == null || requiredState.isEmpty()) {
                    return new CollisionConfigEntry(condition, entry.getValue());
                }

                if (requiredState.equals(blockState)) {
                    return new CollisionConfigEntry(condition, entry.getValue());
                }
            }
        }
        return null;
    }




    static {
        CODEC = BuilderCodec.builder(CollisionConfig.class, CollisionConfig::new)
                .appendInherited(new KeyedCodec<>("Spread", new MapCodec<>(CollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockSpreadCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockSpreadCollisionMap, o), (ticker) -> ticker.rawBlockSpreadCollisionMap, (ticker, parent) -> ticker.rawBlockSpreadCollisionMap = parent.rawBlockSpreadCollisionMap).documentation("Defines what happens when this fluid touches a block").add()
                .appendInherited(new KeyedCodec<>("Demote", new MapCodec<>(CollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockDemoteCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockDemoteCollisionMap, o), (ticker) -> ticker.rawBlockDemoteCollisionMap, (ticker, parent) -> ticker.rawBlockDemoteCollisionMap = parent.rawBlockDemoteCollisionMap).documentation("Defines what happens when this fluid stops touching a specific block").add()
                .build();
    }
}