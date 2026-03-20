package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

public class BCConditionConfig {
    public static final BuilderCodec<BCConditionConfig> CODEC;

    protected String blockToPlace;
    protected String blockState = "";
    protected int blockToPlaceIndex = Integer.MIN_VALUE;

    public int getBlockToPlaceIndex() {
        if (this.blockToPlaceIndex == Integer.MIN_VALUE && this.blockToPlace != null) {
            this.blockToPlaceIndex = BlockType.getBlockIdOrUnknown(this.blockToPlace, "Unknown block type %s", this.blockToPlace);
        }

        return this.blockToPlaceIndex;
    }



    public String getBlockToPlace() {
        return blockToPlace;
    }


    public String getBlockState() {
        return blockState;
    }

    static {
        CODEC = BuilderCodec.builder(BCConditionConfig.class, BCConditionConfig::new)
                .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to that gets replaced when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets replaced").add()
                .build();

    }
}
