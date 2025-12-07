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

    // ■■■ 対抗魔法処理 ■■■
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        // 1. 魔法式（魔法陣）に当たった場合 -> 術式解散
        if (target instanceof EntityMagicSequence) {
            target.discard(); // 魔法陣を消去（発動キャンセル）

            // 演出: 解散のエフェクト（白い煙と音）
            this.level.addParticle(ParticleTypes.CLOUD, target.getX(), target.getY(), target.getZ(), 0, 0, 0);
            this.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    net.minecraft.sounds.SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 2.0F);

            return; // 貫通せずに終了
        }

        // 2. 敵の魔法弾（エアバレットなど）に当たった場合 -> 迎撃
        if (target instanceof EntityAirBullet) {
            target.discard(); // 弾を消去
            // 演出: 衝撃
            this.level.addParticle(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 0, 0, 0);
            return;
        }

        // 3. 生き物（Mob/Player）に当たった場合 -> ダメージ＆ノックバック
        if (target instanceof LivingEntity) {
            float baseDamage = 1.0F; // 物理威力は低い
            float finalDamage = baseDamage * this.damageMultiplier;

            target.hurt(DamageSource.MAGIC, finalDamage);

            // 魔法効果の解除（バフ消し）
            ((LivingEntity) target).removeAllEffects();

            // 強いノックバック（吹き飛ばし）
            Vec3 knockback = this.getDeltaMovement().normalize().scale(2.5);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback));
        }
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
