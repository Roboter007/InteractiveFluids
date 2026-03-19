package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class BCConfigEntry {
    public static final BuilderCodec<BCConfigEntry> CODEC;

    private BCConditionConfig conditionConfig;
    private BCResultConfig resultConfig;


    public BCConfigEntry(BCConditionConfig conditionConfig, BCResultConfig resultConfig) {
        this.conditionConfig = conditionConfig;
        this.resultConfig = resultConfig;
    }

    public BCConfigEntry() {
        this(new BCConditionConfig(), new BCResultConfig());
    }

    public BCConditionConfig getConditionConfig() {
        return conditionConfig;
    }

    public BCResultConfig getResultConfig() {
        return resultConfig;
    }

    static {
        CODEC = BuilderCodec.builder(BCConfigEntry.class, BCConfigEntry::new)
                .appendInherited(new KeyedCodec<>("Condition", BCConditionConfig.CODEC), (config, o) -> config.conditionConfig = o, (config) -> config.conditionConfig, (config, parent) -> config.conditionConfig = parent.conditionConfig).documentation("Expand to configure more. Defines the condition that has to exist in order for the conversion to happen.").add()
                .appendInherited(new KeyedCodec<>("Result", BCResultConfig.CODEC), (config, o) -> config.resultConfig = o, (config) -> config.resultConfig, (config, parent) -> config.resultConfig = parent.resultConfig).documentation("Expand to configure more. Defines the result that.").add()
                .build();
    }
}
