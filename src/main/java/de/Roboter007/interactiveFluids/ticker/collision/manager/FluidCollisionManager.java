package de.Roboter007.interactiveFluids.ticker.collision.manager;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FluidCollisionManager {

    private record PendingChange(int x, int y, int z, BlockType type, long executeAt) {}

    private static final ConcurrentLinkedQueue<PendingChange> queue = new ConcurrentLinkedQueue<>();

    private static final int MAX_CHANGES_PER_TICK = 50;

    public static void addDelayedCollision(int x, int y, int z, BlockType type, float delaySeconds) {
        long executeAt = System.currentTimeMillis() + (long) (delaySeconds * 1000);
        queue.add(new PendingChange(x, y, z, type, executeAt));
    }

    //ToDo: check if change is even relevant -> for example when the block changes
    //ToDo: add option for submerged blocks like grass if they should be affected or not
    public static void tick(World world) {
        if (queue.isEmpty()) return;

        long now = System.currentTimeMillis();
        int processed = 0;

        Iterator<PendingChange> iterator = queue.iterator();
        while (iterator.hasNext() && processed < MAX_CHANGES_PER_TICK) {
            PendingChange change = iterator.next();

            if (now >= change.executeAt()) {
                long chunkIndex = ChunkUtil.indexChunkFromBlock(change.x(), change.z());
                WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);

                if (chunk != null) {
                    chunk.setBlock(change.x(), change.y(), change.z(), change.type());
                    tickSurrounding(change, chunk);
                }

                iterator.remove();
                processed++;
            }
        }
    }

    private static void tickSurrounding(PendingChange change, WorldChunk chunk) {
        int worldX = change.x();
        int worldY = change.y();
        int worldZ = change.z();

        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                for (int x = -1; x <= 1; x++) {
                    int bx = worldX + x;
                    int by = worldY + y;
                    int bz = worldZ + z;
                    if (chunk != null) {
                        chunk.setTicking(bx, by, bz, true);
                    }
                }
            }
        }
    }
}