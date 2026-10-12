package dev.gumark.oresim.mixin;

import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the configured height provider from {@link HeightRangePlacement}. */
@Mixin(HeightRangePlacement.class)
public interface HeightRangePlacementModifierAccessor {
    @Accessor("height")
    HeightProvider oresim$height();
}
