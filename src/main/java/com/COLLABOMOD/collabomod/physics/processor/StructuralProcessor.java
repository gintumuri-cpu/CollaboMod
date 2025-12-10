package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.science.ScienceContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StructuralProcessor {

    public void process(Level level, BlockPos center, ScienceContext ctx) {
        float decompositionPower = ctx.compMatter * ctx.energy;
        float radius = ctx.radius;
        // 処理範囲の最適化: 半径が大きすぎる場合は処理を間引くなどの対策も可能ですが、
        // まずは「殻（Shell）」のみを処理するようにします。

        int range = (int) Math.ceil(radius);
        float innerRadius = Math.max(0, radius - 2.0F); // 殻の厚み2ブロック分
        float innerSq = innerRadius * innerRadius;
        float outerSq = radius * radius;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x*x + y*y + z*z;

                    // ■ 最適化: 「範囲外」または「処理済みの内側」ならスキップ
                    if (distSq > outerSq || distSq < innerSq) continue;

                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;

                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) continue;

                    if (decompositionPower > hardness * 5.0F || decompositionPower > 1000.0F) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
    }
}
