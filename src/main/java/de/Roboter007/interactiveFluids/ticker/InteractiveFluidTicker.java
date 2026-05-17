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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import de.Roboter007.interactiveFluids.ticker.collision.config.CollisionConfigEntry;
import de.Roboter007.interactiveFluids.ticker.collision.config.CollisionConfig;
import de.Roboter007.interactiveFluids.ticker.collision.config.CollisionResultConfig;
import de.Roboter007.interactiveFluids.ticker.collision.config.CollisionSourceConfig;
import de.Roboter007.interactiveFluids.ticker.collision.AssetType;
import de.Roboter007.interactiveFluids.ticker.fluidblocking.FluidBlockingBlockConfigEntry;
import de.Roboter007.interactiveFluids.ticker.collision.FluidCollisionManager;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowPhase;
import de.Roboter007.interactiveFluids.ticker.flowShape.FlowShapeConfig;
import de.Roboter007.interactiveFluids.ticker.utils.IFOperators;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class InteractiveFluidTicker extends FluidTicker {

    public static final BuilderCodec<InteractiveFluidTicker> CODEC;
    public static final InteractiveFluidTicker INSTANCE;

    private static final BlockTypeAssetMap<String, BlockType> BLOCK_MAP = BlockType.getAssetMap();
    private static final IndexedLookupTableAssetMap<String, Fluid> FLUID_MAP = Fluid.getAssetMap();

    private static final int[] DEFAULT_SHAPE_SIZE = {1, 1, 1};

    private String spreadFluid;
    private int spreadFluidId;

    private boolean spreadWithoutSource = false;
    private Map<String, FluidBlockingBlockConfigEntry> fluidBlockingBlocks = null;

    private FlowShapeConfig flowShapeConfig = null;
    private CollisionConfig collisionConfig = null;

    public FlowShapeConfig getFlowShapeConfig() {
        if(flowShapeConfig == null) {
            this.flowShapeConfig = new FlowShapeConfig();
        }
        return flowShapeConfig;
    }

    public CollisionConfig getCollisionConfig() {
        if(collisionConfig == null) {
            this.collisionConfig = new CollisionConfig();
        }
        return collisionConfig;
    }

    @NotNull
    public Map<String, FluidBlockingBlockConfigEntry> fluidBlockingBlocks() {
        if(fluidBlockingBlocks == null) {
            this.fluidBlockingBlocks = Collections.emptyMap();
        }
        return fluidBlockingBlocks;
    }

    public static int[] shapeGenConfigDefault() {
        return DEFAULT_SHAPE_SIZE;
    }

    public boolean spreadWithoutSource() {
        return spreadWithoutSource;
    }

    @Nonnull
    @Override
    protected BlockTickStrategy spread(@Nonnull World world, long tick, @Nonnull Accessor accessor, @Nonnull FluidSection fluidSection, BlockSection blockSection,
                                       @Nonnull Fluid fluid, int fluidId, byte fluidLevel, int worldX, int worldY, int worldZ) {
        if (worldY == 0) {
            return BlockTickStrategy.SLEEP;
        } else {
            BlockTickStrategy blockTickStrategy = handleCollisions(FlowPhase.Spread, fluidId, world, worldX, worldY, worldZ, accessor, blockSection, fluidSection);
            if(blockTickStrategy != null) {
                return blockTickStrategy;
            } else {
                return handleFlowingFluid(FlowPhase.Spread, accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ);
            }
        }
    }

    @Nonnull
    protected BlockTickStrategy demote(@Nonnull World world, @Nonnull Accessor accessor, @Nonnull FluidSection fluidSection, BlockSection blockSection,
                                       @Nonnull Fluid fluid, int fluidId, byte fluidLevel, int worldX, int worldY, int worldZ) {
        if (fluidLevel == 1) {
            fluidSection.setFluid(worldX, worldY, worldZ, 0, (byte) 0);
            setTickingSurrounding(accessor, blockSection, worldX, worldY, worldZ);

            BlockTickStrategy blockTickStrategy = handleCollisions(FlowPhase.Demote, fluidId, world, worldX, worldY, worldZ, accessor, blockSection, fluidSection);
            return Objects.requireNonNullElse(blockTickStrategy, BlockTickStrategy.SLEEP);
        }

        fluidSection.setFluid(worldX, worldY, worldZ, fluidId, (byte) ((fluidLevel == 0 ? fluid.getMaxFluidLevel() : fluidLevel) - 1));
        if(spreadWithoutSource) {
            BlockTickStrategy blockTickStrategy = handleFlowingFluid(FlowPhase.Spread, accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ);
            setTickingSurrounding(accessor, blockSection, worldX, worldY, worldZ);
            return Objects.requireNonNullElse(blockTickStrategy, BlockTickStrategy.SLEEP);
        } else {
            setTickingSurrounding(accessor, blockSection, worldX, worldY, worldZ);
            return BlockTickStrategy.SLEEP;
        }
    }

    public BlockTickStrategy handleFlowingFluid(FlowPhase flowPhase, Accessor accessor, FluidSection fluidSection, BlockSection blockSection, Fluid fluid, int fluidId, byte fluidLevel, int worldX, int worldY, int worldZ) {
        boolean isDifferentSectionBelow = fluidSection.getY() != ChunkUtil.chunkCoordinate(worldY - 1);
        FluidSection fluidSectionBelow = isDifferentSectionBelow ? accessor.getFluidSectionByBlock(worldX, worldY - 1, worldZ) : fluidSection;
        BlockSection blockSectionBelow = isDifferentSectionBelow ? accessor.getBlockSectionByBlock(worldX, worldY - 1, worldZ) : blockSection;
        if (fluidSectionBelow != null && blockSectionBelow != null) {
            int fluidBelowId = fluidSectionBelow.getFluidId(worldX, worldY - 1, worldZ);
            int blockIdBelow = blockSectionBelow.get(worldX, worldY - 1, worldZ);
            int spreadFluidId = this.getSpreadFluidId(fluidId);

            Fluid fluidBelow = FLUID_MAP.getAsset(fluidBelowId);
            Fluid spreadFluid = FLUID_MAP.getAsset(spreadFluidId);
            BlockType blockBelow = BLOCK_MAP.getAsset(blockIdBelow);


            if (blockBelow != null && isSolid(blockBelow) || fluidBelowId != 0 && fluidBelowId != spreadFluidId && fluidBelowId == fluidId) {
                if (fluidBelowId == 0 || fluidBelowId != spreadFluidId) {
                    if (fluidLevel == 1 && fluid.getMaxFluidLevel() != 1) {
                        return BlockTickStrategy.SLEEP;
                    }

                    int offsets = this.getSpreadOffsets(BLOCK_MAP, accessor, fluidSection, blockSection, worldX, worldY, worldZ, ORTO_OFFSETS, fluidId, 5);
                    if (offsets == 2147483646) {
                        return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
                    }

                    int childFillLevel = fluidLevel - 1;
                    if (spreadFluid != null && spreadFluidId != fluidId) {
                        childFillLevel = spreadFluid.getMaxFluidLevel() - 1;
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

                            if (sourceBlock != null && !this.blocksFluidFrom(sourceBlock, sourceRotationIndex, -x, -z, sourceFiller)) {
                                boolean isDifferentSection = !ChunkUtil.isSameChunkSection(worldX, worldY, worldZ, blockX, worldY, blockZ);
                                FluidSection otherFluidSection = isDifferentSection ? accessor.getFluidSectionByBlock(blockX, worldY, blockZ) : fluidSection;
                                BlockSection otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, worldY, blockZ) : blockSection;

                                if (otherFluidSection == null || otherBlockSection == null) {
                                    return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
                                }

                                BlockType block = BLOCK_MAP.getAsset(otherBlockSection.get(blockX, worldY, blockZ));
                                int rotationIndex = otherBlockSection.getRotationIndex(blockX, worldY, blockZ);
                                int destFiller = otherBlockSection.getFiller(blockX, worldY, blockZ);

                                int otherFluidId = otherFluidSection.getFluidId(blockX, worldY, blockZ);
                                Fluid otherFluid = FLUID_MAP.getAsset(otherFluidId);


                                boolean fluidCollision = otherFluidId != 0 && otherFluidId != spreadFluidId;
                                boolean blockCollision = block != null && !block.getId().equals("Empty");

                                if (fluidCollision || blockCollision) {
                                    CollisionConfigEntry entry = getCollisionEntry(flowPhase, otherFluid, block);
                                    if (entry == null || entry.placeFluid()) {
                                        continue;
                                    }
                                }

                                if (block != null && !this.blocksFluidFrom(block, rotationIndex, x, z, destFiller)) {
                                    byte fillLevel = otherFluidSection.getFluidLevel(blockX, worldY, blockZ);
                                    if (otherFluidId != spreadFluidId || fillLevel < childFillLevel) {
                                        if (childFillLevel == 0) {
                                            otherFluidSection.setFluid(blockX, worldY, blockZ, 0, (byte) 0);
                                        } else {
                                            otherFluidSection.setFluid(blockX, worldY, blockZ, spreadFluidId, (byte) childFillLevel);
                                            otherBlockSection.setTicking(blockX, worldY, blockZ, true);
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            } else {
                CollisionConfigEntry entry = getCollisionEntry(flowPhase, fluidBelow, blockBelow);

                if (entry != null && !entry.placeFluid()) {
                    return BlockTickStrategy.CONTINUE;
                } else {
                    byte fluidLevelBelow = fluidSectionBelow.getFluidLevel(worldX, worldY - 1, worldZ);
                    if (blockBelow != null && fluidBelowId == 0 && !isSolid(blockBelow) || fluidBelow != null && fluidBelowId == spreadFluidId && fluidLevelBelow < fluidBelow.getMaxFluidLevel()) {
                        boolean changed = spreadFluid != null && fluidSectionBelow.setFluid(worldX, worldY - 1, worldZ, spreadFluidId, (byte)spreadFluid.getMaxFluidLevel());
                        if (changed) {
                            blockSectionBelow.setTicking(worldX, worldY - 1, worldZ, true);
                        }
                    }
                }
            }
        }
        return BlockTickStrategy.SLEEP;
    }

    @Nullable
    public CollisionConfigEntry getCollisionEntry(FlowPhase flowPhase, Fluid fluid, BlockType block) {
        Object2ObjectOpenHashMap<String, CollisionConfigEntry> collisionMap = getCollisionConfig().getCollision(flowPhase);
        CollisionConfigEntry config;

        if (collisionMap.containsKey(IFOperators.ANYTHING)) {
            if (fluid != null && fluid.getId().equals("Empty")) {
                config = null;
            } else {
                config = collisionMap.get(IFOperators.ANYTHING);
            }

        } else if (collisionMap.containsKey(IFOperators.ALL_FROM_CURRENT_ASSET_TYPE)) {
            if (fluid != null && fluid.getId().equals("Empty")) {
                config = null;
            } else {
                config = collisionMap.get(IFOperators.ALL_FROM_CURRENT_ASSET_TYPE);
                if (config.getSourceConfig().getAssetType() == AssetType.Fluid) {
                    config = null;
                }
            }
        } else if (fluid != null && !fluid.getId().equals("Empty")) {
            config = collisionMap.get(fluid.getId());
        } else if (block != null && !block.getId().equals("Empty")) {
            config = collisionMap.get(block.getId());
        } else {
            config = null;
        }

        return config;
    }


    @Nullable
    public BlockTickStrategy handleCollisions(FlowPhase flowPhase, int fluidId, World world, int worldX, int worldY, int worldZ, Accessor accessor, BlockSection blockSection, FluidSection fluidSection) {
        for (Vector3i vector3i : getNeighborBlockPos()) {
            int blockX = worldX + vector3i.x;
            int blockY = worldY + vector3i.y;
            int blockZ = worldZ + vector3i.z;

            boolean isDifferentSection = !ChunkUtil.isSameChunkSection(worldX, worldY, worldZ, blockX, blockY, blockZ);
            BlockSection otherBlockSection = isDifferentSection ? accessor.getBlockSectionByBlock(blockX, blockY, blockZ) : blockSection;
            FluidSection otherFluidSection = isDifferentSection ? accessor.getFluidSectionByBlock(blockX, worldY, blockZ) : fluidSection;

            if (otherBlockSection == null || otherFluidSection == null) {
                return BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
            }


            BlockType block = BLOCK_MAP.getAsset(otherBlockSection.get(blockX, blockY, blockZ));
            Fluid fluid = otherFluidSection.getFluid(blockX, blockY, blockZ);

            if(block != null) {
                String blockName = block.getId();
                if (blockName != null && !blockName.isEmpty() && !blockName.equals("Empty")) {
                    StateData blockStateData = block.getState();
                    String blockState = null;
                    if (blockStateData != null) {
                        blockState = block.getStateForBlock(block);
                    }

                    CollisionConfigEntry config = this.getCollisionConfig().getCollisionByCondition(flowPhase, blockName, blockState);

                    if (config != null) {
                        if (block.getDrawType() != DrawType.Cube) {
                            BlockBoundingBoxes blockBoundingBoxes = BlockBoundingBoxes.getAssetMap().getAsset(block.getHitboxType());

                            if (blockBoundingBoxes != null) {
                                int dBlockY = blockY - getFluidPosition(30, block.getId(), otherBlockSection, blockX, blockY, blockZ);

                                executeCollision(world, fluidId, config, otherBlockSection, otherFluidSection, blockX, dBlockY, blockZ);
                            }

                        } else {
                            executeCollision(world, fluidId, config, otherBlockSection, fluidSection, blockX, blockY, blockZ);
                        }
                    }
                }
            }

            if(fluid != null) {
                String fluidName = fluid.getId();
                if (fluidName != null && !fluidName.isEmpty() && !fluidName.equals("Empty")) {
                    Object2ObjectOpenHashMap<String, CollisionConfigEntry> collisionMap = this.getCollisionConfig().getCollision(flowPhase);
                    CollisionConfigEntry configEntry = collisionMap.get(fluidName);

                    if (configEntry != null) {
                        executeCollision(world, fluidId, configEntry, otherBlockSection, fluidSection, blockX, blockY, blockZ);
                    }
                }
            }
        }
        return null;
    }

    public static int getFluidPosition(int maxSearchHeight, String blockIdToCheck, BlockSection otherBlockSection, int blockX, int blockY, int blockZ) {
        for (int i = 1; i <= maxSearchHeight; i++) {
            int block = otherBlockSection.get(blockX, blockY - i, blockZ);
            BlockType selectedBlock = BLOCK_MAP.getAsset(block);
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


    private static void executeCollision(@Nonnull World world, int fluidId, @Nonnull CollisionConfigEntry config, BlockSection blockSection, FluidSection fluidSection, int blockX, int blockY, int blockZ) {
        CollisionSourceConfig sourceConfig = config.getSourceConfig();
        CollisionResultConfig resultConfig = config.getResultConfig();

        if(sourceConfig.getAssetType() == AssetType.Block) {
            BlockType sourceType = BLOCK_MAP.getAsset(blockSection.get(blockX, blockY, blockZ));
            if(sourceType != null) {

                if (resultConfig.getAssetType() == AssetType.Block) {
                    BlockType resultBlock = BlockType.getAssetMap().getAsset(resultConfig.getAssetID());

                    if (resultBlock != null && !resultConfig.getBlockState().isEmpty()) {
                        resultBlock = resultBlock.getBlockForState(resultConfig.getBlockState());
                    }
                    if (resultBlock != null) {
                        FluidCollisionManager.addDelayedBlockToBlock(world, blockX, blockY, blockZ, sourceType, resultBlock, fluidId, config.getPlaceDelay(), config.useBreakAnimation());
                    }
                } else {
                    Fluid resultFluid = Fluid.getAssetMap().getAsset(resultConfig.getAssetID());

                    if (resultFluid != null) {
                        FluidCollisionManager.addDelayedBlockToFluid(world, blockX, blockY, blockZ, sourceType, resultFluid, resultConfig.getFluidLevel(), fluidId, config.getPlaceDelay(), config.useBreakAnimation());
                    }
                }
            }
        } else {
            Fluid sourceType = fluidSection.getFluid(blockX, blockY, blockZ);

            if(sourceType != null) {
                if (resultConfig.getAssetType() == AssetType.Block) {
                    BlockType resultBlock = BLOCK_MAP.getAsset(resultConfig.getAssetID());

                    if (resultBlock != null && !resultConfig.getBlockState().isEmpty()) {
                        resultBlock = resultBlock.getBlockForState(resultConfig.getBlockState());
                    }

                    if(resultBlock != null) {
                        FluidCollisionManager.addDelayedFluidToBlock(world, blockX, blockY, blockZ, sourceType, resultBlock, fluidId, config.getPlaceDelay());
                    }
                } else {
                    Fluid resultFluid = FLUID_MAP.getAsset(resultConfig.getAssetID());

                    if(resultFluid != null) {
                        FluidCollisionManager.addDelayedFluidToFluid(world, blockX, blockY, blockZ, sourceType, resultFluid, resultConfig.getFluidLevel(), fluidId, config.getPlaceDelay());
                    }
                }
            }
        }
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
        return switch (this.isAlive(accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ)) {
            case ALIVE -> this.spread(world, tick, accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ);
            case DEMOTE -> this.demote(world, accessor, fluidSection, blockSection, fluid, fluidId, fluidLevel, worldX, worldY, worldZ);
            case WAIT_FOR_ADJACENT_CHUNK -> BlockTickStrategy.WAIT_FOR_ADJACENT_CHUNK_LOAD;
        };
    }

    @Override
    public boolean blocksFluidFrom(@NotNull BlockType blockType, int rotationIndex, int offsetX, int offsetZ, int filler) {
        if(fluidBlockingBlocks != null) {
            if (fluidBlockingBlocks.containsKey(IFOperators.ANYTHING)) {
                FluidBlockingBlockConfigEntry entry = fluidBlockingBlocks.get(IFOperators.ANYTHING);
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
                .appendInherited(new KeyedCodec<>("SpreadWithoutSource", Codec.BOOLEAN), (ticker, o) -> ticker.spreadWithoutSource = o, (ticker) -> ticker.spreadWithoutSource, (ticker, parent) -> ticker.spreadWithoutSource = parent.spreadWithoutSource).documentation("allows a fluid to flow even though its source block got removed (normal Hytale behavior: false)").add()
                .appendInherited(new KeyedCodec<>("FluidBlockingBlocks",new MapCodec<>(FluidBlockingBlockConfigEntry.CODEC, HashMap::new)), (ticker, o) -> ticker.fluidBlockingBlocks = MapUtil.combineUnmodifiable(ticker.fluidBlockingBlocks(), o), (ticker) -> ticker.fluidBlockingBlocks, (ticker, parent) -> ticker.fluidBlockingBlocks = parent.fluidBlockingBlocks).add()
                .appendInherited(new KeyedCodec<>("SpreadFluid", Codec.STRING), (ticker, o) -> ticker.spreadFluid = o, (ticker) -> ticker.spreadFluid, (ticker, parent) -> ticker.spreadFluid = parent.spreadFluid).addValidator(Fluid.VALIDATOR_CACHE.getValidator().late()).add()
                .appendInherited(new KeyedCodec<>("FlowShape", FlowShapeConfig.CODEC), (ticker, o) -> ticker.flowShapeConfig = o, (ticker) -> ticker.flowShapeConfig, (ticker, parent) -> ticker.flowShapeConfig = parent.flowShapeConfig).documentation("Defines the interaction field of the fluid regarding their defined block collisions").add()
                .appendInherited(new KeyedCodec<>("Collisions", CollisionConfig.CODEC), (ticker, o) -> ticker.collisionConfig = o, (ticker) -> ticker.collisionConfig, (ticker, parent) -> ticker.collisionConfig = parent.collisionConfig).documentation("Defines the interaction field of the fluid regarding their defined (block and fluid) collisions").add()
                .build();

        INSTANCE = new InteractiveFluidTicker();
    }

}