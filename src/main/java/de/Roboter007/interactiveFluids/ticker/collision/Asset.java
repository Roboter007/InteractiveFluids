package de.Roboter007.interactiveFluids.ticker.collision;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;

public class Asset {

    private final AssetType assetType;

    private final int id;
    private final String name;

    private final Fluid fluid;
    private final BlockType blockType;

    private final byte fluidLevel;

    public Asset(Fluid fluid) {
        this(fluid, Byte.MIN_VALUE);
    }

    public Asset(Fluid fluid, byte fluidLevel) {
        this.fluid = fluid;
        this.name = fluid.getId();
        this.id = Fluid.getFluidIdOrUnknown(name, "Couldn't find the fluid asset: " + name, name);
        this.fluidLevel = fluidLevel;
        this.blockType = null;
        this.assetType = AssetType.Fluid;
    }

    public Asset(BlockType blockType) {
        this.blockType = blockType;
        this.name = blockType.getId();
        this.id = BlockType.getBlockIdOrUnknown(name, "Couldn't find the block asset: " + name, name);
        this.fluid = null;
        this.fluidLevel = Byte.MIN_VALUE;
        this.assetType = AssetType.Block;
    }

    public byte getFluidLevel(byte fluidLevelAtPos) {
        if (fluidLevel == -2) {
            return fluidLevelAtPos;
        } else if (fluidLevel == -1) {
            return (byte) fluid.getMaxFluidLevel();
        } else if(fluidLevel <= 0) {
            return 0;
        }  else if(fluidLevel >= fluid.getMaxFluidLevel()) {
            return (byte) fluid.getMaxFluidLevel();
        } else {
            return fluidLevel;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isBlock() {
        return assetType == AssetType.Block && blockType != null;
    }

    public boolean isFluid() {
        return assetType == AssetType.Fluid && fluid != null;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    private enum AssetType {
        Block,
        Fluid
    }
}
