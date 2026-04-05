package de.Roboter007.interactiveFluids;

import com.hypixel.hytale.builtin.buildertools.utils.Material;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import de.Roboter007.interactiveFluids.ticker.InteractiveFluidTicker;
import de.Roboter007.interactiveFluids.ticker.collision.manager.FluidCollisionManager;

import java.util.concurrent.TimeUnit;

public class InteractiveFluidsPlugin extends JavaPlugin {
    protected static InteractiveFluidsPlugin instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public InteractiveFluidsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Surprising Hello by the Interactive Fluids!!! | %s - Version: %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Loading Interactive Fluids...");
        FluidTicker.CODEC.register("Interactive Fluid Ticker", InteractiveFluidTicker.class, InteractiveFluidTicker.CODEC);
        instance = this;
    }

    @Override
    protected void start() {
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            Universe universe = Universe.get();
            if (universe != null) {
                for (World world : universe.getWorlds().values()) {
                    world.execute(() -> FluidCollisionManager.tick(world, world.getTick()));
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS);

    }

    public static InteractiveFluidsPlugin get() {
        return instance;
    }
}
