package com.COLLABOMOD.collabomod.physics.processor;

import com.COLLABOMOD.collabomod.science.ScienceContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class KineticProcessor {

    public void process(Level level, BlockPos center, ScienceContext ctx) {
        // エネルギーを衝撃力に変換
        float force = ctx.energy * 0.1F;
        float radius = ctx.radius * 2.0F; // 影響範囲は広めに

        AABB area = new AABB(center).inflate(radius);
        List<Entity> entities = level.getEntities(null, area);

        for (Entity e : entities) {
            if (e instanceof LivingEntity) {
                double dist = e.distanceToSqr(center.getX(), center.getY(), center.getZ());
                double distSqrt = Math.sqrt(dist);

                if (distSqrt < radius) {
                    // 中心からのベクトル
                    Vec3 dir = e.position().subtract(center.getX(), center.getY(), center.getZ()).normalize();

                    // 距離減衰
                    double power = force * (1.0 - (distSqrt / radius));

                    // ノックバック適用
                    e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(power)));
                    e.hurtMarked = true;
                }
            }
        }
    }
}
