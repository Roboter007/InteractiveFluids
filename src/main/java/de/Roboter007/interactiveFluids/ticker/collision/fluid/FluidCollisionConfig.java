package de.Roboter007.interactiveFluids.ticker.collision.fluid;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;

public class FluidCollisionConfig {
    public static final BuilderCodec<FluidCollisionConfig> CODEC;
    private String blockToPlace;
    private int blockToPlaceIndex = Integer.MIN_VALUE;
    public boolean placeFluid = false;
    private String soundEvent;
    private int soundEventIndex = Integer.MIN_VALUE;

    public int getBlockToPlaceIndex() {
        if (this.blockToPlaceIndex == Integer.MIN_VALUE && this.blockToPlace != null) {
            this.blockToPlaceIndex = BlockType.getBlockIdOrUnknown(this.blockToPlace, "Unknown block type %s", this.blockToPlace);
        }

        return this.blockToPlaceIndex;
    }

    public int getSoundEventIndex() {
        if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
        }

        return this.soundEventIndex;
    }

    static {
        CODEC = (BuilderCodec.builder(FluidCollisionConfig.class, FluidCollisionConfig::new)
                .appendInherited(new KeyedCodec<>("BlockToPlace", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to place when a collision occurs").add())
                .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                .appendInherited(new KeyedCodec<>("PlaceFluid", Codec.BOOLEAN), (o, v) -> o.placeFluid = v, (o) -> o.placeFluid, (o, p) -> o.placeFluid = p.placeFluid).documentation("Whether to still place the fluid on collision").add()
                .build();
    }
}