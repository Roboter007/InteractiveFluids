package de.Roboter007.interactiveFluids;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import de.Roboter007.interactiveFluids.system.InteractiveFluidTicker;

public class InteractiveFluidsPlugin extends JavaPlugin {

    protected static InteractiveFluidsPlugin instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public InteractiveFluidsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Surprising Hello by the Interactive Fluids!!! | %s  - Version: %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Loading Interactive Fluids...");
        FluidTicker.CODEC.register("Interactive Fluid Ticker", InteractiveFluidTicker.class, InteractiveFluidTicker.CODEC);

        instance = this;
    }


    @Override
    protected void start() {
    }

    public static InteractiveFluidsPlugin get() {
        return instance;
    }
}
