package de.Roboter007.interactiveFluids.ticker.collision.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;

import javax.annotation.Nonnull;
import java.util.*;

public class CollisionResultConfig {
    public static final BuilderCodec<CollisionResultConfig> CODEC;

    protected ResultAssetType assetType = ResultAssetType.Block;
    protected String assetID = "Empty";
    private int id = Integer.MIN_VALUE;

    protected String blockState = "";
    protected Map<String, String> blockProperties = Collections.emptyMap();

    protected byte fluidLevel = Byte.MIN_VALUE;

    public ResultAssetType getAssetType() {
        return assetType;
    }

    public String getAssetID() {
        return assetID;
    }

    public String getBlockState() {
        return assetType == ResultAssetType.Block ? blockState : "";
    }

    public byte getFluidLevel() {
        return assetType == ResultAssetType.Fluid ? fluidLevel : Byte.MIN_VALUE;
    }

    @Nonnull
    public Map<String, String> getBlockProperties() {
        if (assetType != ResultAssetType.Block) {
            return Collections.emptyMap();
        }
        return blockProperties != null ? blockProperties : Collections.emptyMap();
    }

    public String getBlockToPlace() {
        return assetID;
    }

    static {
        CODEC = BuilderCodec.builder(CollisionResultConfig.class, CollisionResultConfig::new)
                .appendInherited(new KeyedCodec<>("AssetType", new EnumCodec<>(ResultAssetType.class)), (o, v) -> o.assetType = v, o -> o.assetType, (o, p) -> o.assetType = p.assetType).documentation("Defines what type of asset gets placed.").add()
                .appendInherited(new KeyedCodec<>("AssetId", Codec.STRING), (o, v) -> o.assetID = v, o -> o.assetID, (o, p) -> o.assetID = p.assetID).documentation("The asset (block or fluid) to place when a collision occurs.").add()
                .appendInherited(new KeyedCodec<>("BlockState", Codec.STRING), (o, v) -> o.blockState = v, o -> o.blockState, (o, p) -> o.blockState = p.blockState).documentation("The block state of the block that gets placed.").add()
                .appendInherited(new KeyedCodec<>("FluidLevel", Codec.BYTE), (o, v) -> o.fluidLevel = v, o -> o.fluidLevel, (o, p) -> o.fluidLevel = p.fluidLevel).documentation("The fluid level of the fluid that gets placed.").add()
                .appendInherited(new KeyedCodec<>("BlockProperties", new MapCodec<>(Codec.STRING, HashMap::new)), (o, v) -> o.blockProperties = v, o -> o.blockProperties, (o, p) -> o.blockProperties = p.blockProperties).documentation("Optional block-specific properties passed to a registered CollisionHook by an addon").add()
                .metadata(new AssetVariantSchemaMetadata())

                .afterDecode((asset, _) -> {
                    asset.id = Integer.MIN_VALUE;
                    if (asset.assetType != ResultAssetType.Block) {
                        asset.blockState = "";
                    }
                    if (asset.assetType != ResultAssetType.Fluid) {
                        asset.fluidLevel = Byte.MIN_VALUE;
                    }
                    if (asset.blockProperties == null) {
                        asset.blockProperties = Collections.emptyMap();
                    }
                })
                .build();

    }

    @Override
    public String toString() {
        return "CollisionResultConfig{" +
                "assetType=" + assetType +
                ", assetID='" + assetID + '\'' +
                ", id=" + id +
                ", blockState='" + blockState + '\'' +
                ", fluidLevel=" + fluidLevel +
                '}';
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
            Schema blockPropsSchema = originalProps.get("BlockProperties");

            if (assetTypeSchema == null || assetIdSchema == null || blockStateSchema == null || fluidLevelSchema == null || blockPropsSchema == null) {
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
            blockProps.put("BlockProperties", blockPropsSchema);
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

    public enum ResultAssetType {
        Block,
        Fluid
    }
}
