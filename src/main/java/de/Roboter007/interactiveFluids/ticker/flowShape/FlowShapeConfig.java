package de.Roboter007.interactiveFluids.ticker.flowShape;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;

import static de.Roboter007.interactiveFluids.ticker.InteractiveFluidTicker.shapeGenConfigDefault;

public class FlowShapeConfig {
    public static final BuilderCodec<FlowShapeConfig> CODEC;

    private FlowShape flowShape = FlowShape.Diamond;
    private int[] shapeGenConfig = shapeGenConfigDefault();

    public FlowShape getFlowShape() {
        return flowShape;
    }

    public int[] getShapeGenConfig() {
        return shapeGenConfig;
    }

    static {
        CODEC = BuilderCodec.builder(FlowShapeConfig.class, FlowShapeConfig::new)
                .appendInherited(new KeyedCodec<>("FlowShape", new EnumCodec<>(FlowShape.class)), (ticker, o) -> ticker.flowShape = o, (ticker) -> ticker.flowShape, (ticker, parent) -> ticker.flowShape = parent.flowShape).add()
                .appendInherited(new KeyedCodec<>("ShapeSizeOptions", Codec.INT_ARRAY), (ticker, o) -> ticker.shapeGenConfig = o, (ticker) -> ticker.shapeGenConfig, (ticker, parent) -> ticker.shapeGenConfig = parent.shapeGenConfig).add()
                .build();
    }
}
