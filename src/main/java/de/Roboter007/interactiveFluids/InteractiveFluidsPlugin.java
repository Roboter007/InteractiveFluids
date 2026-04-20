package de.Roboter007.interactiveFluids;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import de.Roboter007.interactiveFluids.ticker.InteractiveFluidTicker;
import de.Roboter007.interactiveFluids.ticker.collision.manager.FluidCollisionManager;
import de.Roboter007.interactiveFluids.ticker.collision.manager.PendingChangeChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

public class InteractiveFluidsPlugin extends JavaPlugin {

    protected static InteractiveFluidsPlugin instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private static ComponentType<ChunkStore, PendingChangeChunk> pendingChangeChunkType;

    public InteractiveFluidsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log(
                "Surprising Hello by the Interactive Fluids!!! | %s - Version: %s",
                this.getName(), this.getManifest().getVersion().toString()
        );
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Loading Interactive Fluids...");

        FluidTicker.CODEC.register(
                "Interactive Fluid Ticker",
                InteractiveFluidTicker.class,
                InteractiveFluidTicker.CODEC
        );

        ComponentRegistryProxy<ChunkStore> chunkStoreRegistry = this.getChunkStoreRegistry();
        pendingChangeChunkType = chunkStoreRegistry.registerComponent(
                PendingChangeChunk.class,
                "InteractiveFluids_PendingChanges",
                PendingChangeChunk.CODEC
        );

        chunkStoreRegistry.registerSystem(new EnsurePendingChangeChunkSystem(pendingChangeChunkType));

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

    @Nullable
    public static ComponentType<ChunkStore, PendingChangeChunk> getPendingChangeChunkType() {
        return pendingChangeChunkType;
    }

    private static class EnsurePendingChangeChunkSystem extends HolderSystem<ChunkStore> {

        @Nonnull
        private final ComponentType<ChunkStore, PendingChangeChunk> componentType;

        EnsurePendingChangeChunkSystem(@Nonnull ComponentType<ChunkStore, PendingChangeChunk> componentType) {
            this.componentType = componentType;
        }

        @Override
        public void onEntityAdd(
                @Nonnull Holder<ChunkStore> holder,
                @Nonnull AddReason reason,
                @Nonnull Store<ChunkStore> store
        ) {
            holder.ensureComponent(this.componentType);
        }

        @Override
        public void onEntityRemoved(@Nonnull Holder<ChunkStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store) {
        }

        @Nonnull
        @Override
        public Query<ChunkStore> getQuery() {
            return WorldChunk.getComponentType();
        }
    }
}