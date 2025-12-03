package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.register.EntityRegister;
import com.mojang.math.Vector3f; // 色の指定に必要
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class EntityMaterialBurst extends Entity {

    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);

    private float maxRadius = 150.0F;//最終的な半径（50ブロック＝直径100ブロックのクレーター）
    private float expansionSpeed = 0.75F; // 広がる速度を少しアップ

    // サイオンの光の色（シアン～白）
    private static final Vector3f SPHERE_COLOR = new Vector3f(0.2F, 0.9F, 1.0F);

    public EntityMaterialBurst(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityMaterialBurst(Level level, double x, double y, double z) {
        this(EntityRegister.MATERIAL_BURST.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CURRENT_RADIUS, 0.0F);
    }

    public float getRadius() {
        return this.entityData.get(CURRENT_RADIUS);
    }

    public void setRadius(float radius) {
        this.entityData.set(CURRENT_RADIUS, radius);
    }

    @Override
    public void tick() {
        super.tick();

        float currentRadius = getRadius();
        float prevRadius = currentRadius;

        currentRadius += expansionSpeed;
        setRadius(currentRadius);

        // ■ クライアント側：高密度パーティクル球体
        if (this.level.isClientSide) {
            spawnDenseSphereParticles(currentRadius);

            // 轟音（サイズに応じてピッチを下げる＝巨大感を演出）
            if (this.tickCount % 5 == 0) {
                float pitch = 1.0F - (currentRadius / maxRadius) * 0.5F; // 1.0 -> 0.5
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, pitch, false);
            }
        }

        // ■ サーバー側：質量変換
        else {
            if (currentRadius > maxRadius) {
                this.discard();
                return;
            }
            processDestruction(prevRadius, currentRadius);
            processEntityDamage(currentRadius);
        }
    }

    // ■ 修正: 水も溶岩も完全に消す破壊処理
    private void processDestruction(float minR, float maxR) {
        BlockPos center = this.blockPosition();
        int range = (int) Math.ceil(maxR);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x * x + y * y + z * z;

                    // シェル（殻）の範囲内のみ処理
                    if (distSq <= maxR * maxR && distSq > minR * minR) {
                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(targetPos);
                        FluidState fluid = level.getFluidState(targetPos);

                        // 空気でなければ消す（液体も含む）
                        // !state.isAir() だけだと水源が消えないことがあるため、!fluid.isEmpty() もチェック
                        if (!state.isAir() || !fluid.isEmpty()) {

                            // 岩盤などは除外（必要なら外してください）
                            if (state.getDestroySpeed(level, targetPos) < 0) continue;

                            // ★重要: setBlockで強制的に「空気」にする
                            // removeBlockはドロップ処理などが走るが、setBlock(AIR)は「置換」なので確実かつ軽量
                            // flag 2 (ビット演算) = クライアントへ通知するが、隣接ブロックの更新（水流発生など）を通知しない
                            // これにより水流の計算が発生しにくくなり、水抜きがスムーズになる
                            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private void processEntityDamage(float radius) {
        AABB area = this.getBoundingBox().inflate(radius);
        List<Entity> list = this.level.getEntities(this, area);

        for (Entity e : list) {
            if (e.distanceToSqr(this) <= radius * radius) {
                if (e instanceof LivingEntity) {
                    e.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
                } else {
                    e.discard();
                }
            }
        }
    }

    // ■ 修正: 美しい球体エフェクトの描画
    private void spawnDenseSphereParticles(float radius) {
        // 色付きパーティクル（ダスト）を使用
        // RGB (0.2, 0.9, 1.0) -> シアンブルー
        // サイズ: 2.0F (少し大きめ)
        ParticleOptions particle = new DustParticleOptions(SPHERE_COLOR, 50.0F);

        // 半径が大きいほどパーティクル数を増やす（スカスカ防止）
        // 表面積(4πr^2)に比例させると重すぎるので、半径に比例させる程度に調整
        int count = (int)(radius * radius * 1.5);
        if (count > 2000) count = 2000; // 上限設定（クライアント負荷対策）

        for (int i = 0; i < count; i++) {
            // 球面上のランダムな点を計算（均一分布）
            double z = random.nextDouble() * 2.0 - 1.0; // -1 to 1
            double theta = random.nextDouble() * 2.0 * Math.PI; // 0 to 2π
            double r = Math.sqrt(1.0 - z * z) * radius;

            double x = r * Math.cos(theta);
            double y = r * Math.sin(theta);
            double finalZ = z * radius;

            // 座標
            double px = this.getX() + x;
            double py = this.getY() + y;
            double pz = this.getZ() + finalZ;

            // パーティクル生成
            this.level.addParticle(particle, px, py, pz, 0, 0, 0);

            // 演出強化: 内部にも少し「FLASH」を入れて、エネルギーの塊感を出す
            if (random.nextInt(100) == 0) {
                this.level.addParticle(ParticleTypes.FLASH, px, py, pz, 0, 0, 0);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setRadius(tag.getFloat("Radius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", getRadius());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}