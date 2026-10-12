package dev.gumark.oresim;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A single ore replacement target: vanilla {@code BlockReplacement} on 26.3,
 * {@code OreConfiguration.TargetBlockState} on 26.1/26.2. Wrapped in a mod type
 * so the shared simulation code compiles against every supported generation.
 */
public record Target(TargetTest test, BlockState state) {
    @FunctionalInterface
    public interface TargetTest {
        boolean test(BlockState state, BlockPos pos, RandomSource random);
    }
}
