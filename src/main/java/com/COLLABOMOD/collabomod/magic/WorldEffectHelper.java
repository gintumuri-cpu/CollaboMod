package com.COLLABOMOD.collabomod.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 魔法の物理パラメータに基づいてワールドのブロックに影響を与えるユーティリティ。
 * 保護ブロック（黒曜石、基盤岩など）は変更対象から除外する。
 */
public class WorldEffectHelper {

    /**
     * 高温によるブロック変化 (温度 > 1500K)
     * - 可燃ブロック → 着火
     * - 氷 → 水
     * - 石系 → 溶岩
     */
    public static void applyHeatEffect(Level level, BlockPos center, float radius, float temperature) {
        if (level.isClientSide)
            return;
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (center.distSqr(pos) > radius * radius)
                        continue;
                    if (isProtected(level, pos))
                        continue;

                    BlockState state = level.getBlockState(pos);

                    // 可燃ブロックの着火
                    if (state.isFlammable(level, pos, net.minecraft.core.Direction.UP)) {
                        BlockPos above = pos.above();
                        if (level.isEmptyBlock(above)) {
                            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }

                    // 氷・雪 → 水
                    if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
                    }
                    if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.SNOW)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }

                    // 超高温: 石系 → 溶岩
                    if (temperature > 2000.0f) {
                        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                                || state.is(Blocks.DEEPSLATE)) {
                            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * 低温によるブロック変化 (温度 < 100K)
     * - 水 → 氷
     * - 溶岩 → 黒曜石
     * - 周囲に霜ブロック (雪層)
     */
    public static void applyFreezeEffect(Level level, BlockPos center, float radius) {
        if (level.isClientSide)
            return;
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (center.distSqr(pos) > radius * radius)
                        continue;
                    if (isProtected(level, pos))
                        continue;

                    BlockState state = level.getBlockState(pos);

                    if (state.is(Blocks.WATER)) {
                        level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
                    }
                    if (state.is(Blocks.LAVA)) {
                        level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                    }
                    if (state.is(Blocks.FIRE)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }

                    // 地面の上に雪
                    if (state.isSolidRender(level, pos)) {
                        BlockPos above = pos.above();
                        if (level.isEmptyBlock(above)) {
                            level.setBlock(above, Blocks.SNOW.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * 爆発によるブロック破壊 (エネルギー > 100 + RADIAL)
     */
    public static void applyExplosiveEffect(Level level, BlockPos center, float radius, float energy) {
        if (level.isClientSide)
            return;
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    float distSq = (float) center.distSqr(pos);
                    if (distSq > radius * radius)
                        continue;
                    if (isProtected(level, pos))
                        continue;

                    BlockState state = level.getBlockState(pos);
                    if (state.isAir())
                        continue;

                    // 距離に応じた破壊確率（中心に近いほど確実）
                    float dist = (float) Math.sqrt(distSq);
                    float breakChance = 1.0f - (dist / radius);
                    if (level.random.nextFloat() < breakChance) {
                        level.destroyBlock(pos, true);
                    }
                }
            }
        }
    }

    /**
     * ビーム貫通破壊 (DIRECTIONAL + 高速度)
     */
    public static void applyBeamEffect(Level level, Vec3 origin, Vec3 direction, float length, float energy) {
        if (level.isClientSide)
            return;
        Vec3 dir = direction.normalize();
        int steps = (int) (length * 2);
        for (int i = 0; i < steps; i++) {
            Vec3 point = origin.add(dir.scale(i * 0.5));
            BlockPos pos = new BlockPos(point);
            if (isProtected(level, pos))
                continue;

            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                // 硬度チェック: エネルギーが十分なら貫通
                float hardness = state.getDestroySpeed(level, pos);
                if (hardness >= 0 && hardness < energy * 0.2f) {
                    level.destroyBlock(pos, true);
                }
            }
        }
    }

    /**
     * バリアブロック一時設置 (FIELD + isSolid)
     */
    public static void applyBarrierEffect(Level level, BlockPos center, float radius) {
        if (level.isClientSide)
            return;
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                // 球殻のみ
                float distSq = dx * dx + dz * dz;
                float rInner = (radius - 0.8f) * (radius - 0.8f);
                if (distSq > radius * radius || distSq < rInner)
                    continue;

                for (int dy = 0; dy <= (int) radius; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.isEmptyBlock(pos)) {
                        // バリアブロック (インビジブル) を設置
                        level.setBlock(pos, Blocks.BARRIER.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    /**
     * 保護ブロック判定 — 変更してはいけないブロック
     */
    private static boolean isProtected(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // 基盤岩、エンドポータルフレーム、コマンドブロック等は保護
        if (state.is(Blocks.BEDROCK))
            return true;
        if (state.is(Blocks.END_PORTAL_FRAME))
            return true;
        if (state.is(Blocks.COMMAND_BLOCK))
            return true;
        if (state.is(Blocks.CHAIN_COMMAND_BLOCK))
            return true;
        if (state.is(Blocks.REPEATING_COMMAND_BLOCK))
            return true;
        if (state.is(Blocks.BARRIER))
            return true;
        if (state.is(Blocks.STRUCTURE_BLOCK))
            return true;
        // 破壊不能ブロック
        if (state.getDestroySpeed(level, pos) < 0)
            return true;
        return false;
    }
}
