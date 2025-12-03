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
import java.util.Random;

public class EntityMaterialBurst extends Entity {

    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);

    // ■ 修正: 半径60.0F (直径120ブロック)
    private float maxRadius = 65.0F;
    private float expansionSpeed = 0.25F;

    public EntityMaterialBurst(EntityType<EntityMaterialBurst> type, Level level) {
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

        float prevRadius = getRadius();
        float currentRadius = prevRadius + expansionSpeed;
        setRadius(currentRadius);

        // クライアント側: 音の演出のみ（描画はRendererに任せる）
        if (this.level.isClientSide) {
            if (this.tickCount % 10 == 0) {
                float pitch = 1.0F - Math.min(0.5F, (currentRadius / maxRadius) * 0.5F);
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.WEATHER, 50.0F, pitch, false);
            }
            // ■■■ 追加: 終了間際の残滓演出 ■■■
            // 最大半径に近づいたら、フェードアウト用のパーティクルを出す
            if (currentRadius >= maxRadius - 1.0F) {
                spawnRemnantParticles(maxRadius);
            }
        }
        // サーバー側: 破壊処理
        else {
            if (currentRadius > maxRadius) {
                this.discard();
                return;
            }
            processDestruction(prevRadius, currentRadius);
            processEntityDamage(currentRadius);
        }
    }

    private void processDestruction(float minR, float maxR) {
        BlockPos center = this.blockPosition();
        int range = (int) Math.ceil(maxR);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x * x + y * y + z * z;

                    // ■ 修正: minRが0の場合（初回）は、中心点（距離0）も含めるように条件分岐
                    boolean isInsideInner = (minR == 0) ? false : (distSq <= minR * minR);

                    if (distSq <= maxR * maxR && !isInsideInner) {
                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(targetPos);
                        FluidState fluid = level.getFluidState(targetPos);

                        if (!state.isAir() || !fluid.isEmpty()) {
                            if (state.getDestroySpeed(level, targetPos) < 0) continue;
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

    private void spawnRemnantParticles(float radius) {
        int count = 200; // クライアント負荷を考慮して程々に
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            // 球の内部～表面にランダム配置
            double r = radius * Math.sqrt(rand.nextDouble()); // 体積一様分布
            double theta = rand.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rand.nextDouble() - 1);

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);

            // 1. CAMPFIRE_SIGNAL_SMOKE: 長く残る白い煙（蒸発した物質）
            this.level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    this.getX() + x, this.getY() + y, this.getZ() + z,
                    0, 0.1, 0); // 少し上昇する

            // 2. EXPLOSION: 爆発の余韻
            if (i % 5 == 0) {
                this.level.addParticle(ParticleTypes.EXPLOSION,
                        this.getX() + x, this.getY() + y, this.getZ() + z,
                        0, 0, 0);
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