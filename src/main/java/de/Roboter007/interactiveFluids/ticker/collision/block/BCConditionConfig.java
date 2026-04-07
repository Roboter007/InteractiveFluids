package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public class BCConditionConfig {
    public static final BuilderCodec<BCConditionConfig> CODEC;

    protected String block;
    protected String blockState = "";


    public String getBlock() {
        return block;
    }


    public String getBlockState() {
        return blockState;
    }

    static {
        CODEC = BuilderCodec.builder(BCConditionConfig.class, BCConditionConfig::new)
                .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.block = v, (o) -> o.block, (o, p) -> o.block = p.block).documentation("The block to that gets replaced when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets replaced").add()
                .build();

    }
}
