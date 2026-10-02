package dev.gumark.oresim;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single simulated ore placement, mirroring the vanilla placed-feature it was
 * built from (count, height range, rarity, vein size, air-exposure discard, ...).
 */
public class Ore {
    public int step;
    public int index;
    public IntProvider count = ConstantInt.of(1);
    public HeightProvider heightProvider;
    public WorldGenerationContext heightContext;
    public float rarity = 1;
    public float discardOnAirChance;
    public int size;
    public OreType type;
    public boolean scattered;
    public List<BlockReplacement> targets;

    private Ore(PlacedFeature feature, int step, int index, OreType type) {
        this.step = step;
        this.index = index;
        this.type = type;

        int bottom = Minecraft.getInstance().level.getMinY();
        int height = Minecraft.getInstance().level.dimensionType().logicalHeight();
        this.heightContext = new WorldGenerationContext(null, LevelHeightAccessor.create(bottom, height));

        for (PlacementModifier modifier : feature.placement()) {
            if (modifier instanceof CountPlacement countPlacement) {
                this.count = countPlacement.count();
            } else if (modifier instanceof HeightRangePlacement heightRangePlacement) {
                this.heightProvider = heightRangePlacement.height();
            } else if (modifier instanceof RarityFilter rarityFilter) {
                this.rarity = rarityFilter.chance();
            }
        }

        Feature oreFeature = feature.feature().value();
        if (oreFeature instanceof AbstractOreFeature abstractOreFeature) {
            this.discardOnAirChance = abstractOreFeature.discardChanceOnAirExposure();
            this.size = abstractOreFeature.size();
            this.targets = abstractOreFeature.targetStates();
        } else {
            throw new IllegalStateException("config for " + feature + " is not an ore feature");
        }

        if (oreFeature instanceof ScatteredOreFeature) {
            this.scattered = true;
        }
    }

    private static void registerOre(
        Map<PlacedFeature, Ore> map,
        List<FeatureSorter.StepFeatureData> indexer,
        HolderLookup.RegistryLookup<PlacedFeature> oreRegistry,
        ResourceKey<PlacedFeature> oreKey,
        int genStep,
        OreType type
    ) {
        PlacedFeature orePlacement = oreRegistry.getOrThrow(oreKey).value();
        int index = indexer.get(genStep).indexMapping().applyAsInt(orePlacement);
        map.put(orePlacement, new Ore(orePlacement, genStep, index, type));
    }

    /** Builds the biome &rarr; ores map for the given dimension from vanilla's placed features. */
    public static Map<ResourceKey<Biome>, List<Ore>> getRegistry(ResourceKey<Level> dimension) {
        HolderLookup.Provider registry = VanillaRegistries.createWorldLookup();
        HolderLookup.RegistryLookup<PlacedFeature> features = registry.lookupOrThrow(Registries.PLACED_FEATURE);
        WorldPreset worldPreset = registry.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value();
        WorldDimensions dimensions = worldPreset.createWorldDimensions();

        ResourceKey<LevelStem> stemKey;
        if (dimension == Level.NETHER) stemKey = LevelStem.NETHER;
        else if (dimension == Level.END) stemKey = LevelStem.END;
        else stemKey = LevelStem.OVERWORLD;

        LevelStem stem = dimensions.get(stemKey).orElseThrow();

        List<Holder<Biome>> biomeList = stem.generator().getBiomeSource().possibleBiomes().stream().toList();
        List<FeatureSorter.StepFeatureData> indexer = FeatureSorter.buildFeaturesPerStep(
            biomeList, biomeEntry -> biomeEntry.value().getGenerationSettings().features(), true
        );

        Map<PlacedFeature, Ore> featureToOre = new HashMap<>();
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COAL_LOWER, 6, OreType.COAL);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COAL_UPPER, 6, OreType.COAL);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_MIDDLE, 6, OreType.IRON);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_SMALL, 6, OreType.IRON);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_IRON_UPPER, 6, OreType.IRON);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD, 6, OreType.GOLD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_LOWER, 6, OreType.GOLD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_EXTRA, 6, OreType.GOLD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_NETHER, 7, OreType.GOLD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_GOLD_DELTAS, 7, OreType.GOLD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_REDSTONE, 6, OreType.REDSTONE);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_REDSTONE_LOWER, 6, OreType.REDSTONE);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND, 6, OreType.DIAMOND);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_BURIED, 6, OreType.DIAMOND);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_LARGE, 6, OreType.DIAMOND);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_DIAMOND_MEDIUM, 6, OreType.DIAMOND);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_LAPIS, 6, OreType.LAPIS);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_LAPIS_BURIED, 6, OreType.LAPIS);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COPPER, 6, OreType.COPPER);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_COPPER_LARGE, 6, OreType.COPPER);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_EMERALD, 6, OreType.EMERALD);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_QUARTZ_NETHER, 7, OreType.QUARTZ);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_QUARTZ_DELTAS, 7, OreType.QUARTZ);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_ANCIENT_DEBRIS_SMALL, 7, OreType.DEBRIS);
        registerOre(featureToOre, indexer, features, OrePlacements.ORE_ANCIENT_DEBRIS_LARGE, 7, OreType.DEBRIS);

        Map<ResourceKey<Biome>, List<Ore>> biomeOreMap = new HashMap<>();
        biomeList.forEach(biome -> {
            ResourceKey<Biome> biomeKey = biome.unwrapKey().orElseThrow();
            List<Ore> ores = new ArrayList<>();
            biomeOreMap.put(biomeKey, ores);
            biome.value().getGenerationSettings().features().stream()
                .flatMap(HolderSet::stream)
                .map(Holder::value)
                .filter(featureToOre::containsKey)
                .forEach(feature -> ores.add(featureToOre.get(feature)));
        });
        return biomeOreMap;
    }
}
