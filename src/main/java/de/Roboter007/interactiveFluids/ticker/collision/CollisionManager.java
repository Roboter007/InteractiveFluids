package de.Roboter007.interactiveFluids.ticker.collision;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class CollisionManager {

    private static final int MAX_COLLISIONS_PER_TICK = 1000;

    private static final ConcurrentHashMap<String, List<PendingCollision>> COLLISION_QUEUE = new ConcurrentHashMap<>();

    public record CollisionInfo(int x, int y, int z, Asset source, Asset result, long delayTicks, boolean showBreakingAnimation) {
        public CollisionInfo(int x, int y, int z, Asset source, Asset result, long delayTicks) {
            this(x, y, z, source, result, delayTicks, false);
        }

    }

    public static final class PendingCollision {

        @NotNull
        private final CollisionManager.CollisionInfo collisionInfo;

        private final long createdAtTick;
        private final long executeAtTick;

        private float lastSentHealth = 1.0f;

        public PendingCollision(@NotNull CollisionManager.CollisionInfo collisionInfo, long createdAtTick) {
            this.collisionInfo = collisionInfo;
            this.createdAtTick = createdAtTick;
            this.executeAtTick = createdAtTick + collisionInfo.delayTicks;
        }

        private float progress(long currentTick) {
            long elapsed = Math.max(0L, currentTick - this.createdAtTick);
            return (float) elapsed / this.collisionInfo.delayTicks;
        }

        private boolean canBeExecuted(long currentTick) {
            return currentTick >= this.executeAtTick;
        }

        private void updateBreakAnimation(@Nonnull World world, long currentTick) {
            float health = 1.0F - progress(currentTick);
            float delta = health - this.lastSentHealth;
            if (!(Math.abs(delta) < 0.01f)) {
                world.getNotificationHandler().updateBlockDamage(this.collisionInfo.x, this.collisionInfo.y, this.collisionInfo.z, health, delta);
                this.lastSentHealth = health;
            }
        }

        private void clearBreakAnimation(@Nonnull World world) {
            if (this.lastSentHealth < 0.999f) {
                world.getNotificationHandler().updateBlockDamage(this.collisionInfo.x, this.collisionInfo.y, this.collisionInfo.z, 1.0f, 0.0f);
            }
            this.lastSentHealth = 1.0f;
        }

        public boolean execute(@NotNull World world) {
            int x = collisionInfo.x();
            int y = collisionInfo.y();
            int z = collisionInfo.z();


            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);

            if (chunk != null) {
                if (collisionInfo.result().isBlock()) {
                    int resultId = collisionInfo.result().getId();
                    int rotation = world.getBlockRotationIndex(x, y, z);

                    this.clearBreakAnimation(world);

                    return chunk.setBlock(x, y, z, resultId, collisionInfo.result().getBlockType(), rotation, 0, 0);
                } else if (collisionInfo.result().isFluid()) {
                    this.clearBreakAnimation(world);

                    Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReferenceAtBlock(x, y, z);

                    if (sectionRef == null) {
                        return false;
                    }

                    Store<ChunkStore> store = sectionRef.getStore();
                    FluidSection fluidSection = store.ensureAndGetComponent(sectionRef, FluidSection.getComponentType());

                    Fluid fluid = collisionInfo.result().getFluid();
                    byte fluidLevel = fluidSection.getFluidLevel(x, y, z);

                    if (fluid == null || collisionInfo.result().getFluidLevel(fluidLevel) == Byte.MIN_VALUE) {
                        return false;
                    }

                    boolean placed = fluidSection.setFluid(x, y, z, fluid, collisionInfo.result().getFluidLevel(fluidLevel));

                    if (placed) {
                        chunk.setBlock(x, y, z, BlockType.EMPTY_ID, BlockType.EMPTY, 0, 0, 0);
                        return true;
                    } else {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }

        public boolean isStillExpectedAsset(@NotNull World world) {
            int x = collisionInfo.x();
            int y = collisionInfo.y();
            int z = collisionInfo.z();

            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
            if (chunk == null) {
                return false;
            }

            if(this.collisionInfo.source().isBlock()) {
                BlockType block = chunk.getBlockType(x, y, z);
                return block != null && block.getId().equals(collisionInfo.source().getName());
            } else if (collisionInfo.source().isFluid()) {
                int fluidId = world.getFluidId(x, y, z);
                return fluidId > 0 && fluidId == collisionInfo.source().getId();
            } else {
                throw new RuntimeException("Invalid State for Source Type!");
            }
        }

    }


    public static void addDelayedCollision(@Nonnull World world, CollisionManager.CollisionInfo collisionInfo) {
        String worldID = world.getWorldConfig().getUuid().toString();
        CollisionManager.PendingCollision change = new CollisionManager.PendingCollision(collisionInfo, world.getTick());

        COLLISION_QUEUE.computeIfAbsent(worldID, _ -> Collections.synchronizedList(new ArrayList<>())).add(change);
    }


    private static int getCollisionLimit(World world) {
        HistoricMetric metric = world.getBufferedTickLengthMetricSet();
        long[] periods = metric.getPeriodsNanos();

        if (periods.length == 0) {
            return MAX_COLLISIONS_PER_TICK;
        }

        int periodIndex = 0;
        double avgTickMs = metric.getAverage(periodIndex) / 1_000_000.0;

        if (avgTickMs < 40.0) {
            return MAX_COLLISIONS_PER_TICK;
        }
        if (avgTickMs < 50.0) {
            return MAX_COLLISIONS_PER_TICK / 2;
        }
        if (avgTickMs < 75.0) {
            return MAX_COLLISIONS_PER_TICK / 4;
        }
        return MAX_COLLISIONS_PER_TICK / 10;
    }

    public static void tick(@Nonnull World world, long currentTick) {
        List<PendingCollision> queue = COLLISION_QUEUE.get(worldKey(world));
        if (queue != null && !queue.isEmpty()) {
            synchronized (queue) {
                List<PendingCollision> currentChanges = new ArrayList<>(queue);

                int maxCollisions = getCollisionLimit(world);

                for (int i = 0; i < maxCollisions; i++) {
                    if(i >= currentChanges.size()) {
                        break;
                    }

                    PendingCollision change = currentChanges.get(i);
                    if (change.isStillExpectedAsset(world)) {
                        if (change.canBeExecuted(currentTick)) {
                            if (change.execute(world)) {
                                tickSurrounding(world, change.collisionInfo.x, change.collisionInfo.y, change.collisionInfo.z);
                            }
                            queue.remove(change);
                        } else if (change.collisionInfo.showBreakingAnimation) {
                            change.updateBreakAnimation(world, currentTick);
                        }
                    } else {
                        queue.remove(change);
                    }
                }

            }
        }
    }

    private static void tickSurrounding(@Nonnull World world, int blockX, int blockY, int blockZ) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int x = blockX + dx;
                    int y = blockY + dy;
                    int z = blockZ + dz;
                    WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(x, z));
                    if (chunk != null) {
                        chunk.setTicking(x, y, z, true);
                    }
                }
            }
        }
    }

    @Nonnull
    private static String worldKey(@Nonnull World world) {
        UUID uuid = world.getWorldConfig().getUuid();
        return uuid.toString();
    }

    @Nullable
    private static World worldFromKey(@Nonnull String worldKey) {
        return Universe.get().getWorld(UUID.fromString(worldKey));
    }
}
