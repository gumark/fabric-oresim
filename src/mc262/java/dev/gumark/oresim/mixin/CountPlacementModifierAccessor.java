package dev.gumark.oresim.mixin;

import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the configured repeat count from {@link CountPlacement}. */
@Mixin(CountPlacement.class)
public interface CountPlacementModifierAccessor {
    @Accessor("count")
    IntProvider oresim$count();
}
