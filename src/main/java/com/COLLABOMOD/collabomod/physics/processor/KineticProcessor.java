package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.science.ScienceContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KineticProcessor {

    public void process(Level level, BlockPos center, ScienceContext ctx, LivingEntity caster, LivingEntity target) {
//        Level level = ctx.level;
//        Vec3 origin = Vec3.atCenterOf(center);
//        ScienceContext science = ctx.science;

        float radius = ctx.radius;
        Vec3 origin = Vec3.atCenterOf(center);
        AABB area = new AABB(center).inflate(radius);
        List<Entity> entities = level.getEntities(null, area);

        // ■ 1. 加速・移動 (Acceleration)
        // 速度ベクトル(velocity)が高い場合
        if (ctx.velocity > 1.5F) {
            for (Entity e : entities) {
                if (target != null && e != target) continue;
                // 方向ベクトルがないため、簡易的に中心からの放射ベクトルを使用
                Vec3 accel = e.position().subtract(origin).normalize().scale(ctx.velocity * 0.5);
                e.setDeltaMovement(e.getDeltaMovement().add(accel));
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
        }

        // ■ 2. 加重・重力制御 (Weight)
        // 質量(mass)が付与されている場合
        if (ctx.mass > 0.0F) {
            for (Entity e : entities) {
                if (e == caster) continue;
                if (e instanceof LivingEntity living) {
                    double gravity = ctx.mass * 0.05;
                    e.setDeltaMovement(e.getDeltaMovement().add(0, -gravity, 0));
                    int amplifier = (int)(ctx.mass / 20.0F);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, amplifier));
                    living.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128));
                }
            }
        }

        // ■ 3. 衝撃波 (Shockwave)
        // 爆発エネルギーによる吹き飛ばし
        if (ctx.energy > 50.0F) {
            for (Entity e : entities) {
                if (e instanceof LivingEntity && e != caster) {
                    double dist = e.distanceToSqr(origin);
                    if (dist < radius * radius) {
                        Vec3 dir = e.position().subtract(origin).normalize();
                        double power = (ctx.energy * 0.05) * (1.0 - (Math.sqrt(dist) / radius));
                        e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(power)));
                        e.hurtMarked = true;
                    }
                }
            }

            // 脆いブロックの破壊
            if (ctx.energy > 200.0F) {
                int r = (int) Math.ceil(radius);
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = center.offset(x, y, z);
                            if (pos.distSqr(center) > radius * radius) continue;
                            BlockState state = level.getBlockState(pos);
                            float hardness = state.getDestroySpeed(level, pos);
                            if (hardness >= 0.0F && hardness <= 0.3F) {
                                level.destroyBlock(pos, true);
                            }
                        }
                    }
                }
            }
        }
    }
}
