package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import de.Roboter007.interactiveFluids.ticker.collision.manager.AssetType;
import de.Roboter007.interactiveFluids.ticker.collision.manager.PlaceableAsset;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CollisionSourceConfig {
    /*public static final BuilderCodec<CollisionSourceConfig> CODEC;

    protected String block;
    protected String blockState = "";


    public String getBlock() {
        return block;
    }


    public String getBlockState() {
        return blockState;
    }

    static {
        CODEC = BuilderCodec.builder(CollisionSourceConfig.class, CollisionSourceConfig::new)
                .appendInherited(new KeyedCodec<>("Block", Codec.STRING), (o, v) -> o.block = v, (o) -> o.block, (o, p) -> o.block = p.block).documentation("The block to that gets replaced when a collision occurs").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, (o) -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets replaced").add()
                .build();
    } */

    public static final BuilderCodec<CollisionSourceConfig> BASE_CODEC;
    public static final BuilderCodec<CollisionSourceConfig> FLUID_CODEC;
    public static final BuilderCodec<CollisionSourceConfig> BLOCK_CODEC;

    public static final BuilderCodec<CollisionSourceConfig> CODEC;

    protected AssetType assetType = AssetType.Block;
    protected String assetID = "Empty";
    protected String blockState = "";

    private int id = Integer.MIN_VALUE;

    public int getId() {
        if (id == Integer.MIN_VALUE) {
            if (assetType == AssetType.Block) {
                id = BlockType.getBlockIdOrUnknown(this.assetID, "Unknown block type %s", this.assetID);
            } else if (assetType == AssetType.Fluid) {
                id = Fluid.getFluidIdOrUnknown(this.assetID, "Unknown fluid %s", this.assetID);
            }
        }
        return id;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public String getAssetID() {
        return assetID;
    }

    public String getBlockState() {
        return assetType == AssetType.Block ? blockState : "";
    }

    static {
        FLUID_CODEC = buildCodec(AssetType.Fluid);
        BLOCK_CODEC = buildCodec(AssetType.Block);
        BASE_CODEC = BLOCK_CODEC;
        CODEC = BLOCK_CODEC;
    }

    private static BuilderCodec<CollisionSourceConfig> buildCodec(AssetType assetType) {
        BuilderCodec.Builder<CollisionSourceConfig> builder = BuilderCodec.builder(CollisionSourceConfig.class, CollisionSourceConfig::new)
                .appendInherited(new KeyedCodec<>("AssetType", new EnumCodec<>(AssetType.class)),
                        (o, v) -> o.assetType = v,
                        o -> o.assetType,
                        (o, p) -> o.assetType = p.assetType)
                .documentation("The asset type (block or fluid) of the asset which is needed in order to result in a collision.")
                .add()

                .appendInherited(new KeyedCodec<>("AssetId", Codec.STRING),
                        (o, v) -> o.assetID = v,
                        o -> o.assetID,
                        (o, p) -> o.assetID = p.assetID)
                .documentation("The asset (block or fluid) that is needed in order to result in a collision.")
                .add();

        if (assetType == AssetType.Block) {
            builder.appendInherited(new KeyedCodec<>("BlockState", Codec.STRING),
                            (o, v) -> o.blockState = v,
                            o -> o.blockState,
                            (o, p) -> o.blockState = p.blockState)
                    .documentation("The block state of the block that is required for the block in order to result in an collision.")
                    .add();

            builder.metadata(new CollisionSourceConfig.AssetVariantSchemaMetadata());
        }

        builder.afterDecode((asset, _) -> {
            asset.id = Integer.MIN_VALUE;
            if (asset.assetType != AssetType.Block) {
                asset.blockState = "";
            }
        });

        return builder.build();
    }

    private static final class AssetVariantSchemaMetadata implements Metadata {
        @Override
        public void modify(Schema schema) {
            if (!(schema instanceof ObjectSchema root)) {
                return;
            }

            Map<String, Schema> originalProps = root.getProperties();
            if (originalProps == null) {
                return;
            }

            Schema assetTypeSchema = originalProps.get("AssetType");
            Schema assetIdSchema = originalProps.get("AssetId");
            Schema blockStateSchema = originalProps.get("BlockState");

            if (assetTypeSchema == null || assetIdSchema == null || blockStateSchema == null) {
                return;
            }

            ObjectSchema blockSchema = new ObjectSchema();
            blockSchema.setAdditionalProperties(false);
            blockSchema.setTitle("Block");
            Objects.requireNonNull(blockSchema.getHytale()).setMergesProperties(true);

            LinkedHashMap<String, Schema> blockProps = new LinkedHashMap<>();
            blockProps.put("AssetType", StringSchema.constant("Block"));
            blockProps.put("AssetId", assetIdSchema);
            blockProps.put("BlockState", blockStateSchema);
            blockSchema.setProperties(blockProps);
            blockSchema.setRequired("AssetType");

            ObjectSchema fluidSchema = new ObjectSchema();
            fluidSchema.setAdditionalProperties(false);
            fluidSchema.setTitle("Fluid");
            Objects.requireNonNull(fluidSchema.getHytale()).setMergesProperties(true);

            LinkedHashMap<String, Schema> fluidProps = new LinkedHashMap<>();
            fluidProps.put("AssetType", StringSchema.constant("Fluid"));
            fluidProps.put("AssetId", assetIdSchema);
            fluidSchema.setProperties(fluidProps);
            fluidSchema.setRequired("AssetType");

            root.setProperties(new LinkedHashMap<>());
            root.setAnyOf(blockSchema, fluidSchema);
            Objects.requireNonNull(root.getHytale()).setMergesProperties(true);
            root.setHytaleSchemaTypeField(new Schema.SchemaTypeField("AssetType", "Block", "Block", "Fluid"));
        }
    }
}
