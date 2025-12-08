package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.science.ScienceContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StructuralProcessor {

    public void process(Level level, BlockPos center, ScienceContext ctx) {
        // ■ 修正: composition_matter -> compMatter
        float decompositionPower = ctx.compMatter * ctx.energy;
        float radius = ctx.radius;
        int range = (int) Math.ceil(radius);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.distSqr(center) > radius * radius) continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) continue; // 岩盤は破壊不可

                    if (decompositionPower > hardness * 5.0F || decompositionPower > 1000.0F) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
