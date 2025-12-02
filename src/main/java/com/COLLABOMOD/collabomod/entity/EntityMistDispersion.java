package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.register.EntityRegister;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

public class EntityMistDispersion extends ThrowableProjectile{
    public EntityMistDispersion(EntityType<? extends ThrowableProjectile> type, Level level) {
        super(type, level);
    }

    public EntityMistDispersion(Level level, LivingEntity shooter) {
        super(EntityRegister.MIST_DISPERSION.get(), shooter, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        // 演出：水色の粒子が静かに、しかし速く飛ぶ
        if (this.level.isClientSide) {
            this.level.addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
        if (this.tickCount > 40) this.discard(); // 射程は短めでもOK（必殺技なので）
    }

    // 重力なし（直線軌道）
    @Override
    protected float getGravity() { return 0.0F; }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            this.discard();
        }
    }

    // ★ここが「3連分解」の再現ロジック
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();

        if (target instanceof LivingEntity livingTarget) {
            // 1. 【領域干渉】魔法障壁（ポーション効果）の無効化
            livingTarget.removeAllEffects();

            // 盾を持っていた場合、盾を無効化（クールダウン付与）
            if (livingTarget instanceof Player targetPlayer) {
                if (targetPlayer.isUsingItem() && targetPlayer.getUseItem().is(Items.SHIELD)) {
                    targetPlayer.getCooldowns().addCooldown(Items.SHIELD, 100);
                    targetPlayer.stopUsingItem();
                }
            }

            // 2. 【情報強化分解】鎧・エンチャントを無視するダメージソース
            // DamageSource.OUT_OF_WORLD（奈落ダメージ）は、クリエイティブ以外すべての防御を貫通します

            // 3. 【肉体分解】分子レベルの消去
            // HP上限を超えるダメージを与えて即死させる
            livingTarget.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);

            // 【演出】消滅時のエフェクト（霧になって消える）
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF, livingTarget.getX(), livingTarget.getY() + 1, livingTarget.getZ(),
                        20, 0.5, 0.5, 0.5, 0.05);
                // "ジュッ"という蒸発音
                serverLevel.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, target.getSoundSource(), 1.0F, 1.0F);
            }
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
