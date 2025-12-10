package com.COLLABOMOD.collabomod.physics;

import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.physics.processor.KineticProcessor;
import com.COLLABOMOD.collabomod.physics.processor.StructuralProcessor;
import com.COLLABOMOD.collabomod.physics.processor.ThermalProcessor;
import com.COLLABOMOD.collabomod.physics.processor.WaveProcessor;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
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
    public static void applyPhysics(Level level, Vec3 origin, ScienceContext ctx, LivingEntity caster, LivingEntity target) {
        if (level.isClientSide) return;

        //Level level = ctx.level;
        //Vec3 origin = ctx.origin;
        //ScienceContext science = ctx.science;
        BlockPos center = new BlockPos(origin);

        // 1. 熱力学 (温度)
        if (Math.abs(ctx.temperature - 300.0F) > 50.0F) {
            thermal.process(level, center, ctx);
        }

        // 2. 構造力学 (分解)
        if (ctx.compMatter > 0.0F) {
            structural.process(level, center, ctx);
        }

        // 3. 運動力学 (移動・衝撃・加重)
        // 速度、質量、またはエネルギーが高い場合に発動
        if (ctx.velocity > 1.0F || ctx.mass > 0.0F || ctx.energy > 10.0F) {
            kinetic.process(level, center, ctx, caster, target);
        }

        // 4. 波動 (防御・干渉)
        // 振動、光、または防御成分がある場合に発動
        if (ctx.compWave > 0.0F || ctx.compLight > 0.0F || ctx.compShield > 0.0F) {
            wave.process(level, center, ctx, caster, target);
        }
    }
}
