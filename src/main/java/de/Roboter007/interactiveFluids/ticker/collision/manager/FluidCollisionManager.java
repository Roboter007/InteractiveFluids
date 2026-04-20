package de.Roboter007.interactiveFluids.ticker.collision.manager;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import de.Roboter007.interactiveFluids.InteractiveFluidsPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

public final class FluidCollisionManager {

    private FluidCollisionManager() {}

    public static void addDelayedCollision(
            @Nonnull World world,
            int x, int y, int z,
            @Nonnull BlockType originalType,
            @Nullable BlockType resultType,
            int expectedFluidId,
            long delayedTicks,
            boolean showBreakAnimation,
            boolean revertBlock
    ) {
        world.execute(() -> {
            PendingChangeChunk changeChunk = getOrCreateChangeChunk(world, x, z);
            if (changeChunk == null) return;

            int idx = ChunkUtil.indexBlockInColumn(x, y, z);

            if (changeChunk.contains(idx)) return;

            PendingChange change = new PendingChange(
                    x, y, z,
                    world.getTick(), delayedTicks,
                    originalType, resultType,
                    expectedFluidId, showBreakAnimation,
                    revertBlock
            );
            changeChunk.put(change);
        });
    }

    public static void addDelayedRevert(
            @Nonnull World world,
            int x, int y, int z,
            @Nonnull BlockType expectedCurrentBlock,
            int expectedFluidId,
            long delayedTicks,
            boolean showBreakAnimation
    ) {
        world.execute(() -> {
            PendingChangeChunk changeChunk = getChangeChunk(world, x, z);
            if (changeChunk == null) return;

            int idx = ChunkUtil.indexBlockInColumn(x, y, z);
            if (changeChunk.contains(idx)) return;

            String originalTypeKey = changeChunk.getAndRemoveRevert(idx);
            if (originalTypeKey == null) return;

            BlockType currentBlock = world.getBlockType(x, y, z);
            if (currentBlock == null || !currentBlock.getId().equals(expectedCurrentBlock.getId())) {
                return;
            }

            int originalId = BlockType.getAssetMap().getIndex(originalTypeKey);
            BlockType originalType = BlockType.getAssetMap().getAsset(originalId);
            if (originalType == null) return;

            PendingChange revertChange = new PendingChange(
                    x, y, z,
                    world.getTick(), delayedTicks,
                    currentBlock,
                    originalType,
                    expectedFluidId,
                    showBreakAnimation,
                    false
            );
            changeChunk.put(revertChange);
        });
    }

    public static void tick(@Nonnull World world, long currentTick) {
        ComponentType<ChunkStore, PendingChangeChunk> componentType = getComponentType();
        if (componentType == null) return;

        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore.getStore().isShutdown()) return;

        chunkStore.getStore().forEachChunk(componentType, (archetypeChunk, commandBuffer) -> {
            for (int i = 0; i < archetypeChunk.size(); i++) {
                PendingChangeChunk changeChunk = archetypeChunk.getComponent(i, componentType);
                if (changeChunk == null || changeChunk.size() == 0) continue;

                Int2ObjectMap<PendingChange> map = changeChunk.rawMap();
                Iterator<PendingChange> it = map.values().iterator();

                while (it.hasNext()) {
                    PendingChange change = it.next();

                    if (change.createdAtTick > currentTick) {
                        change.clearBreakAnimation(world);
                        it.remove();
                        continue;
                    }

                    if (!change.isStillTheExpectedBlock(world)) {
                        change.clearBreakAnimation(world);
                        it.remove();
                        continue;
                    }

                    if (change.canPlaceBlock(currentTick)) {
                        if (change.placeBlock(world)) {
                            if (change.revertBlock) {
                                changeChunk.putRevert(change.blockInColumnIdx, change.originalTypeKey);
                            }
                            tickSurrounding(world, change.x, change.y, change.z);
                        }
                        it.remove();
                    } else if (change.showBreakAnimation) {
                        change.updateBreakAnimation(world, currentTick);
                    }
                }
            }
        });
    }

    public static boolean hasPendingChange(@Nonnull World world, int x, int y, int z) {
        PendingChangeChunk changeChunk = getChangeChunk(world, x, z);
        if (changeChunk == null) return false;
        return changeChunk.contains(ChunkUtil.indexBlockInColumn(x, y, z));
    }

    @Nullable
    public static PendingChange getPendingChange(@Nonnull World world, int x, int y, int z) {
        PendingChangeChunk changeChunk = getChangeChunk(world, x, z);
        if (changeChunk == null) return null;
        return changeChunk.get(ChunkUtil.indexBlockInColumn(x, y, z));
    }

    public static boolean hasRevertMarker(@Nonnull World world, int x, int y, int z) {
        PendingChangeChunk changeChunk = getChangeChunk(world, x, z);
        if (changeChunk == null) return false;
        return changeChunk.hasRevert(ChunkUtil.indexBlockInColumn(x, y, z));
    }

    public static void cancelPendingChange(@Nonnull World world, int x, int y, int z) {
        PendingChangeChunk changeChunk = getChangeChunk(world, x, z);
        if (changeChunk == null) return;

        int idx = ChunkUtil.indexBlockInColumn(x, y, z);
        PendingChange change = changeChunk.remove(idx);
        if (change != null) {
            change.clearBreakAnimation(world);
        }
    }

    public static void clear(@Nonnull World world) {
        ComponentType<ChunkStore, PendingChangeChunk> componentType = getComponentType();
        if (componentType == null) return;

        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore.getStore().isShutdown()) return;

        chunkStore.getStore().forEachChunk(componentType, (archetypeChunk, commandBuffer) -> {
            for (int i = 0; i < archetypeChunk.size(); i++) {
                PendingChangeChunk changeChunk = archetypeChunk.getComponent(i, componentType);
                if (changeChunk != null) changeChunk.clear();
            }
        });
    }

    @Nullable
    static PendingChangeChunk getChangeChunk(@Nonnull World world, int worldX, int worldZ) {
        ComponentType<ChunkStore, PendingChangeChunk> componentType = getComponentType();
        if (componentType == null) return null;

        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        if (chunkRef == null || !chunkRef.isValid()) return null;

        return chunkStore.getStore().getComponent(chunkRef, componentType);
    }

    @Nullable
    private static PendingChangeChunk getOrCreateChangeChunk(@Nonnull World world, int worldX, int worldZ) {
        ComponentType<ChunkStore, PendingChangeChunk> componentType = getComponentType();
        if (componentType == null) return null;

        ChunkStore chunkStore = world.getChunkStore();
        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
        Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
        if (chunkRef == null || !chunkRef.isValid()) return null;

        return chunkStore.getStore().ensureAndGetComponent(chunkRef, componentType);
    }

    @Nullable
    private static ComponentType<ChunkStore, PendingChangeChunk> getComponentType() {
        return InteractiveFluidsPlugin.getPendingChangeChunkType();
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
}