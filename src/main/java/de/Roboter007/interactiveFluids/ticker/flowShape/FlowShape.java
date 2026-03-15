package de.Roboter007.interactiveFluids.ticker.flowShape;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum FlowShape {
    Cube(cube()),
    Cuboid(cuboid()),
    Sphere(sphere()),
    Diamond(diamond()),
    Flat_Square(flatSquare()),
    Flat_Rectangle(flatRectangle()),
    Flat_Circle(flatCircle()),
    Flat_Diamond(flatDiamond());

    private static Function<int[], List<Vector3i>> cube() {
        return array -> {
            List<Vector3i> blocks = new ArrayList<>();
            int half = array[0] / 2;

            for (int x = -half; x <= half; x++) {
                for (int y = -half; y <= half; y++) {
                    for (int z = -half; z <= half; z++) {
                        blocks.add(new Vector3i(x, y, z));
                    }
                }
            }
            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> cuboid() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int width  = shapeGenConfig[0];
            int height = shapeGenConfig[1];
            int depth  = shapeGenConfig[2];

            int halfX = width  / 2;
            int halfY = height / 2;
            int halfZ = depth  / 2;

            for (int x = -halfX; x <= halfX; x++) {
                for (int y = -halfY; y <= halfY; y++) {
                    for (int z = -halfZ; z <= halfZ; z++) {
                        blocks.add(new Vector3i(x, y, z));
                    }
                }
            }

            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> sphere() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();
            int radius = shapeGenConfig[0];

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (x * x + y * y + z * z <= radius * radius) {
                            blocks.add(new Vector3i(x, y, z));
                        }

                    }
                }
            }
            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> diamond() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int radius = shapeGenConfig[0];

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {

                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= radius) {
                            blocks.add(new Vector3i(x, y, z));
                        }

                    }
                }
            }

            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> flatSquare() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int size = shapeGenConfig[0];
            int start = -size / 2;

            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    blocks.add(new Vector3i(
                            start + x,
                            0,
                            start + z
                    ));
                }
            }

            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> flatRectangle() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int width = shapeGenConfig[0];
            int depth = shapeGenConfig[1];

            int startX = -width / 2;
            int startZ = -depth / 2;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    blocks.add(new Vector3i(
                            startX + x,
                            0,
                            startZ + z
                    ));
                }
            }

            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> flatCircle() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int radius = shapeGenConfig[0];

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (x * x + z * z <= radius * radius) {
                        blocks.add(new Vector3i(x, 0, z));
                    }

                }
            }

            return blocks;
        };
    }

    private static Function<int[], List<Vector3i>> flatDiamond() {
        return shapeGenConfig -> {
            List<Vector3i> blocks = new ArrayList<>();

            int radius = shapeGenConfig[0];

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (Math.abs(x) + Math.abs(z) <= radius) {
                        blocks.add(new Vector3i(x, 0, z));
                    }

                }
            }

            return blocks;
        };
    }


    private final Function<int[], List<Vector3i>> blockPosFunction;

    FlowShape(Function<int[], List<Vector3i>> blockPosFunction) {
        this.blockPosFunction = blockPosFunction;
    };


    public Function<int[], List<Vector3i>> getBlockPosFunction() {
        return blockPosFunction;
    }
}
