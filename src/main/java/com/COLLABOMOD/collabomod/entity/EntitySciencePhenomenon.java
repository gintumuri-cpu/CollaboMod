package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.physics.PhysicsSystem;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import com.COLLABOMOD.collabomod.science.PhenomenonType;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import com.COLLABOMOD.collabomod.science.ScienceEngine;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class EntitySciencePhenomenon extends Entity {
    // 同期データ
    private static final EntityDataAccessor<Float> CURRENT_RADIUS = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_ENERGY = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_ALPHA = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);

    // 物理パラメータ
    private static final EntityDataAccessor<Float> TEMPERATURE = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MASS = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VELOCITY = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);

    // 成分データ
    private static final EntityDataAccessor<Float> COMP_MATTER = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COMP_WAVE = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COMP_SHIELD = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.FLOAT);

    // 現象タイプ (Ordinal値で保存)
    private static final EntityDataAccessor<Integer> PHENOMENON_TYPE = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntitySciencePhenomenon.class, EntityDataSerializers.INT);

    // ローカル変数
    private float maxRadius = 50.0F;
    private float expansionSpeed = 0.5F;
    private int maxLifeTime = 200;

    public EntitySciencePhenomenon(EntityType<EntitySciencePhenomenon> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntitySciencePhenomenon(Level level, Vec3 pos, ScienceContext ctx, LivingEntity caster) {
        this(EntityRegister.SCIENCE_PHENOMENON.get(), level);
        this.setPos(pos);

        this.maxRadius = ctx.radius;
        this.expansionSpeed = Math.max(0.1F, ctx.velocity * 0.2F);

        if (caster == null) {
            this.maxLifeTime = 40; // 2秒で消える
        } else {
            // プレイヤーの魔法（マテリアルバーストなど）は長めに
            this.maxLifeTime = 400; // 20秒
        }

        this.entityData.set(CURRENT_ENERGY, ctx.energy);
        this.entityData.set(TEMPERATURE, ctx.temperature);
        this.entityData.set(MASS, ctx.mass);
        this.entityData.set(VELOCITY, ctx.velocity);

        this.entityData.set(COMP_MATTER, ctx.compMatter);
        this.entityData.set(COMP_WAVE, ctx.compWave);
        this.entityData.set(COMP_SHIELD, ctx.compShield);

        // ■ タイプを保存
        this.entityData.set(PHENOMENON_TYPE, ctx.type.ordinal());

        if (caster != null) {
            this.entityData.set(CASTER_ID, caster.getId());
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CURRENT_RADIUS, 0.0F);
        this.entityData.define(CURRENT_ENERGY, 0.0F);
        this.entityData.define(CURRENT_ALPHA, 1.0F);
        this.entityData.define(TEMPERATURE, 300.0F);
        this.entityData.define(MASS, 0.0F);
        this.entityData.define(VELOCITY, 1.0F);
        this.entityData.define(COMP_MATTER, 0.0F);
        this.entityData.define(COMP_WAVE, 0.0F);
        this.entityData.define(COMP_SHIELD, 0.0F);
        this.entityData.define(PHENOMENON_TYPE, PhenomenonType.SPHERE_EXPANSION.ordinal());
        this.entityData.define(CASTER_ID, -1);
    }

    // Getter
    public float getRadius() { return this.entityData.get(CURRENT_RADIUS); }
    public float getEnergy() { return this.entityData.get(CURRENT_ENERGY); }
    public float getAlpha() { return this.entityData.get(CURRENT_ALPHA); }
    public PhenomenonType getPhenomenonType() {return PhenomenonType.values()[this.entityData.get(PHENOMENON_TYPE)];}

    // ビジュアル情報の取得
    public VisualMetadata getVisualMetadata() {
        ScienceContext ctx = new ScienceContext();
        ctx.energy = getEnergy();
        ctx.radius = getRadius();
        ctx.compMatter = this.entityData.get(COMP_MATTER);
        ctx.compWave = this.entityData.get(COMP_WAVE);
        ctx.compShield = this.entityData.get(COMP_SHIELD);
        ctx.temperature = this.entityData.get(TEMPERATURE);
        ctx.type = getPhenomenonType(); // 保存されたタイプを使用

        ScienceEngine.simulateVisuals(ctx);
        return ctx.visuals;
    }

    // 互換用
    public String getRendererID() { return getVisualMetadata().rendererID; }
    public Vector3f getColor() { return getVisualMetadata().mainColor; }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > maxLifeTime) {
            this.discard();
            return;
        }

        float currentRadius = getRadius();
        PhenomenonType type = getPhenomenonType();
        boolean isActive = false;

        // ■■■ タイプごとの挙動分岐 ■■■

        // 1. 球体膨張 (Expansion): どんどん広がる
        if (type == PhenomenonType.SPHERE_EXPANSION) {
            if (currentRadius < maxRadius) {
                currentRadius += expansionSpeed;
                this.entityData.set(CURRENT_RADIUS, currentRadius);
                isActive = true;
            }
        }
        // 2. その他のタイプ (Point/Beam等):
        // ※将来的に移動ロジックなどをここに追加。今回は簡易的に寿命管理のみ
        else {
            // 簡易実装: 一定時間留まるなど
            if (this.tickCount < 100) { // 仮の寿命
                isActive = true;
            }
        }

        // 寿命切れならフェードアウト
        if (!isActive) {
            float alpha = getAlpha() - 0.02F;
            if (alpha < 0) alpha = 0;
            this.entityData.set(CURRENT_ALPHA, alpha);
        }

        // --- クライアント側 ---
        if (this.level.isClientSide) {
            if (getAlpha() <= 0.0F) return;
            // 音演出
            if (this.tickCount % 20 == 0) {
                // シールドなら静かに
                if (this.entityData.get(COMP_SHIELD) > 0) {
                    this.level.playLocalSound(this.getX(), this.getY(), this.getZ(),
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0F, 1.0F, false);
                }
            }
        }

        // --- サーバー側 ---
        else {
            if (getAlpha() <= 0.0F) {
                this.discard();
                return;
            }

            // 物理エンジンの実行
            if (isActive) {
                ScienceContext ctx = new ScienceContext();
                ctx.energy = getEnergy();
                ctx.radius = currentRadius;
                ctx.compMatter = this.entityData.get(COMP_MATTER);
                ctx.compWave = this.entityData.get(COMP_WAVE);
                ctx.compShield = this.entityData.get(COMP_SHIELD);
                ctx.temperature = this.entityData.get(TEMPERATURE);
                ctx.mass = this.entityData.get(MASS);
                ctx.velocity = this.entityData.get(VELOCITY);
                ctx.type = type;

                LivingEntity caster = null;
                int cid = this.entityData.get(CASTER_ID);
                if (cid != -1) {
                    Entity e = level.getEntity(cid);
                    if (e instanceof LivingEntity) caster = (LivingEntity) e;
                }

                PhysicsSystem.applyPhysics(level, this.position(), ctx, caster, null);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(CURRENT_RADIUS, tag.getFloat("Radius"));
        this.entityData.set(CURRENT_ENERGY, tag.getFloat("Energy"));
        this.entityData.set(CURRENT_ALPHA, tag.getFloat("Alpha"));
        this.entityData.set(COMP_MATTER, tag.getFloat("CompMatter"));
        this.entityData.set(COMP_WAVE, tag.getFloat("CompWave"));
        this.entityData.set(COMP_SHIELD, tag.getFloat("CompShield"));
        this.entityData.set(TEMPERATURE, tag.getFloat("Temperature"));
        this.entityData.set(MASS, tag.getFloat("Mass"));
        this.entityData.set(VELOCITY, tag.getFloat("Velocity"));
        this.entityData.set(PHENOMENON_TYPE, tag.getInt("PType"));
        this.entityData.set(CASTER_ID, tag.getInt("CasterID"));
        this.maxRadius = tag.getFloat("MaxRadius");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Radius", getRadius());
        tag.putFloat("Energy", getEnergy());
        tag.putFloat("Alpha", getAlpha());
        tag.putFloat("CompMatter", this.entityData.get(COMP_MATTER));
        tag.putFloat("CompWave", this.entityData.get(COMP_WAVE));
        tag.putFloat("CompShield", this.entityData.get(COMP_SHIELD));
        tag.putFloat("Temperature", this.entityData.get(TEMPERATURE));
        tag.putFloat("Mass", this.entityData.get(MASS));
        tag.putFloat("Velocity", this.entityData.get(VELOCITY));
        tag.putInt("PType", this.entityData.get(PHENOMENON_TYPE));
        tag.putInt("CasterID", this.entityData.get(CASTER_ID));
        tag.putFloat("MaxRadius", maxRadius);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
