package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.science.ScienceContext;
import com.COLLABOMOD.collabomod.world.cardinal.WorldCardinalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ThermalProcessor {
    public void process(Level level, BlockPos center, ScienceContext ctx, LivingEntity caster) {
        float temp = ctx.temperature;
        float radius = ctx.radius;

        int range = (int) Math.ceil(radius);
        float innerRadius = Math.max(0, radius - 2.0F);
        float innerSq = innerRadius * innerRadius;
        float outerSq = radius * radius;

        WorldCardinalSystem cardinal = null;
        if (level instanceof ServerLevel serverLevel) {
            cardinal = WorldCardinalSystem.get(serverLevel);
        }

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x*x + y*y + z*z;
                    if (distSq > outerSq || distSq < innerSq) continue;

                    BlockPos pos = center.offset(x, y, z);

                    if (cardinal != null && caster != null) {
                        float diff = temp - 300.0F;
                        cardinal.addTemperature(level, pos, diff);
                    }

                    // ■■■ 即時物理効果 ■■■
                    BlockState state = level.getBlockState(pos);

                    // 1000度超え
                    if (temp > 1000.0F) {
                        // 草などを焼却
                        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                        // ★修正: 水の即時蒸発
                        else if (state.is(Blocks.WATER)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            if (level instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.CLOUD, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, 1, 0.2, 0.2, 0.2, 0.05);
                            }
                        }
                    }
                }
            }
        }
    }
}
