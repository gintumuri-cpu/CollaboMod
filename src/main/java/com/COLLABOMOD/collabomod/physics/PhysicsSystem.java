package com.COLLABOMOD.collabomod.physics;

import com.COLLABOMOD.collabomod.physics.processor.KineticProcessor;
import com.COLLABOMOD.collabomod.physics.processor.StructuralProcessor;
import com.COLLABOMOD.collabomod.physics.processor.ThermalProcessor;
import com.COLLABOMOD.collabomod.physics.processor.WaveProcessor;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PhysicsSystem {
    private static final KineticProcessor kinetic = new KineticProcessor();
    private static final ThermalProcessor thermal = new ThermalProcessor();
    private static final StructuralProcessor structural = new StructuralProcessor();
    private static final WaveProcessor wave = new WaveProcessor();

    /**
     * 科学パラメータ(ctx)に基づいて、物理現象を世界に適用する
     */
    public static void applyPhysics(Level level, Vec3 origin, ScienceContext ctx) {
        if (level.isClientSide) return; // 物理干渉はサーバーのみ

        BlockPos center = new BlockPos(origin);

        // 1. 熱力学処理 (温度変化)
        // 常温(300K)から大きく離れている場合のみ実行
        if (Math.abs(ctx.temperature - 300.0F) > 50.0F) {
            thermal.process(level, center, ctx);
        }

        // 2. 構造力学処理 (分解・破壊)
        // 物質分解成分が含まれている場合
        if (ctx.compMatter > 0.0F) {
            structural.process(level, center, ctx);
        }

        // 3. 運動力学処理 (爆風・衝撃)
        // 振動成分やエネルギーが高い場合
        if (ctx.compWave > 0.0F || ctx.energy > 10.0F) {
            kinetic.process(level, center, ctx);
        }

        // 4. 波動処理 (干渉・透過)
        // 今回は簡易的に、振動成分があれば波動処理も呼ぶ
        if (ctx.compWave > 0.0F) {
            wave.process(level, center, ctx);
        }
    }
}
