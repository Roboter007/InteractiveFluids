package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

public class BlockCollisionConfigEntry {
    public static final BuilderCodec<BlockCollisionConfigEntry> CODEC;
    protected String blockToPlace;
    protected String blockState = "";
    protected int blockToPlaceIndex = Integer.MIN_VALUE;
    protected String soundEvent;
    protected int soundEventIndex = Integer.MIN_VALUE;

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

    public int getSoundEventIndex() {
        if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
        }

        return this.soundEventIndex;
    }

    static {
        CODEC = BuilderCodec.builder(BlockCollisionConfigEntry.class, BlockCollisionConfigEntry::new)
                .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to place when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets placed").add()
                .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                .build();

    }
}
