package de.Roboter007.interactiveFluids.ticker.collision.manager;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import org.bson.BsonValue;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class PlaceableAsset {

    public static final BuilderCodec<PlaceableAsset> CODEC;

    protected AssetType assetType = AssetType.Block;
    protected String assetID = "Empty";
    private int id = Integer.MIN_VALUE;
    private boolean placeFluid = false;


    protected String blockState = "";

    protected byte fluidLevel = Byte.MIN_VALUE;

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

    public boolean placeFluid() {
        return placeFluid;
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

    public byte getFluidLevel() {
        return assetType == AssetType.Fluid ? fluidLevel : Byte.MIN_VALUE;
    }

    static {
        CODEC = BuilderCodec.builder(PlaceableAsset.class, PlaceableAsset::new)
                .appendInherited(new KeyedCodec<>("AssetType", new EnumCodec<>(AssetType.class)),
                        (o, v) -> o.assetType = v,
                        o -> o.assetType,
                        (o, p) -> o.assetType = p.assetType)
                .documentation("Defines what type of asset gets placed.")
                .add()

                .appendInherited(new KeyedCodec<>("AssetId", Codec.STRING),
                        (o, v) -> o.assetID = v,
                        o -> o.assetID,
                        (o, p) -> o.assetID = p.assetID)
                .documentation("The asset (block or fluid) to place when a collision occurs.")
                .add()

                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING),
                        (o, v) -> o.blockState = v,
                        o -> o.blockState,
                        (o, p) -> o.blockState = p.blockState)
                .documentation("The block state of the block that gets placed.")
                .add()
                .appendInherited(new KeyedCodec<>("FluidLevel", Codec.BYTE),
                        (o, v) -> o.fluidLevel = v,
                        o -> o.fluidLevel,
                        (o, p) -> o.fluidLevel = p.fluidLevel)
                .documentation("The fluid level of the fluid that gets placed.")
                .add()
                .appendInherited(new KeyedCodec<>("PlaceFluid", Codec.BOOLEAN), (o, v) -> o.placeFluid = v, (o) -> o.placeFluid, (o, p) -> o.placeFluid = p.placeFluid).documentation("Whether to still place the fluid on collision")
                .add()

                .metadata(new AssetVariantSchemaMetadata())

                .afterDecode((asset, _) -> {
                    asset.id = Integer.MIN_VALUE;
                    if (asset.assetType != AssetType.Block) {
                        asset.blockState = "";
                    }
                    if (asset.assetType != AssetType.Fluid) {
                        asset.fluidLevel = Byte.MIN_VALUE;
                    }
                })

                .build();
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
            Schema fluidLevelSchema = originalProps.get("FluidLevel");

            if (assetTypeSchema == null || assetIdSchema == null || blockStateSchema == null || fluidLevelSchema == null) {
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
            fluidProps.put("FluidLevel", fluidLevelSchema);
            fluidSchema.setProperties(fluidProps);
            fluidSchema.setRequired("AssetType");

            root.setProperties(new LinkedHashMap<>());
            root.setAnyOf(blockSchema, fluidSchema);
            Objects.requireNonNull(root.getHytale()).setMergesProperties(true);
            root.setHytaleSchemaTypeField(new Schema.SchemaTypeField("AssetType", "Block", "Block", "Fluid"));
        }
    }
}