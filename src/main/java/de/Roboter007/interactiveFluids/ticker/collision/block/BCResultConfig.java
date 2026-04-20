package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

public class BCResultConfig {
    public static final BuilderCodec<BCResultConfig> CODEC;

    protected long blockPlaceDelay = 0;
    protected String blockToPlace;
    protected String blockState = "";
    protected int blockToPlaceIndex = Integer.MIN_VALUE;
    protected String soundEvent;
    protected int soundEventIndex = Integer.MIN_VALUE;
    protected boolean revertBlock = false;
    protected boolean useBreakAnimation = false;

    public int getBlockToPlaceIndex() {
        if (this.blockToPlaceIndex == Integer.MIN_VALUE && this.blockToPlace != null) {
            this.blockToPlaceIndex = BlockType.getBlockIdOrUnknown(this.blockToPlace, "Unknown block type %s", this.blockToPlace);
        }
        return this.blockToPlaceIndex;
    }

    public String getBlockToPlace() {
        return blockToPlace;
    }

    public long getBlockPlaceDelay() {
        return blockPlaceDelay;
    }

    public String getBlockState() {
        return blockState;
    }

    public int getSoundEventIndex() {
        if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
        }
        return this.soundEventIndex;
    }

    public boolean revertBlock() {
        return this.revertBlock;
    }

    public boolean useBreakAnimation() {
        return this.useBreakAnimation;
    }

    static {
        CODEC = BuilderCodec.builder(BCResultConfig.class, BCResultConfig::new)
                .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to place when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets placed").add()
                .appendInherited(new KeyedCodec<>("BlockPlaceDelay", Codec.LONG), (o, v) -> o.blockPlaceDelay = v, (o) -> o.blockPlaceDelay, (o, p) -> o.blockPlaceDelay = p.blockPlaceDelay).documentation("If defined it will delay the block placement by a certain amount in ticks").add()
                .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                .appendInherited(new KeyedCodec<>("RevertBlock", Codec.BOOLEAN), (o, v) -> o.revertBlock = v, (o) -> o.revertBlock, (o, p) -> o.revertBlock = p.revertBlock).documentation("If true, it will revert the current result block back. This happens in the opposite fluid phase").add()
                .appendInherited(new KeyedCodec<>("UseBreakAnimation", Codec.BOOLEAN), (o, v) -> o.useBreakAnimation = v, (o) -> o.useBreakAnimation, (o, p) -> o.useBreakAnimation = p.useBreakAnimation).documentation("If true, the block damage overlay is shown while the delayed conversion is pending").add()
                .build();
    }
}