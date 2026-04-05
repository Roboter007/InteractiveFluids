package de.Roboter007.interactiveFluids.ticker.fluidblocking;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class FluidBlockingBlockConfigEntry {

    public static final BuilderCodec<FluidBlockingBlockConfigEntry> CODEC;

    public boolean blocksFluid = false;
    public boolean keepVanillaBehavior = true;

    static {
        CODEC = BuilderCodec.builder(FluidBlockingBlockConfigEntry.class, FluidBlockingBlockConfigEntry::new)
                .appendInherited(new KeyedCodec<>("BlocksFluid", Codec.BOOLEAN), (o, v) -> o.blocksFluid = v, (o) -> o.blocksFluid, (o, p) -> o.blocksFluid = p.blocksFluid).documentation("Overrides the fluid blocking behavior of the specified block").add()
                .appendInherited(new KeyedCodec<>("KeepVanillaBehavior", Codec.BOOLEAN), (o, v) -> o.keepVanillaBehavior = v, (o) -> o.keepVanillaBehavior, (o, p) -> o.keepVanillaBehavior = p.keepVanillaBehavior).documentation("When BlocksFluid is false, the vanilla Hytale behavior will be used to check if it should block the fluid").add()
                .build();

    }
}
