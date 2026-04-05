package de.Roboter007.interactiveFluids.ticker;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.common.util.MapUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector2i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.StateData;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import de.Roboter007.interactiveFluids.ticker.collision.block.BCConfigEntry;
import de.Roboter007.interactiveFluids.ticker.collision.block.BlockCollisionConfig;
import de.Roboter007.interactiveFluids.ticker.collision.block.BCResultConfig;
import de.Roboter007.interactiveFluids.ticker.fluidblocking.FluidBlockingBlockConfigEntry;
import de.Roboter007.interactiveFluids.ticker.collision.fluid.FluidCollisionConfig;
import de.Roboter007.interactiveFluids.ticker.collision.manager.FluidCollisionManager;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowPhase;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowShapeConfig;
import de.Roboter007.interactiveFluids.ticker.utils.IFOperators;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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

    private Map<String, FluidBlockingBlockConfigEntry> fluidBlockingBlocks = null;

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

    @Nullable
    public Map<String, FluidBlockingBlockConfigEntry> getFluidBlockingBlocks() {
        return fluidBlockingBlocks;
    }

    @NotNull
    public Map<String, FluidBlockingBlockConfigEntry> fluidBlockingBlocks() {
        if(fluidBlockingBlocks == null) {
            this.fluidBlockingBlocks = Collections.emptyMap();
        }
        return fluidBlockingBlocks;
    }

    public static int[] shapeGenConfigDefault() {
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
            BlockTickStrategy blockTickStrategy = checkNearbyBlocks(FlowPhase.Spread, fluidId, BLOCK_MAP, world, worldX, worldY, worldZ, accessor, blockSection);
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
    public BlockTickStrategy checkNearbyBlocks(FlowPhase flowPhase, int fluidId, BlockTypeAssetMap<String, BlockType> blockMap, World world, int worldX, int worldY, int worldZ, Accessor accessor, BlockSection blockSection) {
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
            if (block == null) {
                continue;
            }

            String otherBlockID = block.getId();
            if (otherBlockID != null && !otherBlockID.isEmpty() && !otherBlockID.equals("Empty")) {
                StateData blockStateData = block.getState();
                String blockState = null;
                if(blockStateData != null) {
                    blockState = block.getStateForBlock(block);
                }

                BCConfigEntry config = this.getBlockCollisionConfig().getCollision(flowPhase, otherBlockID, blockState);

                if (config != null) {
                    String expectedBlockState = config.getConditionConfig().getBlockState();

                    if (!expectedBlockState.isEmpty()) {
                        block = block.getBlockForState(expectedBlockState);
                    }

                    if (block != null && block.getDrawType() != DrawType.Cube) {
                        BlockBoundingBoxes blockBoundingBoxes = BlockBoundingBoxes.getAssetMap().getAsset(block.getHitboxType());

                        //ToDo: possible fix for getting hitbox height or something?
                        //BlockBoundingBoxes boxes = block.getData().getContainerKey((Class<? extends com.hypixel.hytale.assetstore.JsonAsset<BlockBoundingBoxes>>) BlockBoundingBoxes.class);
                        if (blockBoundingBoxes != null) {
                            // maxSearchHeight = 30 -> hardcoded due to missing possibility for getting the hitbox height
                            int dBlockY = blockY - getFluidPosition(30, block.getId(), otherBlockSection, blockMap, blockX, blockY, blockZ);

                            executeCollision(world, fluidId, config, otherBlockSection, blockX, dBlockY, blockZ);
                        }

                    } else {
                        executeCollision(world, fluidId, config, otherBlockSection, blockX, blockY, blockZ);
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


    private static void executeCollision(@Nonnull World world, int fluidId, @Nonnull BCConfigEntry config, BlockSection targetSection, int blockX, int blockY, int blockZ) {
        BCResultConfig resultConfig = config.getResultConfig();
        BlockType expectedType = BlockType.getAssetMap().getAsset(targetSection.get(blockX, blockY, blockZ));
        if (expectedType == null) {
            return;
        }

        BlockType resultBlock = null;
        int blockToPlaceId = resultConfig.getBlockToPlaceIndex();
        if (blockToPlaceId != Integer.MIN_VALUE) {
            resultBlock = BlockType.getAssetMap().getAsset(blockToPlaceId);
            if (resultBlock != null && !resultConfig.getBlockState().isEmpty()) {
                resultBlock = resultBlock.getBlockForState(resultConfig.getBlockState());
            }
            if (resultBlock == null) {
                return;
            }
        }

        FluidCollisionManager.addDelayedCollision(
                world,
                blockX,
                blockY,
                blockZ,
                expectedType,
                resultBlock,
                fluidId,
                resultConfig.getBlockPlaceDelay(),
                resultConfig.useBreakAnimation()
        );
    }




    @SuppressWarnings("removal")
    private static boolean executeCollision(@Nonnull World world, @Nonnull Accessor accessor, @Nonnull FluidSection fluidSection, BlockSection blockSection, @Nonnull FluidCollisionConfig config, int blockX, int blockY, int blockZ) {

        int blockToPlace = config.getBlockToPlaceIndex();
        if (blockToPlace != Integer.MIN_VALUE) {
            accessor.setBlock(blockX, blockY, blockZ, blockToPlace);

            setTickingSurrounding(accessor, blockSection, blockX, blockY, blockZ);
            fluidSection.setFluid(blockX, blockY, blockZ, 0, (byte) 0);
        }

        int soundEvent = config.getSoundEventIndex();
        if (soundEvent != Integer.MIN_VALUE) {
            world.execute(() -> SoundUtil.playSoundEvent3d(soundEvent, SoundCategory.SFX, (double) blockX, (double) blockY, (double) blockZ, world.getEntityStore().getStore()));
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

                    BlockTickStrategy blockTickStrategy = checkNearbyBlocks(FlowPhase.Demote, fluidId, BlockType.getAssetMap(), world, worldX, worldY, worldZ, accessor, blockSection);
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

    @Override
    public boolean blocksFluidFrom(@NotNull BlockType blockType, int rotationIndex, int offsetX, int offsetZ, int filler) {
        if(fluidBlockingBlocks != null) {
            if (fluidBlockingBlocks.containsKey(IFOperators.ALL_BLOCKS)) {
                FluidBlockingBlockConfigEntry entry = fluidBlockingBlocks.get(IFOperators.ALL_BLOCKS);
                if(!entry.blocksFluid) {
                    if(entry.keepVanillaBehavior) {
                        return super.blocksFluidFrom(blockType, rotationIndex, offsetX, offsetZ, filler);
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            } else if(fluidBlockingBlocks.containsKey(blockType.getId())) {
                FluidBlockingBlockConfigEntry entry = fluidBlockingBlocks.get(blockType.getId());
                if(!entry.blocksFluid) {
                    if(entry.keepVanillaBehavior) {
                        return super.blocksFluidFrom(blockType, rotationIndex, offsetX, offsetZ, filler);
                    } else {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }
        return super.blocksFluidFrom(blockType, rotationIndex, offsetX, offsetZ, filler);

    }



    static {
        CODEC = BuilderCodec.builder(InteractiveFluidTicker.class, InteractiveFluidTicker::new, BASE_CODEC)
                .appendInherited(new KeyedCodec<>("FluidBlockingBlocks",new MapCodec<>(FluidBlockingBlockConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.fluidBlockingBlocks = MapUtil.combineUnmodifiable(ticker.fluidBlockingBlocks(), o), (ticker) -> ticker.fluidBlockingBlocks, (ticker, parent) -> ticker.fluidBlockingBlocks = parent.fluidBlockingBlocks).add()
                .appendInherited(new KeyedCodec<>("SpreadFluid", Codec.STRING), (ticker, o) -> ticker.spreadFluid = o, (ticker) -> ticker.spreadFluid, (ticker, parent) -> ticker.spreadFluid = parent.spreadFluid).addValidator(Fluid.VALIDATOR_CACHE.getValidator().late()).add()
                .appendInherited(new KeyedCodec<>("FlowShape", FlowShapeConfig.CODEC), (ticker, o) -> ticker.flowShapeConfig = o, (ticker) -> ticker.flowShapeConfig, (ticker, parent) -> ticker.flowShapeConfig = parent.flowShapeConfig).documentation("Defines the interaction field of the fluid regarding their defined block collisions").add()
                .appendInherited(new KeyedCodec<>("FluidCollisions", new MapCodec<>(FluidCollisionConfig.CODEC, HashMap::new)), (ticker, o) -> ticker.rawFluidCollisionMap = MapUtil.combineUnmodifiable(ticker.rawFluidCollisionMap, o), (ticker) -> ticker.rawFluidCollisionMap, (ticker, parent) -> ticker.rawFluidCollisionMap = parent.rawFluidCollisionMap).documentation("Defines what happens when this fluid tries to spread into another fluid").add()
                .appendInherited(new KeyedCodec<>("BlockCollisions", BlockCollisionConfig.CODEC), (ticker, o) -> ticker.blockCollisionConfig = o, (ticker) -> ticker.blockCollisionConfig, (ticker, parent) -> ticker.blockCollisionConfig = parent.blockCollisionConfig).documentation("Defines the interaction field of the fluid regarding their defined block collisions").add()
                .build();

        INSTANCE = new InteractiveFluidTicker();
    }

}