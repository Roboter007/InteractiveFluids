package de.Roboter007.interactiveFluids.ticker.collision.manager;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import de.Roboter007.interactiveFluids.InteractiveFluidsPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.desktop.QuitEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public final class FluidCollisionManager {

    private FluidCollisionManager() {}

    private static final int MAX_CHANGES_PER_TICK = 50;
    private static final ConcurrentHashMap<String, List<PendingChange>> QUEUES = new ConcurrentHashMap<>();

    public record SimplifiedBlock(int id, AssetExtraInfo.Data data) {
        public static SimplifiedBlock ofType(BlockType blockType) {
            return new SimplifiedBlock(BlockType.getAssetMap().getIndex(blockType.getId()), blockType.getData());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SimplifiedBlock(int id1, AssetExtraInfo.Data data1))) return false;
            return id == id1 && Objects.equals(data, data1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, data);
        }
    }

    private static final class PendingChange {
        private final World world;
        private final int x;
        private final int y;
        private final int z;
        private final SimplifiedBlock expectedBlock;
        @Nullable
        private final BlockType resultType;
        private final int expectedFluidId;

        private final boolean showBreakAnimation;
        private final long delayedTicks;

        private final long createdAtTick;
        private final long executeAtTick;

        private float lastSentHealth = 1.0f;

        private PendingChange(World world, int x, int y, int z, long delayedTicks, SimplifiedBlock expectedBlock, @Nullable BlockType resultType, int expectedFluidId, boolean showBreakAnimation) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.delayedTicks = delayedTicks;
            this.expectedBlock = expectedBlock;
            this.resultType = resultType;
            this.expectedFluidId = expectedFluidId;
            this.showBreakAnimation = showBreakAnimation;

            this.createdAtTick = world.getTick();
            this.executeAtTick = this.createdAtTick + delayedTicks;
        }

        private float progress(long currentTick) {
            long elapsed = Math.max(0L, currentTick - this.createdAtTick);
            return (float) elapsed / this.delayedTicks;
        }

        private boolean canPlaceBlock(long currentTick) {
            return currentTick >= this.executeAtTick;
        }

        private void updateBreakAnimation(@Nonnull World world, long currentTick) {
            float health = 1.0F - progress(currentTick);
            float delta = health - this.lastSentHealth;
            if (!(Math.abs(delta) < 0.01f)) {
                world.getNotificationHandler().updateBlockDamage(this.x, this.y, this.z, health, delta);
                this.lastSentHealth = health;
            }
        }

        private void clearBreakAnimation(@Nonnull World world) {
            if (this.lastSentHealth < 0.999f) {
                world.getNotificationHandler().updateBlockDamage(this.x, this.y, this.z, 1.0f, 0.0f);
            }
            this.lastSentHealth = 1.0f;
        }

        public boolean placeBlock(@Nonnull World world) {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(this.x, this.z);
            WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);

            if (chunk != null) {
                if (this.resultType != null) {
                    int resultId = BlockType.getAssetMap().getIndex(this.resultType.getId());

                    int rotation = world.getBlockRotationIndex(this.x, this.y, this.z);
                    this.clearBreakAnimation(world);
                    return chunk.setBlock(this.x, this.y, this.z, resultId, this.resultType, rotation, 0, 0);
                } else {
                    chunk.setBlock(this.x, this.y, this.z, 0);
                    this.clearBreakAnimation(world);
                }
                return true;
            } else {
                return false;
            }
        }

        public boolean isStillTheExpectedBlock() {
            BlockType blockType = this.world.getBlockType(this.x, this.y, this.z);
            if(blockType != null) {
                return this.expectedBlock.equals(SimplifiedBlock.ofType(blockType));
            } else {
                return false;
            }
        }

    }


    public static void addDelayedCollision(@Nonnull World world, int x, int y, int z, @Nonnull SimplifiedBlock expectedBlock, @Nullable BlockType resultType, int expectedFluidId, long delayedTicks, boolean showBreakAnimation) {
        String worldKey = worldKey(world);
        PendingChange change = new PendingChange(world, x, y, z, delayedTicks, expectedBlock, resultType, expectedFluidId, showBreakAnimation);

        QUEUES.computeIfAbsent(worldKey, _ -> Collections.synchronizedList(new ArrayList<>())).add(change);
    }

    public static void tick(@Nonnull World world, long currentTick) {
        List<PendingChange> queue = QUEUES.get(worldKey(world));
        if (queue != null && !queue.isEmpty()) {
            InteractiveFluidsPlugin.get().getLogger().atInfo().log("Queue Size: " + queue.size());
            synchronized (queue) {
                Iterator<PendingChange> it = queue.iterator();
                while (it.hasNext()) {
                    PendingChange change = it.next();

                    if(change.isStillTheExpectedBlock()) {
                        if (change.canPlaceBlock(currentTick)) {
                            if (change.placeBlock(world)) {
                                tickSurrounding(world, change.x, change.y, change.z);
                            }
                            it.remove();
                        } else if (change.showBreakAnimation) {
                            change.updateBreakAnimation(world, currentTick);
                        }
                    } else {
                        it.remove();
                    }
                }
            }
        }
    }

    public static void clear(@Nonnull World world) {
        QUEUES.remove(worldKey(world));
    }


    private static boolean isSubmerged(@Nonnull World world, int x, int y, int z) {
        int fluidId = world.getFluidId(x, y, z);
        return fluidId != 0 && fluidId != Integer.MIN_VALUE;
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
