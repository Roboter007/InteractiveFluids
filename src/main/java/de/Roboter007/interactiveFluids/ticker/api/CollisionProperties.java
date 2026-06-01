package de.Roboter007.interactiveFluids.ticker.api;

import java.util.HashMap;

public record CollisionProperties(HashMap<String, String> properties, ICollisionHook collisionHook) {
}
