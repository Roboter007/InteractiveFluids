package de.Roboter007.interactiveFluids.ticker.collision.manager;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;

public class Asset {

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
    }

    public Asset(BlockType blockType) {
        this.blockType = blockType;
        this.name = blockType.getId();
        this.id = BlockType.getBlockIdOrUnknown(name, "Couldn't find the block asset: " + name, name);
        this.fluid = null;
        this.fluidLevel = Byte.MIN_VALUE;
    }

    public byte getFluidLevel() {
        if (fluidLevel == -1) {
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
        return blockType != null;
    }

    public boolean isFluid() {
        return fluid != null;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public Fluid getFluid() {
        return fluid;
    }
}
