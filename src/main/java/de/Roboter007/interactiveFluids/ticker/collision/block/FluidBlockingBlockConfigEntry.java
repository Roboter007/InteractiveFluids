package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FluidBlockingBlockConfigEntry {

    public static final BuilderCodec<FluidBlockingBlockConfigEntry> CODEC;

    public boolean blocksFluid = false;
    public boolean keepVanillaBehavior = true;

    static {
        CODEC = BuilderCodec.builder(FluidBlockingBlockConfigEntry.class, FluidBlockingBlockConfigEntry::new)
                .appendInherited(new KeyedCodec<>("BlocksFluid", Codec.BOOLEAN), (o, v) -> o.blocksFluid = v, (o) -> o.blocksFluid, (o, p) -> o.blocksFluid = p.blocksFluid).documentation("The block to that gets replaced when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("KeepVanillaBehavior", Codec.BOOLEAN), (o, v) -> o.keepVanillaBehavior = v, (o) -> o.keepVanillaBehavior, (o, p) -> o.keepVanillaBehavior = p.keepVanillaBehavior).documentation("The block state of the block that gets replaced").add()
                .build();

    }
}
