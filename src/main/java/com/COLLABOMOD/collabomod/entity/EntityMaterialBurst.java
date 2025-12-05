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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.Random;

public class EntityMaterialBurst extends Entity {

    // データ同期用のキー定義
    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_ENERGY = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_ALPHA = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);

    private float maxRadius = 30.0F;
    private float expansionSpeed = 0.1F;

    private static final Vector3f SPHERE_COLOR = new Vector3f(0.2F, 0.9F, 1.0F);

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
        // ■ 重要: ここで全てのキーを初期化しないとクラッシュします
        this.entityData.define(CURRENT_RADIUS, 0.0F);
        this.entityData.define(CURRENT_ENERGY, 0.0F);
        this.entityData.define(CURRENT_ALPHA, 1.0F);
    }

    // Getter / Setter
    public float getRadius() { return this.entityData.get(CURRENT_RADIUS); }
    public void setRadius(float radius) { this.entityData.set(CURRENT_RADIUS, radius); }

    public float getEnergy() { return this.entityData.get(CURRENT_ENERGY); }
    public void setEnergy(float energy) { this.entityData.set(CURRENT_ENERGY, energy); }

    public float getAlpha() { return this.entityData.get(CURRENT_ALPHA); }
    public void setAlpha(float alpha) { this.entityData.set(CURRENT_ALPHA, alpha); }

    @Override
    public void tick() {
        super.tick();

        float currentRadius = getRadius();
        float prevRadius = currentRadius;
        float currentAlpha = getAlpha();

        // 拡大フェーズ
        if (currentRadius < maxRadius) {
            currentRadius += expansionSpeed;
            setRadius(currentRadius);
        }
        // フェードアウトフェーズ
        else {
            currentAlpha -= 0.025F;
            if (currentAlpha < 0.0F) currentAlpha = 0.0F;
            setAlpha(currentAlpha);
        }

        // --- クライアント側 ---
        if (this.level.isClientSide) {
            float energy = getEnergy();
            // 音の演出（フェードアウト中も継続）
            if (currentAlpha > 0.0F && this.tickCount % 10 == 0) {
                float pitch = 1.0F - Math.min(0.5F, (currentRadius / maxRadius) * 0.5F);
                float volume = (50.0F + (energy * 0.1F)) * currentAlpha;
                this.level.playLocalSound(this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_AMBIENT, SoundSource.WEATHER, volume, pitch, false);
            }

            // 残滓演出（最大サイズ到達時）
            if (currentRadius >= maxRadius - expansionSpeed && currentAlpha > 0.9F) {
                spawnRemnantParticles(maxRadius);
            }
        }

        // --- サーバー側 ---
        else {
            float currentEnergy = getEnergy();
            if (currentEnergy > 0) setEnergy(currentEnergy * 0.8F);

            // 完全に透明になったら消滅
//            if (currentAlpha <= 0.0F) {
//                level.playSound(null, this.getX(), this.getY(), this.getZ(),
//                        SoundEvents.GENERIC_EXPLODE, SoundSource.WEATHER, 100.0F, 0.5F);
//                this.discard();
//                return;
//            }

            if (currentAlpha <= 0.0F) {
                // 音は消去済み
                this.discard();
                return;
            }

            if (currentRadius < maxRadius) {
                // ■ 修正: スキャン漏れを防ぐため、破壊範囲に厚みを持たせる
                // 現在の半径から「3.0ブロック手前」までを毎回スキャンする
                // これにより、計算漏れや、流入してきた水を何度も消し飛ばすことができます
                processDestruction(currentRadius - 3.0F, currentRadius);

                // ■ 修正: 内部の水抜き処理（Core Cleanup）
                // 3.0Fより内側の「既に破壊が終わったはずの場所」に水が戻ってきていないかチェック
                // 毎Tick全範囲やると重いので、ランダムまたは分割してチェックするのが理想ですが、
                // 今回は「内側5ブロック」と「ランダムな内部」を掃除します
                processCoreCleanup(currentRadius);

                processShockwave(currentRadius);
                processEntityDamage(currentRadius);
            }
        }
    }

    // ■ 修正: 破壊処理（厚みを持たせてスキャン）
    private void processDestruction(float minR, float maxR) {
        BlockPos center = this.blockPosition();
        // 負の値にならないように
        if (minR < 0) minR = 0;

        int range = (int) Math.ceil(maxR);
        float massEnergy = 0.0F;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x * x + y * y + z * z;

                    // シェルの範囲内
                    if (distSq <= maxR * maxR && distSq > minR * minR) {
                        BlockPos targetPos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(targetPos);
                        FluidState fluid = level.getFluidState(targetPos);

                        if (!state.isAir() || !fluid.isEmpty()) {
                            if (state.getDestroySpeed(level, targetPos) < 0) continue;

                            // 質量エネルギー計算
                            float hardness = state.getExplosionResistance(level, targetPos, null);
                            if (hardness < 1.0F) hardness = 1.0F;
                            massEnergy += hardness;

                            // 強制置換（水流更新なし）
                            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        if (massEnergy > 0) {
            float totalEnergy = getEnergy() + massEnergy;
            if (totalEnergy > 1000.0F) totalEnergy = 1000.0F;
            setEnergy(totalEnergy);
        }
    }

    // ■ 追加: 内部の液体の掃除
    private void processCoreCleanup(float currentRadius) {
        // 半径が小さい時は全域掃除
        if (currentRadius < 5.0F) {
            cleanFluidsInArea(0, currentRadius);
            return;
        }

        // 半径が大きい時は、中心付近(5ブロック)と、ランダムな内部を掃除
        cleanFluidsInArea(0, 5.0F); // 爆心地の確保

        // 負荷軽減のため、内部全体ではなくランダムに数点をチェックして水を消す
        // (大量の水が雪崩れ込んできた場合の対策)
        Random rand = new Random();
        BlockPos center = this.blockPosition();
        for (int i = 0; i < 20; i++) { // 20箇所チェック
            double r = rand.nextDouble() * (currentRadius - 3.0F); // シェルより内側
            double theta = rand.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rand.nextDouble() - 1);
            int x = (int)(r * Math.sin(phi) * Math.cos(theta));
            int y = (int)(r * Math.sin(phi) * Math.sin(theta));
            int z = (int)(r * Math.cos(phi));

            BlockPos pos = center.offset(x, y, z);
            if (!level.getFluidState(pos).isEmpty()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    // 範囲内の液体を消すヘルパー
    private void cleanFluidsInArea(float minR, float maxR) {
        BlockPos center = this.blockPosition();
        int range = (int) Math.ceil(maxR);

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq <= maxR * maxR && distSq >= minR * minR) {
                        BlockPos pos = center.offset(x, y, z);
                        if (!level.getFluidState(pos).isEmpty()) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    // 残滓パーティクル
    private void spawnRemnantParticles(float radius) {
        int count = 500;
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            double r = radius * Math.sqrt(rand.nextDouble());
            double theta = rand.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rand.nextDouble() - 1);

            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);

            this.level.addParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    this.getX() + x, this.getY() + y, this.getZ() + z,
                    0, 0.05, 0);

            if (r > radius * 0.8) {
                this.level.addParticle(ParticleTypes.EXPLOSION,
                        this.getX() + x, this.getY() + y, this.getZ() + z,
                        0, 0, 0);
            }
        }
    }

//    private void processDestruction(float minR, float maxR) {
//        BlockPos center = this.blockPosition();
//        int range = (int) Math.ceil(maxR);
//        float massEnergy = 0.0F;
//
//        for (int x = -range; x <= range; x++) {
//            for (int y = -range; y <= range; y++) {
//                for (int z = -range; z <= range; z++) {
//                    double distSq = x * x + y * y + z * z;
//
//                    if (distSq <= maxR * maxR && (minR == 0 || distSq > minR * minR)) {
//                        BlockPos targetPos = center.offset(x, y, z);
//                        BlockState state = level.getBlockState(targetPos);
//                        FluidState fluid = level.getFluidState(targetPos);
//
//                        if (!state.isAir() || !fluid.isEmpty()) {
//                            if (state.getDestroySpeed(level, targetPos) < 0) continue;
//
//                            float hardness = state.getExplosionResistance(level, targetPos, null);
//                            if (hardness < 1.0F) hardness = 1.0F;
//                            massEnergy += hardness;
//
//                            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (massEnergy > 0) {
//            float totalEnergy = getEnergy() + massEnergy;
//            if (totalEnergy > 1000.0F) totalEnergy = 1000.0F;
//            setEnergy(totalEnergy);
//        }
//    }

    private void processShockwave(float radius) {
        float energy = getEnergy();
        if (energy < 10.0F) return;

        double shockRange = radius * 2.0D;
        AABB area = this.getBoundingBox().inflate(shockRange);
        List<Entity> list = this.level.getEntities(this, area);

        for (Entity e : list) {
            if (e instanceof LivingEntity && e != this) {
                double dist = e.distanceTo(this);
                if (dist > radius) {
                    double force = (energy / 50.0D) * (1.0D - (dist / shockRange));
                    if (force > 0) {
                        Vec3 dir = e.position().subtract(this.position()).normalize();
                        e.setDeltaMovement(e.getDeltaMovement().add(dir.scale(force)));
                        e.hurtMarked = true;
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
                if (e instanceof LivingEntity) e.hurt(DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
                else e.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setRadius(tag.getFloat("Radius"));
        this.setEnergy(tag.getFloat("Energy"));
        this.setAlpha(tag.getFloat("Alpha"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", getRadius());
        tag.putFloat("Energy", getEnergy());
        tag.putFloat("Alpha", getAlpha());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    // ■■■ 重要追加: 距離による描画制限を解除 ■■■
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // 通常は "distance < 64 * 64" などの判定が入りますが、
        // true を返すことで、チャンクが読み込まれている限り、どんなに遠くても描画させます
        return true;
    }
}