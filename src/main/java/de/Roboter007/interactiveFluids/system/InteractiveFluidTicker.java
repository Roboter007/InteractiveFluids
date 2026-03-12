package de.Roboter007.interactiveFluids.system;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import de.Roboter007.interactiveFluids.InteractiveFluidsPlugin;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class InteractiveFluidTicker extends FluidTicker {

    public static final BuilderCodec<InteractiveFluidTicker> CODEC;
    public static final InteractiveFluidTicker INSTANCE;

    private static final BlockTypeAssetMap<String, BlockType> BLOCK_MAP = BlockType.getAssetMap();
    private static final IndexedLookupTableAssetMap<String, Fluid> FLUID_MAP = Fluid.getAssetMap();

    private String spreadFluid;
    private int spreadFluidId;

    private Map<String, FluidCollisionConfig> rawFluidCollisionMap = Collections.emptyMap();
    @Nullable
    private transient Int2ObjectMap<FluidCollisionConfig> fluidCollisionMap = null;

    private FlowShapeConfig flowShapeConfig = null;
    private BlockCollisionConfig blockCollisionConfig = null;

    public FlowShapeConfig getFlowShapeConfig() {
        if(flowShapeConfig == null) {
            this.flowShapeConfig = new FlowShapeConfig();
        }
        return flowShapeConfig;
    }

    public BlockCollisionConfig getBlockCollisionConfig() {
        if(blockCollisionConfig == null) {
            this.blockCollisionConfig = new BlockCollisionConfig();
        }
        return blockCollisionConfig;
    }

    private static int[] shapeGenConfigDefault() {
        int[] defaultConfig = new int[3];
        defaultConfig[0] = 1;
        defaultConfig[1] = 1;
        defaultConfig[2] = 1;
        return defaultConfig;
    }


    @Nonnull
    @Override
    protected BlockTickStrategy spread(
            @Nonnull World world,
            long tick,
            @Nonnull Accessor accessor,
            @Nonnull FluidSection fluidSection,
            BlockSection blockSection,
            @Nonnull Fluid fluid,
            int fluidId,
            byte fluidLevel,
            int worldX,
            int worldY,
            int worldZ
    ) {
        if (worldY == 0) {
            return BlockTickStrategy.SLEEP;
        } else {
            BlockTickStrategy blockTickStrategy = checkNearbyBlocks(FlowPhase.Spread, BLOCK_MAP, world, worldX, worldY, worldZ, accessor, blockSection);
            if(blockTickStrategy != null) {
                return blockTickStrategy;
            }

            boolean isDifferentSectionBelow = fluidSection.getY() != ChunkUtil.chunkCoordinate(worldY - 1);
            FluidSection fluidSectionBelow = isDifferentSectionBelow ? accessor.getFluidSectionByBlock(worldX, worldY - 1, worldZ) : fluidSection;
            BlockSection blockSectionBelow = isDifferentSectionBelow ? accessor.getBlockSectionByBlock(worldX, worldY - 1, worldZ) : blockSection;
            if (fluidSectionBelow != null && blockSectionBelow != null) {
                int fluidBelowId = fluidSectionBelow.getFluidId(worldX, worldY - 1, worldZ);
                Fluid fluidBelow = FLUID_MAP.getAsset(fluidBelowId);
                byte fluidLevelBelow = fluidSectionBelow.getFluidLevel(worldX, worldY - 1, worldZ);
                int spreadFluidId = this.getSpreadFluidId(fluidId);
                int blockIdBelow = blockSectionBelow.get(worldX, worldY - 1, worldZ);
                BlockType blockBelow = BLOCK_MAP.getAsset(blockIdBelow);

                if (isSolid(blockBelow) || fluidBelowId != 0 && fluidBelowId != spreadFluidId && fluidBelowId == fluidId) {
                    if (fluidBelowId == 0 || fluidBelowId != spreadFluidId) {
                        if (fluidLevel == 1 && fluid.getMaxFluidLevel() != 1) {
                            return BlockTickStrategy.SLEEP;
                        }

                        int offsets = this.getSpreadOffsets(BLOCK_MAP, accessor, fluidSection, blockSection, worldX, worldY, worldZ, ORTO_OFFSETS, fluidId, 5);
                        if (offsets == 2147483646) {
                            return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
                        }

                        int childFillLevel = fluidLevel - 1;
                        if (spreadFluidId != fluidId) {
                            childFillLevel = FLUID_MAP.getAsset(spreadFluidId).getMaxFluidLevel() - 1;
                        }

                        BlockType sourceBlock = BLOCK_MAP.getAsset(blockSection.get(worldX, worldY, worldZ));
                        int sourceRotationIndex = blockSection.getRotationIndex(worldX, worldY, worldZ);
                        int sourceFiller = blockSection.getFiller(worldX, worldY, worldZ);

                        for (int i = 0; i < ORTO_OFFSETS.length; i++) {
                            if (offsets == 0 || (offsets & 1 << i) != 0) {
                                Vector2i offset = ORTO_OFFSETS[i];
                                int x = offset.x;
                                int z = offset.y;
                                int blockX = worldX + x;
                                int blockZ = worldZ + z;
                                if (!this.blocksFluidFrom(sourceBlock, sourceRotationIndex, -x, -z, sourceFiller)) {
                                    boolean isDifferentSection = !ChunkUtil.isSameChunkSection(worldX, worldY, worldZ, blockX, worldY, blockZ);
                                    FluidSection otherFluidSection = isDifferentSection ? accessor.getFluidSectionByBlock(blockX, worldY, blockZ) : fluidSection;
                                    BlockSection otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, worldY, blockZ) : blockSection;
                                    if (otherFluidSection == null || otherBlockSection == null) {
                                        return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
                                    }

                                    BlockType block = BLOCK_MAP.getAsset(otherBlockSection.get(blockX, worldY, blockZ));
                                    int rotationIndex = otherBlockSection.getRotationIndex(blockX, worldY, blockZ);
                                    int destFiller = otherBlockSection.getFiller(blockX, worldY, blockZ);
                                    if (!this.blocksFluidFrom(block, rotationIndex, x, z, destFiller)) {
                                        int otherFluidId = otherFluidSection.getFluidId(blockX, worldY, blockZ);
                                        if (otherFluidId != 0 && otherFluidId != spreadFluidId) {
                                            FluidCollisionConfig config = this.getFluidCollisionMap().get(otherFluidId);
                                            if (config == null || executeCollision(world, accessor, otherFluidSection, otherBlockSection, config, blockX, worldY, blockZ)) {
                                                continue;
                                            }
                                        }

                                        byte fillLevel = otherFluidSection.getFluidLevel(blockX, worldY, blockZ);
                                        if (otherFluidId != spreadFluidId || fillLevel < childFillLevel) {
                                            if (childFillLevel == 0) {
                                                otherFluidSection.setFluid(blockX, worldY, blockZ, 0, (byte)0);
                                            } else {
                                                otherFluidSection.setFluid(blockX, worldY, blockZ, spreadFluidId, (byte)childFillLevel);
                                                otherBlockSection.setTicking(blockX, worldY, blockZ, true);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return BlockTickStrategy.SLEEP;
                } else {
                    FluidCollisionConfig fluidCollisionConfig = this.getFluidCollisionMap().get(fluidBelowId);
                    if (fluidCollisionConfig != null && !executeCollision(world, accessor, fluidSectionBelow, blockSectionBelow, fluidCollisionConfig, worldX, worldY - 1, worldZ)) {
                        return BlockTickStrategy.CONTINUE;
                    } else {
                        if (fluidBelowId == 0 && !isSolid(blockBelow) || fluidBelowId == spreadFluidId && fluidLevelBelow < fluidBelow.getMaxFluidLevel()) {
                            int spreadId = this.getSpreadFluidId(fluidId);
                            Fluid spreadFluid = FLUID_MAP.getAsset(spreadId);
                            boolean changed = fluidSectionBelow.setFluid(worldX, worldY - 1, worldZ, spreadId, (byte)spreadFluid.getMaxFluidLevel());
                            if (changed) {
                                blockSectionBelow.setTicking(worldX, worldY - 1, worldZ, true);
                            }
                        }

                        return BlockTickStrategy.SLEEP;
                    }
                }
            } else {
                return BlockTickStrategy.SLEEP;
            }
        }
    }

    @Nullable
    public BlockTickStrategy checkNearbyBlocks(FlowPhase flowPhase, BlockTypeAssetMap<String, BlockType> blockMap, World world, int worldX, int worldY, int worldZ, Accessor accessor, BlockSection blockSection) {
        for (Vector3i vector3i : getNeighborBlockPos()) {
            int x = vector3i.x;
            int y = vector3i.y;
            int z = vector3i.z;
            int blockX = worldX + x;
            int blockY = worldY + y;
            int blockZ = worldZ + z;

            boolean isDifferentSection = !ChunkUtil.isSameChunkSection(worldX, worldY, worldZ, blockX, blockY, blockZ);
            BlockSection otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, blockY, blockZ) : blockSection;

            if (otherBlockSection == null) {
                return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
            }

            int blockId = otherBlockSection.get(blockX, blockY, blockZ);
            BlockType block = blockMap.getAsset(blockId);
            String otherBlockID = block.getId();
            if (otherBlockID != null && !otherBlockID.isEmpty() && !otherBlockID.equals("Empty")) {
                BlockCollisionConfigEntry config = this.getBlockCollisionConfig().getCollisionMap(flowPhase).get(otherBlockID);

                if (config != null) {
                    String blockState = config.getBlockState();
                    if (!blockState.isEmpty()) {
                        block = block.getBlockForState(blockState);
                    }

                    if (block != null && block.getDrawType() != DrawType.Cube) {
                        BlockBoundingBoxes blockBoundingBoxes = BlockBoundingBoxes.getAssetMap().getAsset(block.getHitboxType());

                        if (blockBoundingBoxes != null) {
                            // maxSearchHeight = 30 -> hardcoded due to missing possibility for getting the hitbox height
                            int dBlockY = blockY - getFluidPosition(30, block.getId(), otherBlockSection, blockMap, blockX, blockY, blockZ);

                            otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, dBlockY, blockZ) : blockSection;
                            executeCollision(world, accessor, otherBlockSection, config, blockX, dBlockY, blockZ);
                        }

                    } else {
                        otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, blockY, blockZ) : blockSection;
                        executeCollision(world, accessor, otherBlockSection, config, blockX, blockY, blockZ);
                    }
                }
            }
        }
        return null;
    }

    public static int getFluidPosition(int maxSearchHeight, String blockIdToCheck, BlockSection otherBlockSection, BlockTypeAssetMap<String, BlockType> blockMap, int blockX, int blockY, int blockZ) {
        for (int i = 1; i <= maxSearchHeight; i++) {
            int block = otherBlockSection.get(blockX, blockY - i, blockZ);
            BlockType selectedBlock = blockMap.getAsset(block);
            if(selectedBlock != null) {
                if(!selectedBlock.getId().equals(blockIdToCheck)) {
                    return i - 1;
                }
            } else {
                return i - 1;
            }
        }
        return 0;
    }

    private List<Vector3i> getNeighborBlockPos() {
        return this.getFlowShapeConfig().getFlowShape().getBlockPosFunction().apply(getFlowShapeConfig().getShapeGenConfig());
    }

    private static void executeCollision(@Nonnull World world, @Nonnull Accessor accessor, BlockSection blockSection, @Nonnull BlockCollisionConfigEntry config, int blockX, int blockY, int blockZ) {
        int blockToPlace = config.getBlockToPlaceIndex();
        BlockType block = BlockType.getAssetMap().getAsset(blockToPlace);
        if (blockToPlace != Integer.MIN_VALUE) {
            if(!config.blockState.isEmpty()) {
                if(block != null) {
                    block = block.getBlockForState(config.getBlockState());
                }
            }
            setBlock(accessor, blockX, blockY, blockZ, block);


            setTickingSurrounding(accessor, blockSection, blockX, blockY, blockZ);
        }

        int soundEvent = config.getSoundEventIndex();
        if (soundEvent != Integer.MIN_VALUE) {
            world.execute(() -> SoundUtil.playSoundEvent3d(soundEvent, SoundCategory.SFX, (double)blockX, (double)blockY, (double)blockZ, world.getEntityStore().getStore()));
        }
    }

    public static void setBlock(Accessor accessor, int x, int y, int z, BlockType blockType) {

        if(accessor instanceof CachedAccessor cachedAccessor) {
            try {
                Field field = cachedAccessor.getClass().getDeclaredField("commandBuffer");
                field.setAccessible(true);
                CommandBuffer<ChunkStore> commandBuffer = (CommandBuffer<ChunkStore>) field.get(accessor);

                if(commandBuffer == null) {
                    InteractiveFluidsPlugin.get().getLogger().atWarning().log("Failed to load Command Buffer!!!");
                    return;
                }

                Ref<ChunkStore> chunk = cachedAccessor.getChunk(ChunkUtil.chunkCoordinate(x), ChunkUtil.chunkCoordinate(z));
                if (chunk != null && chunk.isValid()) {
                    commandBuffer.run(store -> {
                        if (chunk.isValid()) {
                            WorldChunk wc = store.getComponent(chunk, WorldChunk.getComponentType());
                            if (wc != null) {
                                wc.setBlock(x, y, z, blockType);
                            }
                        }
                    });
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @SuppressWarnings("removal")
    private static boolean executeCollision(@Nonnull World world, @Nonnull Accessor accessor, @Nonnull FluidSection fluidSection, BlockSection blockSection, @Nonnull FluidCollisionConfig config, int blockX, int blockY, int blockZ) {
        int blockToPlace = config.getBlockToPlaceIndex();
        if (blockToPlace != Integer.MIN_VALUE) {
            accessor.setBlock(blockX, blockY, blockZ, blockToPlace);

            setTickingSurrounding(accessor, blockSection, blockX, blockY, blockZ);
            fluidSection.setFluid(blockX, blockY, blockZ, 0, (byte)0);
        }

        int soundEvent = config.getSoundEventIndex();
        if (soundEvent != Integer.MIN_VALUE) {
            world.execute(() -> SoundUtil.playSoundEvent3d(soundEvent, SoundCategory.SFX, (double)blockX, (double)blockY, (double)blockZ, world.getEntityStore().getStore()));
        }

        return !config.placeFluid;
    }

    public boolean isSelfFluid(int selfFluidId, int otherFluidId) {
        return super.isSelfFluid(selfFluidId, otherFluidId) || otherFluidId == this.getSpreadFluidId(selfFluidId);
    }

    private int getSpreadFluidId(int fluidId) {
        if (this.spreadFluidId == 0) {
            if (this.spreadFluid != null) {
                this.spreadFluidId = FLUID_MAP.getIndex(this.spreadFluid);
            } else {
                this.spreadFluidId = Integer.MIN_VALUE;
            }
        }

        return this.spreadFluidId == Integer.MIN_VALUE ? fluidId : this.spreadFluidId;
    }


    @Nonnull
    public Int2ObjectMap<FluidCollisionConfig> getFluidCollisionMap() {
        if (this.fluidCollisionMap == null) {
            Int2ObjectOpenHashMap<FluidCollisionConfig> collisionMap = new Int2ObjectOpenHashMap<>(this.rawFluidCollisionMap.size());

            for(Map.Entry<String, FluidCollisionConfig> entry : this.rawFluidCollisionMap.entrySet()) {
                int index = FLUID_MAP.getIndex(entry.getKey());
                if (index != Integer.MIN_VALUE) {
                    collisionMap.put(index, entry.getValue());
                }
            }

            this.fluidCollisionMap = collisionMap;
        }
        return this.fluidCollisionMap;
    }

    @Override
    public BlockTickStrategy process(
            World world,
            long tick,
            @Nonnull FluidTicker.Accessor accessor,
            @Nonnull FluidSection fluidSection,
            @Nonnull BlockSection blockSection,
            @Nonnull Fluid fluid,
            int fluidId,
            int worldX,
            int worldY,
            int worldZ
    ) {
        byte fluidLevel = fluidSection.getFluidLevel(worldX, worldY, worldZ);
        switch (this.isAlive(accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ)) {
            case ALIVE:
                return this.spread(world, tick, accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ);
            case DEMOTE:
                if (fluidLevel == 1) {
                    fluidSection.setFluid(worldX, worldY, worldZ, 0, (byte) 0);
                    setTickingSurrounding(accessor, blockSection, worldX, worldY, worldZ);

                    BlockTickStrategy blockTickStrategy = checkNearbyBlocks(FlowPhase.Demoted, BlockType.getAssetMap(), world, worldX, worldY, worldZ, accessor, blockSection);
                    return Objects.requireNonNullElse(blockTickStrategy, BlockTickStrategy.SLEEP);
                }

                fluidSection.setFluid(worldX, worldY, worldZ, fluidId, (byte) ((fluidLevel == 0 ? fluid.getMaxFluidLevel() : fluidLevel) - 1));
                setTickingSurrounding(accessor, blockSection, worldX, worldY, worldZ);
                return BlockTickStrategy.SLEEP;
            case WAIT_FOR_ADJACENT_CHUNK:
                return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
            default:
                return BlockTickStrategy.SLEEP;
        }
    }


    static {
        CODEC = BuilderCodec.builder(InteractiveFluidTicker.class, InteractiveFluidTicker::new, BASE_CODEC)
                .appendInherited(new KeyedCodec<>("SpreadFluid", Codec.STRING), (ticker, o) -> ticker.spreadFluid = o, (ticker) -> ticker.spreadFluid, (ticker, parent) -> ticker.spreadFluid = parent.spreadFluid).addValidator(Fluid.VALIDATOR_CACHE.getValidator().late()).add()
                .appendInherited(new KeyedCodec<>("FlowShape", FlowShapeConfig.CODEC), (ticker, o) -> ticker.flowShapeConfig = o, (ticker) -> ticker.flowShapeConfig, (ticker, parent) -> ticker.flowShapeConfig = parent.flowShapeConfig).documentation("Defines the interaction field of the fluid regarding their defined block collisions").add()
                .appendInherited(new KeyedCodec<>("FluidCollisions", new MapCodec<>(FluidCollisionConfig.CODEC, HashMap::new)), (ticker, o) -> ticker.rawFluidCollisionMap = MapUtil.combineUnmodifiable(ticker.rawFluidCollisionMap, o), (ticker) -> ticker.rawFluidCollisionMap, (ticker, parent) -> ticker.rawFluidCollisionMap = parent.rawFluidCollisionMap).documentation("Defines what happens when this fluid tries to spread into another fluid").add()
                .appendInherited(new KeyedCodec<>("BlockCollisions", BlockCollisionConfig.CODEC), (ticker, o) -> ticker.blockCollisionConfig = o, (ticker) -> ticker.blockCollisionConfig, (ticker, parent) -> ticker.blockCollisionConfig = parent.blockCollisionConfig).documentation("Defines the interaction field of the fluid regarding their defined block collisions").add()
                .build();

        INSTANCE = new InteractiveFluidTicker();
    }

    public enum FlowPhase {
        Spread,
        Demoted
    }

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

    public static class FlowShapeConfig {
        public static final BuilderCodec<FlowShapeConfig> CODEC;

        private FlowShape flowShape = FlowShape.Diamond;
        private int[] shapeGenConfig = shapeGenConfigDefault();

        public FlowShape getFlowShape() {
            return flowShape;
        }

        public int[] getShapeGenConfig() {
            return shapeGenConfig;
        }

        static {
            CODEC = BuilderCodec.builder(FlowShapeConfig.class, FlowShapeConfig::new)
                    .appendInherited(new KeyedCodec<>("FlowShape", new EnumCodec<>(FlowShape.class)), (ticker, o) -> ticker.flowShape = o, (ticker) -> ticker.flowShape, (ticker, parent) -> ticker.flowShape = parent.flowShape).add()
                    .appendInherited(new KeyedCodec<>("ShapeSizeOptions", Codec.INT_ARRAY), (ticker, o) -> ticker.shapeGenConfig = o, (ticker) -> ticker.shapeGenConfig, (ticker, parent) -> ticker.shapeGenConfig = parent.shapeGenConfig).add()
                    .build();
        }
    }

    public static class BlockCollisionConfig {
        public static final BuilderCodec<BlockCollisionConfig> CODEC;

        protected Map<String, BlockCollisionConfigEntry> rawBlockSpreadCollisionMap = Collections.emptyMap();
        protected Map<String, BlockCollisionConfigEntry> rawBlockDemoteCollisionMap = Collections.emptyMap();

        @Nullable
        protected transient Object2ObjectMap<String, BlockCollisionConfigEntry> blockSpreadCollisionMap = null;
        @Nullable
        protected transient Object2ObjectMap<String, BlockCollisionConfigEntry> blockDemoteCollisionMap = null;

        @Nonnull
        public Object2ObjectMap<String, BlockCollisionConfigEntry> getSpreadCollisionMap() {
            if (this.blockSpreadCollisionMap == null) {
                Object2ObjectMap<String, BlockCollisionConfigEntry> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockSpreadCollisionMap.size());

                for(Map.Entry<String, BlockCollisionConfigEntry> entry : this.rawBlockSpreadCollisionMap.entrySet()) {
                    var block = BlockType.getAssetMap().getAsset(entry.getKey());

                    if(block != null) {
                        String blockId = block.getId();
                        if (blockId != null && !blockId.isEmpty()) {
                            collisionMap.put(blockId, entry.getValue());
                        }
                    }
                }

                this.blockSpreadCollisionMap = collisionMap;
            }

            return this.blockSpreadCollisionMap;
        }

        @Nonnull
        public Object2ObjectMap<String, BlockCollisionConfigEntry> getDemoteCollisionMap() {
            if (this.blockDemoteCollisionMap == null) {
                Object2ObjectMap<String, BlockCollisionConfigEntry> collisionMap = new Object2ObjectOpenHashMap<>(this.rawBlockDemoteCollisionMap.size());

                for(Map.Entry<String, BlockCollisionConfigEntry> entry : this.rawBlockDemoteCollisionMap.entrySet()) {
                    var block = BlockType.getAssetMap().getAsset(entry.getKey());

                    if(block != null) {
                        String blockId = block.getId();
                        if (blockId != null && !blockId.isEmpty()) {
                            collisionMap.put(blockId, entry.getValue());
                        }
                    }
                }

                this.blockDemoteCollisionMap = collisionMap;
            }

            return this.blockDemoteCollisionMap;
        }

        @Nonnull
        public Object2ObjectMap<String, BlockCollisionConfigEntry> getCollisionMap(FlowPhase flowPhase) {
            if(flowPhase == FlowPhase.Spread) {
                return getSpreadCollisionMap();
            } else {
                return getDemoteCollisionMap();
            }
        }


        static {
            CODEC = BuilderCodec.builder(BlockCollisionConfig.class, BlockCollisionConfig::new)
                    .appendInherited(new KeyedCodec<>("Spread", new MapCodec<>(BlockCollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockSpreadCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockSpreadCollisionMap, o), (ticker) -> ticker.rawBlockSpreadCollisionMap, (ticker, parent) -> ticker.rawBlockSpreadCollisionMap = parent.rawBlockSpreadCollisionMap).documentation("Defines what happens when this fluid touches a block").add()
                    .appendInherited(new KeyedCodec<>("Demote", new MapCodec<>(BlockCollisionConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.rawBlockDemoteCollisionMap = MapUtil.combineUnmodifiable(ticker.rawBlockDemoteCollisionMap, o), (ticker) -> ticker.rawBlockDemoteCollisionMap, (ticker, parent) -> ticker.rawBlockDemoteCollisionMap = parent.rawBlockDemoteCollisionMap).documentation("Defines what happens when this fluid stops touching a specific block").add()
                    .build();
        }
    }


    public static class BlockCollisionConfigEntry {
        public static final BuilderCodec<BlockCollisionConfigEntry> CODEC;
        protected String blockToPlace;
        protected String blockState = "";
        protected int blockToPlaceIndex = Integer.MIN_VALUE;
        protected String soundEvent;
        protected int soundEventIndex = Integer.MIN_VALUE;

        public int getBlockToPlaceIndex() {
            if (this.blockToPlaceIndex == Integer.MIN_VALUE && this.blockToPlace != null) {
                this.blockToPlaceIndex = BlockType.getBlockIdOrUnknown(this.blockToPlace, "Unknown block type %s", this.blockToPlace);
            }

            return this.blockToPlaceIndex;
        }

        public String getBlockToPlace() {
            return blockToPlace;
        }


        public String getBlockState() {
            return blockState;
        }

        public int getSoundEventIndex() {
            if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
                this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
            }

            return this.soundEventIndex;
        }

        static {
            CODEC = BuilderCodec.builder(BlockCollisionConfigEntry.class, BlockCollisionConfigEntry::new)
                    .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to place when a collision occurs").add()
                    .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets placed").add()
                    .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                    .build();

        }
    }

    public static class FluidCollisionConfig {
        public static final BuilderCodec<FluidCollisionConfig> CODEC;
        private String blockToPlace;
        private int blockToPlaceIndex = Integer.MIN_VALUE;
        public boolean placeFluid = false;
        private String soundEvent;
        private int soundEventIndex = Integer.MIN_VALUE;

        public int getBlockToPlaceIndex() {
            if (this.blockToPlaceIndex == Integer.MIN_VALUE && this.blockToPlace != null) {
                this.blockToPlaceIndex = BlockType.getBlockIdOrUnknown(this.blockToPlace, "Unknown block type %s", this.blockToPlace);
            }

            return this.blockToPlaceIndex;
        }

        public int getSoundEventIndex() {
            if (this.soundEventIndex == Integer.MIN_VALUE && this.soundEvent != null) {
                this.soundEventIndex = SoundEvent.getAssetMap().getIndex(this.soundEvent);
            }

            return this.soundEventIndex;
        }

        static {
            CODEC = (BuilderCodec.builder(FluidCollisionConfig.class, FluidCollisionConfig::new)
                    .appendInherited(new KeyedCodec<>("BlockToPlace", Codec.STRING), (o, v) -> o.blockToPlace = v, (o) -> o.blockToPlace, (o, p) -> o.blockToPlace = p.blockToPlace).documentation("The block to place when a collision occurs").add())
                    .appendInherited(new KeyedCodec<>("SoundEvent", Codec.STRING), (o, v) -> o.soundEvent = v, (o) -> o.soundEvent, (o, p) -> o.soundEvent = p.soundEvent).addValidator(SoundEvent.VALIDATOR_CACHE.getValidator()).add()
                    .appendInherited(new KeyedCodec<>("PlaceFluid", Codec.BOOLEAN), (o, v) -> o.placeFluid = v, (o) -> o.placeFluid, (o, p) -> o.placeFluid = p.placeFluid).documentation("Whether to still place the fluid on collision").add()
                    .build();
        }
    }
}