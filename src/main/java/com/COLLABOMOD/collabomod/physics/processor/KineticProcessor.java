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
        float radius = ctx.radius;
        Vec3 origin = Vec3.atCenterOf(center);
        AABB area = new AABB(center).inflate(radius);
        List<Entity> entities = level.getEntities(null, area);

        // 1. 加速 (Accel)
        if (ctx.velocity > 1.5F) {
            for (Entity e : entities) {
                if (target != null && e != target) continue;
                Vec3 accel = e.position().subtract(origin).normalize().scale(ctx.velocity * 0.5);
                e.setDeltaMovement(e.getDeltaMovement().add(accel));
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
        }

        // 2. 加重 (Weight)
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

        // 3. 衝撃波 (Shockwave)
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

            // ブロック破壊 (これも殻のみにする)
            if (ctx.energy > 200.0F) {
                int r = (int) Math.ceil(radius);
                float innerRadius = Math.max(0, radius - 1.5F);
                float innerSq = innerRadius * innerRadius;
                float outerSq = radius * radius;

                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            double distSq = x*x + y*y + z*z;
                            if (distSq > outerSq || distSq < innerSq) continue; // ■ 最適化

                            BlockPos pos = center.offset(x, y, z);
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
