package de.Roboter007.interactiveFluids.ticker.collision.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.codec.schema.metadata.ui.UIDisplayMode;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import de.Roboter007.interactiveFluids.ticker.collision.AssetType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CollisionConfigEntry {
    public static final BuilderCodec<CollisionConfigEntry> CODEC;

    private CollisionSourceConfig sourceConfig;
    private CollisionResultConfig resultConfig;

    private boolean placeFluid = false;

    protected long placeDelay = 0;
    protected boolean useBreakAnimation = false;

    protected String soundEvent;
    protected int soundEventIndex = Integer.MIN_VALUE;

    public CollisionConfigEntry(CollisionSourceConfig conditionConfig, CollisionResultConfig resultConfig) {
        this.sourceConfig = conditionConfig;
        this.resultConfig = resultConfig;
    }

    public CollisionConfigEntry() {
        this(new CollisionSourceConfig(), new CollisionResultConfig());
    }

    public CollisionSourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public CollisionResultConfig getResultConfig() {
        return resultConfig;
    }


    public long getPlaceDelay() {
        return placeDelay;
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

    public boolean placeFluid() {
        return placeFluid;
    }

    static {
        CODEC = BuilderCodec.builder(CollisionConfigEntry.class, CollisionConfigEntry::new)
                .appendInherited(new KeyedCodec<>("Source", CollisionSourceConfig.CODEC), (config, o) -> config.sourceConfig = o, (config) -> config.sourceConfig, (config, parent) -> config.sourceConfig = parent.sourceConfig).documentation("Expand to configure more. Defines the condition that has to exist in order for the conversion to happen.").add()
                .appendInherited(new KeyedCodec<>("Result", CollisionResultConfig.CODEC), (config, o) -> config.resultConfig = o, (config) -> config.resultConfig, (config, parent) -> config.resultConfig = parent.resultConfig).documentation("Expand to configure more. Defines the result that.").add()
                .appendInherited(new KeyedCodec<>("PlaceFluid", Codec.BOOLEAN), (o, v) -> o.placeFluid = v, (o) -> o.placeFluid, (o, p) -> o.placeFluid = p.placeFluid).documentation("Whether to still place the fluid on collision (only works for the following configuration: Source -> AssetType = Fluid and Result -> AssetType Block)").add()
                .appendInherited(new KeyedCodec<>("UseBreakAnimation", Codec.BOOLEAN), (o, v) -> o.useBreakAnimation = v, (o) -> o.useBreakAnimation, (o, p) -> o.useBreakAnimation = p.useBreakAnimation).documentation("If true, the block damage overlay is shown while the delayed conversion is pending").add()
                .appendInherited(new KeyedCodec<>("PlaceDelay", Codec.LONG), (o, v) -> o.placeDelay = v, (o) -> o.placeDelay, (o, p) -> o.placeDelay = p.placeDelay).documentation("If defined int will delay the block placement by a certain amount in ticks").add()
                .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                .build();
    }

    @Override
    public String toString() {
        return "CollisionConfigEntry{" +
                "sourceConfig=" + sourceConfig +
                ", resultConfig=" + resultConfig +
                ", placeFluid=" + placeFluid +
                ", placeDelay=" + placeDelay +
                ", useBreakAnimation=" + useBreakAnimation +
                ", soundEvent='" + soundEvent + '\'' +
                ", soundEventIndex=" + soundEventIndex +
                '}';
    }


}
