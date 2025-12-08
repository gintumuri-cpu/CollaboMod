package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ThermalProcessor {
    public void process(Level level, BlockPos center, ScienceContext ctx) {
        float temp = ctx.temperature;
        float radius = ctx.radius;
        int range = (int) Math.ceil(radius);

        // 範囲内のブロックをスキャン
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    // 簡易距離チェック
                    if (pos.distSqr(center) > radius * radius) continue;

                    BlockState state = level.getBlockState(pos);

                    // ■ 高温処理 (> 1000K)
                    if (temp > 1000.0F) {
                        // 水 -> 蒸発 (空気)
                        if (state.is(Blocks.WATER)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                        // 氷/雪 -> 水
                        else if (state.is(Blocks.ICE) || state.is(Blocks.SNOW)) {
                            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                        }
                        // 可燃物 -> 火 (簡易的に空気ブロックの上なら火をつける)
                        else if (state.isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                            if (level.random.nextFloat() < 0.1F) { // 確率で着火
                                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        }
                    }

                    // ■ 低温処理 (< 200K)
                    else if (temp < 200.0F) {
                        // 水 -> 氷
                        if (state.is(Blocks.WATER)) {
                            level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                        }
                        // 溶岩 -> 黒曜石/石
                        else if (state.is(Blocks.LAVA)) {
                            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }
}
