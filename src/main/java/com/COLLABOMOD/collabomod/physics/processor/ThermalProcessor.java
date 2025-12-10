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

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.distSqr(center) > radius * radius) continue;

                    BlockState state = level.getBlockState(pos);

                    // ■ 高温処理 (> 1000K)
                    if (temp > 1000.0F) {
                        if (state.is(Blocks.WATER)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        } else if (state.is(Blocks.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)) {
                            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                        }
                        // ■ 追加: 草や花を焼き払う
                        else if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                        // 可燃物への着火
                        else if (state.isAir() && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                            if (level.random.nextFloat() < 0.1F) {
                                level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                        }
                    }
                    // ■ 低温処理 (< 200K)
                    else if (temp < 200.0F) {
                        if (state.is(Blocks.WATER)) {
                            level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                        } else if (state.is(Blocks.LAVA)) {
                            level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }
}
