package dev.gumark.oresim.compat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.gumark.oresim.Target;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.AbstractOreFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Version-specific API access for Minecraft 26.3 (placement modifiers are
 * records and {@link AbstractOreFeature} carries its own configuration).
 */
public final class Compat {
    private Compat() {
    }

    /** Ore parameters read from a vanilla placed feature. */
    public record OreData(float discardOnAirChance, int size, List<Target> targets, boolean scattered) {
    }

    public static OreData oreData(PlacedFeature feature) {
        Feature oreFeature = feature.feature().value();
        if (oreFeature instanceof AbstractOreFeature abstractOreFeature) {
            List<Target> targets = new ArrayList<>();
            abstractOreFeature.targetStates()
                .forEach(replacement -> targets.add(new Target(replacement.target()::test, replacement.state())));
            return new OreData(
                abstractOreFeature.discardChanceOnAirExposure(),
                abstractOreFeature.size(),
                targets,
                oreFeature instanceof ScatteredOreFeature
            );
        }
        throw new IllegalStateException("config for " + feature + " is not an ore feature");
    }

    public static IntProvider countOf(CountPlacement placement) {
        return placement.count();
    }

    public static HeightProvider heightOf(HeightRangePlacement placement) {
        return placement.height();
    }

    public static float chanceOf(RarityFilter filter) {
        return filter.chance();
    }

    public static HolderLookup.Provider worldLookup() {
        return VanillaRegistries.createWorldLookup();
    }

    public static InputConstants.Type keyboardType() {
        return InputConstants.Type.KEYBOARD;
    }
}
