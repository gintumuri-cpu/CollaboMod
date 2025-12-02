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
import net.minecraftforge.network.NetworkHooks;

public class EntityGramDemolition extends ThrowableProjectile {
    private float damageMultiplier = 1.0F;
    public EntityGramDemolition(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public void setDamageMultiplier(float mul) {
        this.damageMultiplier = mul;
    }

    public EntityGramDemolition(Level level, LivingEntity shooter) {
        super(EntityRegister.GRAM_DEMOLITION.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {}

    // 毎Tick（1/20秒）ごとの処理：パーティクル演出
    @Override
    public void tick() {
        super.tick();

        // サーバー・クライアント共通：飛んでいる最中にパーティクルを出す
        if (this.level.isClientSide) {
            // 想子（サイオン）の塊なので、青い炎と火花で表現
            this.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            this.level.addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }

        // 3秒(60tick)で消滅（負荷対策）
        if (this.tickCount > 60) {
            this.discard();
        }
    }

    // 何かに当たった時の処理
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            this.discard(); // 消滅
        }
    }

    // エンティティ（モブやプレイヤー）に当たった時の処理
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        // 変更: 計算されたダメージを適用
        float baseDamage = 10.0F;
        float finalDamage = baseDamage * this.damageMultiplier;

        target.hurt(DamageSource.MAGIC, finalDamage);

        // 強烈な衝撃（ノックバック）を与える
        target.setDeltaMovement(target.getDeltaMovement().add(this.getDeltaMovement().normalize().scale(1.5)));
    }

    // 重力をゼロにする（真っ直ぐ飛ぶ）
    @Override
    protected float getGravity() {
        return 0.0F;
    }

    // Forge必須記述：パケット送受信
    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
