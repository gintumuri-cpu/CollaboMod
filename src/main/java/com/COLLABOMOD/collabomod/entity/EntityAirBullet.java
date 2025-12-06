package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.register.EntityRegister;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class EntityAirBullet extends ThrowableProjectile {

    public EntityAirBullet(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public EntityAirBullet(Level level, LivingEntity shooter) {
        super(EntityRegister.AIR_BULLET.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();

        // 演出: 空気の歪み（CLOUDやSWEEP_ATTACK）
        if (this.level.isClientSide) {
            for (int i = 0; i < 2; i++) {
                this.level.addParticle(ParticleTypes.CLOUD,
                        this.getX(), this.getY(), this.getZ(),
                        0, 0, 0);
            }
        }

        if (this.tickCount > 40) this.discard(); // 短射程
    }

    @Override
    protected float getGravity() { return 0.01F; } // 空気弾なので少し落ちる

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        // 物理ダメージ + 強ノックバック
        float damage = 6.0F;
        target.hurt(DamageSource.thrown(this, this.getOwner()), damage);

        // 爆裂（衝撃）
        Vec3 vec = this.getDeltaMovement().multiply(1, 0, 1).normalize().scale(2.0);
        target.push(vec.x, 0.5, vec.z);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
