//package com.COLLABOMOD.collabomod.entity;
//
//import com.COLLABOMOD.collabomod.magic.VisualMetadata;
//import com.COLLABOMOD.collabomod.physics.PhysicsSystem;
//import com.COLLABOMOD.collabomod.register.EntityRegister;
//import com.COLLABOMOD.collabomod.science.ScienceContext;
//import com.COLLABOMOD.collabomod.science.ScienceEngine;
//import com.COLLABOMOD.collabomod.science.PhenomenonType;
//import com.mojang.math.Vector3f; // 色の指定に必要
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.particles.DustParticleOptions;
//import net.minecraft.core.particles.ParticleOptions;
//import net.minecraft.core.particles.ParticleTypes;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.protocol.Packet;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Blocks;
//import net.minecraft.world.level.block.LiquidBlock;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraft.world.level.material.FluidState;
//import net.minecraft.world.phys.AABB;
//import net.minecraft.world.phys.Vec3;
//import net.minecraftforge.network.NetworkHooks;
//
//import java.util.List;
//import java.util.Random;
//
//public class EntityMaterialBurst extends Entity {
//
//    // 同期データ: 半径、エネルギー、透明度、そして「成分」
//    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> CURRENT_ENERGY = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> CURRENT_ALPHA = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//
//    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.INT);
//    private static final EntityDataAccessor<Float> TEMPERATURE = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> MASS = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> VELOCITY = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    // 成分データ（見た目と物理挙動の決定に必要）
//    private static final EntityDataAccessor<Float> COMP_MATTER = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> COMP_WAVE = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//    private static final EntityDataAccessor<Float> COMP_SHIELD = SynchedEntityData.defineId(EntityMaterialBurst.class, EntityDataSerializers.FLOAT);
//
//    // 設定値
//    private float maxRadius = 50.0F;
//    private float expansionSpeed = 0.5F;
//
//    // クライアント用キャッシュ
//    private VisualMetadata cachedVisuals = null;
//
//    public EntityMaterialBurst(EntityType<EntityMaterialBurst> type, Level level) {
//        super(type, level);
//        this.noCulling = true;
//    }
//
//    // 初期化用コンストラクタ
//    public EntityMaterialBurst(Level level, Vec3 pos, ScienceContext ctx, LivingEntity caster) {
//        this(EntityRegister.MATERIAL_BURST.get(), level);
//        this.setPos(pos);
//
//        // Contextからデータをコピー
//        this.maxRadius = ctx.radius;
//        this.expansionSpeed = Math.max(0.1F, ctx.velocity * 0.2F); // 速度調整
//
//        this.entityData.set(CURRENT_ENERGY, ctx.energy);
//        this.entityData.set(COMP_MATTER, ctx.compMatter);
//        this.entityData.set(COMP_WAVE, ctx.compWave);
//        this.entityData.set(COMP_SHIELD, ctx.compShield);
//
//        this.entityData.set(TEMPERATURE, ctx.temperature);
//        this.entityData.set(MASS, ctx.mass);
//        this.entityData.set(VELOCITY, ctx.velocity);
//
//        if (caster != null) {
//            this.entityData.set(CASTER_ID, caster.getId());
//        }
//    }
//
//    @Override
//    protected void defineSynchedData() {
//        this.entityData.define(CURRENT_RADIUS, 0.0F);
//        this.entityData.define(CURRENT_ENERGY, 0.0F);
//        this.entityData.define(CURRENT_ALPHA, 1.0F);
//        this.entityData.define(COMP_MATTER, 0.0F);
//        this.entityData.define(COMP_WAVE, 0.0F);
//        this.entityData.define(COMP_SHIELD, 0.0F);
//        this.entityData.define(CASTER_ID, -1);
//        this.entityData.define(TEMPERATURE, 300.0F);
//        this.entityData.define(MASS, 0.0F);
//        this.entityData.define(VELOCITY, 1.0F);
//    }
//
//    // Getter
//    public float getRadius() { return this.entityData.get(CURRENT_RADIUS); }
//    public float getEnergy() { return this.entityData.get(CURRENT_ENERGY); }
//    public float getAlpha() { return this.entityData.get(CURRENT_ALPHA); }
//
//    // ■ ビジュアル情報の取得（レンダラーが呼ぶ）
//    // サーバーから同期された成分値をもとに、クライアントでScienceEngineを回して見た目を決める
//    public VisualMetadata getVisualMetadata() {
//        // 毎回計算すると重いのでキャッシュしても良いが、変化に対応するため簡易計算
//        ScienceContext ctx = new ScienceContext();
//        ctx.energy = getEnergy();
//        ctx.radius = getRadius();
//        ctx.compMatter = this.entityData.get(COMP_MATTER);
//        ctx.compWave = this.entityData.get(COMP_WAVE);
//        ctx.compShield = this.entityData.get(COMP_SHIELD);
//        ctx.temperature = this.entityData.get(TEMPERATURE);
//        ctx.type = PhenomenonType.SPHERE_EXPANSION;
//
//        ScienceEngine.simulateVisuals(ctx);
//        return ctx.visuals;
//    }
//
//    // 互換用（RenderMaterialBurstが呼ぶ）
//    public String getRendererID() { return getVisualMetadata().rendererID; }
//    public com.mojang.math.Vector3f getColor() { return getVisualMetadata().mainColor; }
//
//    @Override
//    public void tick() {
//        super.tick();
//
//        float currentRadius = getRadius();
//
//        // 拡大フェーズ
//        if (currentRadius < maxRadius) {
//            currentRadius += expansionSpeed;
//            this.entityData.set(CURRENT_RADIUS, currentRadius);
//        } else {
//            // フェードアウト
//            float alpha = getAlpha() - 0.02F;
//            if (alpha < 0) alpha = 0;
//            this.entityData.set(CURRENT_ALPHA, alpha);
//        }
//
//        // --- クライアント側 ---
//        if (this.level.isClientSide) {
//            if (getAlpha() <= 0.0F) return;
//
//            // 音の演出 (シールドなら静かに、爆発なら激しく)
//            if (this.tickCount % 20 == 0) {
//                boolean isShield = this.entityData.get(COMP_SHIELD) > 0;
//                if (isShield) {
//                    this.level.playLocalSound(this.getX(), this.getY(), this.getZ(),
//                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0F, 1.0F, false);
//                } else {
//                    // 既存の爆発音
//                }
//            }
//        }
//
//        // --- サーバー側 ---
//        else {
//            if (getAlpha() <= 0.0F) {
//                this.discard();
//                return;
//            }
//
//            // ■ 物理エンジンの実行
//            // このエンティティ自体を「発生源」として物理演算を行う
//            if (currentRadius < maxRadius) {
//                ScienceContext ctx = new ScienceContext();
//                ctx.energy = getEnergy();
//                ctx.radius = currentRadius;
//                ctx.compMatter = this.entityData.get(COMP_MATTER);
//                ctx.compWave = this.entityData.get(COMP_WAVE);
//                ctx.compShield = this.entityData.get(COMP_SHIELD);
//                ctx.temperature = this.entityData.get(TEMPERATURE);
//                // ■ 修正: キャスターを取得して渡す
//                LivingEntity caster = null;
//                int casterId = this.entityData.get(CASTER_ID);
//                if (casterId != -1) {
//                    Entity e = level.getEntity(casterId);
//                    if (e instanceof LivingEntity) caster = (LivingEntity) e;
//                }
//
//                // targetは範囲魔法なのでnullでOK (もしくは必要ならtargetIDも保存する)
//                PhysicsSystem.applyPhysics(level, this.position(), ctx, caster, null);
//            }
//        }
//    }
//
//    @Override
//    protected void readAdditionalSaveData(CompoundTag tag) {
//        this.entityData.set(CURRENT_RADIUS, tag.getFloat("Radius"));
//        this.entityData.set(CURRENT_ENERGY, tag.getFloat("Energy"));
//        this.entityData.set(CURRENT_ALPHA, tag.getFloat("Alpha"));
//        this.entityData.set(COMP_MATTER, tag.getFloat("CompMatter"));
//        this.entityData.set(COMP_WAVE, tag.getFloat("CompWave"));
//        this.entityData.set(COMP_SHIELD, tag.getFloat("CompShield"));
//        this.maxRadius = tag.getFloat("MaxRadius");
//        this.entityData.set(CASTER_ID, tag.getInt("CasterID"));
//        this.entityData.set(TEMPERATURE, tag.getFloat("Temperature"));
//        this.entityData.set(MASS, tag.getFloat("Mass"));
//        this.entityData.set(VELOCITY, tag.getFloat("Velocity"));
//    }
//
//    @Override
//    protected void addAdditionalSaveData(CompoundTag tag) {
//        tag.putFloat("Radius", getRadius());
//        tag.putFloat("Energy", getEnergy());
//        tag.putFloat("Alpha", getAlpha());
//        tag.putFloat("CompMatter", this.entityData.get(COMP_MATTER));
//        tag.putFloat("CompWave", this.entityData.get(COMP_WAVE));
//        tag.putFloat("CompShield", this.entityData.get(COMP_SHIELD));
//        tag.putFloat("MaxRadius", maxRadius);
//        tag.putFloat("Temperature", this.entityData.get(TEMPERATURE));
//        tag.putFloat("Mass", this.entityData.get(MASS));
//        tag.putFloat("Velocity", this.entityData.get(VELOCITY));
//        this.entityData.set(CASTER_ID, tag.getInt("CasterID"));
//    }
//
//    @Override
//    public Packet<?> getAddEntityPacket() {
//        return NetworkHooks.getEntitySpawningPacket(this);
//    }
//}