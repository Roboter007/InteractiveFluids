package de.Roboter007.interactiveFluids.ticker.api;

import com.hypixel.hytale.server.core.universe.world.World;
import de.Roboter007.interactiveFluids.InteractiveFluidsPlugin;
import de.Roboter007.interactiveFluids.ticker.collision.CollisionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CollisionHookRegistry {

    private static final Map<String, CollisionProperties> REGISTRY = new ConcurrentHashMap<>();

    public static void register(@NotNull String blockTypeId, @NotNull HashMap<String, String> properties, @NotNull ICollisionHook processor) {
        CollisionProperties previous = REGISTRY.put(blockTypeId, new CollisionProperties(properties, processor));
        if (previous != null) {
            InteractiveFluidsPlugin.get().getLogger().atWarning().log("[InteractiveFluids] CollisionHookRegistry for block '%s' got overridden.", blockTypeId);
        } else {
            InteractiveFluidsPlugin.get().getLogger().atInfo().log("[InteractiveFluids] CollisionHookRegistry registered for block '%s'.", blockTypeId);
        }
    }

    @Nullable
    public static CollisionProperties get(@NotNull String blockTypeId) {
        return REGISTRY.get(blockTypeId);
    }


    public static void hook(@NotNull World world, CollisionManager.CollisionInfo collisionInfo) {
        String name = collisionInfo.source().getName();
        if (name == null || name.isEmpty()) {
            return;
        }

        CollisionProperties processor = REGISTRY.get(name);
        if (processor == null) {
            return;
        }

        try {
            processor.collisionHook().onCollisionSuccess(world, collisionInfo, Collections.unmodifiableMap(processor.properties()));
        } catch (Exception e) {
            InteractiveFluidsPlugin.get().getLogger().atSevere().withCause(e).log("[InteractiveFluids] Error in CollisionHookRegistry for block '%s' at (%d, %d, %d).", name, collisionInfo.x(), collisionInfo.y(), collisionInfo.z());
        }
    }
}