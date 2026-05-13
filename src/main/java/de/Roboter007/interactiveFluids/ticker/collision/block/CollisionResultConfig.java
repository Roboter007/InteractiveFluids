package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import de.Roboter007.interactiveFluids.ticker.collision.manager.PlaceableAsset;

public class CollisionResultConfig {
    public static final BuilderCodec<CollisionResultConfig> CODEC;

    protected long placeDelay = 0;
    protected PlaceableAsset placeableAsset = new PlaceableAsset();

    protected String soundEvent;
    protected int soundEventIndex = Integer.MIN_VALUE;

    protected boolean useBreakAnimation = false;



    public String getBlockToPlace() {
        return placeableAsset.getAssetID();
    }

    public long getPlaceDelay() {
        return placeDelay;
    }

    public String getBlockState() {
        return placeableAsset.getBlockState();
    }

    public byte getFluidLevel() {
        return placeableAsset.getFluidLevel();
    }

    public PlaceableAsset getPlaceableAsset() {
        return placeableAsset;
    }

    public int getSoundEventIndex() {
        if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
        }

        return this.soundEventIndex;
    }

    public boolean useBreakAnimation() {
        return this.useBreakAnimation;
    }

    static {
        CODEC = BuilderCodec.builder(CollisionResultConfig.class, CollisionResultConfig::new)
                .appendInherited(new KeyedCodec<>("Asset", PlaceableAsset.CODEC),(o, v) -> o.placeableAsset = v, (o) -> o.placeableAsset, (o, p) -> o.placeableAsset = p.placeableAsset).add()
                .appendInherited(new KeyedCodec<>("UseBreakAnimation", Codec.BOOLEAN), (o, v) -> o.useBreakAnimation = v, (o) -> o.useBreakAnimation, (o, p) -> o.useBreakAnimation = p.useBreakAnimation).documentation("If true, the block damage overlay is shown while the delayed conversion is pending").add()
                .appendInherited(new KeyedCodec<>("PlaceDelay", Codec.LONG), (o, v) -> o.placeDelay = v, (o) -> o.placeDelay, (o, p) -> o.placeDelay = p.placeDelay).documentation("If defined int will delay the block placement by a certain amount in ticks").add()
                .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                .build();

    }
}
