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

        float physicalDamage = 1.0F; // 0.5ハート
        target.hurt(DamageSource.MAGIC, physicalDamage);

        // 2. 魔法的要素（バフ・デバフ）をすべて吹き飛ばす
        if (target instanceof LivingEntity living) {
            living.removeAllEffects(); // ポーション効果全消去
        }

        // 3. 強烈なノックバック（物理的な衝撃）
        // ベクトルを正規化して、強く押し出す
        Vec3 knockback = this.getDeltaMovement().normalize().scale(2.5); // 強め
        target.setDeltaMovement(target.getDeltaMovement().add(knockback));
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
