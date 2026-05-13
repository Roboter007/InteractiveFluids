package de.Roboter007.interactiveFluids.ticker.collision.block;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.schema.metadata.Metadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CollisionConfigEntry {
    public static final BuilderCodec<CollisionConfigEntry> CODEC;

    private CollisionSourceConfig sourceConfig;
    private CollisionResultConfig resultConfig;


    public CollisionConfigEntry(CollisionSourceConfig conditionConfig, CollisionResultConfig resultConfig) {
        this.sourceConfig = conditionConfig;
        this.resultConfig = resultConfig;
    }

    public CollisionConfigEntry() {
        this(new CollisionSourceConfig(), new CollisionResultConfig());
    }

    public CollisionSourceConfig getSourceConfig() {
        return sourceConfig;
    }

    public CollisionResultConfig getResultConfig() {
        return resultConfig;
    }

    static {
        CODEC = BuilderCodec.builder(CollisionConfigEntry.class, CollisionConfigEntry::new)
                .appendInherited(new KeyedCodec<>("Source", CollisionSourceConfig.CODEC), (config, o) -> config.sourceConfig = o, (config) -> config.sourceConfig, (config, parent) -> config.sourceConfig = parent.sourceConfig).documentation("Expand to configure more. Defines the condition that has to exist in order for the conversion to happen.").add()
                .appendInherited(new KeyedCodec<>("Result", CollisionResultConfig.CODEC), (config, o) -> config.resultConfig = o, (config) -> config.resultConfig, (config, parent) -> config.resultConfig = parent.resultConfig).documentation("Expand to configure more. Defines the result that.").add()
                .metadata(new EntryVariantSchemaMetadata())
                .build();
    }

    private static final class EntryVariantSchemaMetadata implements Metadata {
        @Override
        public void modify(Schema schema) {
            if (!(schema instanceof ObjectSchema root)) {
                return;
            }

            Map<String, Schema> rootProps = root.getProperties();
            if (rootProps == null) {
                return;
            }

            Schema sourceSchema = rootProps.get("Source");
            Schema resultSchema = rootProps.get("Result");
            if (!(sourceSchema instanceof ObjectSchema sourceObj) || !(resultSchema instanceof ObjectSchema resultObj)) {
                return;
            }

            ObjectSchema sourceFluidCond = new ObjectSchema();
            sourceFluidCond.setProperties(Map.of(
                    "AssetType", StringSchema.constant("Fluid")
            ));
            sourceFluidCond.setRequired("AssetType");

            ObjectSchema resultBlockCond = new ObjectSchema();
            resultBlockCond.setProperties(Map.of(
                    "AssetType", StringSchema.constant("Block")
            ));
            resultBlockCond.setRequired("AssetType");

            ObjectSchema ifSchema = new ObjectSchema();
            ifSchema.setProperties(Map.of(
                    "Source", sourceFluidCond,
                    "Result", resultBlockCond
            ));

            // Variante A: Standard
            ObjectSchema thenSchema = copyRoot(root);
            // hier bleibt PlaceFluid sichtbar, wenn du es im PlaceableAsset-Codec vorhanden lässt

            // Variante B: Default ohne Sonderfall
            ObjectSchema elseSchema = copyRoot(root);

            root.setIf(ifSchema);
            root.setThen(thenSchema);
            root.setElse(elseSchema);
        }

        private ObjectSchema copyRoot(ObjectSchema original) {
            ObjectSchema copy = new ObjectSchema();
            copy.setAdditionalProperties(false);
            copy.setTitle(original.getTitle());
            Objects.requireNonNull(copy.getHytale()).setMergesProperties(true);

            Map<String, Schema> props = original.getProperties();
            if (props != null) {
                copy.setProperties(new LinkedHashMap<>(props));
            }

            return copy;
        }
    }
}
