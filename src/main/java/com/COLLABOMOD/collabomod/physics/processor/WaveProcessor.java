package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class WaveProcessor {
    public void process(Level level, BlockPos center, ScienceContext ctx, LivingEntity caster, LivingEntity target) {
//        Level level = ctx.level;
//        Vec3 origin = ctx.origin;
//        ScienceContext science = ctx.science;
        float radius = ctx.radius;
        Vec3 origin = Vec3.atCenterOf(center);
        AABB area = new AABB(new BlockPos(center)).inflate(radius);

        // ■ 1. 防御・干渉 (Defense / Jamming)
        // 防御成分(compShield)がある場合
        if (ctx.compShield > 0.0F) {
            List<Entity> entities = level.getEntities(null, area);
            for (Entity e : entities) {
                if (e instanceof EntityMagicSequence) e.discard();
                if (e instanceof Projectile) e.discard();

                if (e instanceof LivingEntity living) {
                    // ■ 術者またはターゲット（味方）なら防御バフ
                    if (e == caster || e == target) {
                        int duration = (int)(ctx.energy * 2);
                        if (duration < 60) duration = 60;
                        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
                        if (ctx.temperature < 200.0F) {
                            living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
                        }
                    }
                }
            }
        }

        // ■ 2. 透過・共鳴 (Resonance) - 攻撃的波動
        if (ctx.compWave > 0.8F) {
            List<Entity> targets = level.getEntities(null, area);
            for (Entity e : targets) {
                // ■ 術者はダメージを受けない
                if (e instanceof LivingEntity && e != caster) {
                    if (e.distanceToSqr(center.getX(), center.getY(), center.getZ()) > radius * radius) continue;
                    float damage = ctx.energy * 0.1F;
                    e.hurt(new DamageSource("magic").bypassArmor().setMagic(), damage);
                }
            }
        }
    }
}
