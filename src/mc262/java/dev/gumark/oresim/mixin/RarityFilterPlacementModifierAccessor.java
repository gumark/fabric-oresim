package dev.gumark.oresim.mixin;

import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the configured rarity from {@link RarityFilter}. */
@Mixin(RarityFilter.class)
public interface RarityFilterPlacementModifierAccessor {
    @Accessor("chance")
    int oresim$chance();
}
