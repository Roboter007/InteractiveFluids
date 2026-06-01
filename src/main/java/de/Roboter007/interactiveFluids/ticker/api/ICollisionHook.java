package de.Roboter007.interactiveFluids.ticker.api;
 
import com.hypixel.hytale.server.core.universe.world.World;
import de.Roboter007.interactiveFluids.ticker.collision.CollisionManager;

import javax.annotation.Nonnull;
import java.util.Map;

@FunctionalInterface
public interface ICollisionHook {

    void onCollisionSuccess(@Nonnull World world, @Nonnull CollisionManager.CollisionInfo collisionInfo, @Nonnull Map<String, String> properties);
}
 