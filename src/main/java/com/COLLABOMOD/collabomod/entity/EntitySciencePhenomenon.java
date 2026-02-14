package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.learning.CardinalLearningManager;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.magic.WorldEffectHelper;
import net.minecraft.core.BlockPos;
import com.COLLABOMOD.collabomod.register.EntityRegister; // エンティティ登録クラスへの参照(環境に合わせて修正してください)
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class EntitySciencePhenomenon extends Entity {

    // NBTタグを同期するためのデータアクセサ
    private static final EntityDataAccessor<CompoundTag> VISUAL_DATA = SynchedEntityData
            .defineId(EntitySciencePhenomenon.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> COMMAND_LIST_DATA = SynchedEntityData
            .defineId(EntitySciencePhenomenon.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> PHYSICS_DATA = SynchedEntityData
            .defineId(EntitySciencePhenomenon.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData
            .defineId(EntitySciencePhenomenon.class, EntityDataSerializers.INT);

    // ローカルキャッシュ (毎フレームNBT解析するのを防ぐため)
    private PhysicsMetadata physicsCache = new PhysicsMetadata();
    private VisualMetadata visualCache = new VisualMetadata();
    private boolean isVisualCacheDirty = true;
    private boolean isPhysicsCacheDirty = true;

    public int lifeTime = 0;
    public int maxLifeTime = 100;
    private boolean hasExploded = false; // RADIALタイプの一回性爆発制御用

    // クライアントサイドでのみ使用する動的テクスチャID
    private ResourceLocation dynamicTextureLocation = null;

    // デフォルトコンストラクタ (登録用)
    public EntitySciencePhenomenon(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true; // 常に描画
    }

    // スポーン用コンストラクタ
    public EntitySciencePhenomenon(Level level, LivingEntity caster) {
        this(EntityRegister.SCIENCE_PHENOMENON.get(), level);
        if (caster != null) {
            this.setPos(caster.getX(), caster.getEyeY(), caster.getZ());
            this.entityData.set(CASTER_ID, caster.getId());
        }
    }

    @Override
    protected void defineSynchedData() {
        // デフォルトの空データをセット
        this.entityData.define(VISUAL_DATA, new CompoundTag());
        this.entityData.define(COMMAND_LIST_DATA, new CompoundTag());
        this.entityData.define(PHYSICS_DATA, new CompoundTag());
        this.entityData.define(CASTER_ID, -1);
    }

    /**
     * 物理パラメータを設定し、挙動と寿命を初期化する
     */
    public void setPhysicsMetadata(PhysicsMetadata p) {
        this.physicsCache = p;
        this.entityData.set(PHYSICS_DATA, p.toNBT()); // クライアント同期
        this.isPhysicsCacheDirty = false;

        if (p.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL) {
            this.maxLifeTime = 100;
        } else if (p.forceType == PhysicsMetadata.EnumForceType.RADIAL) {
            this.maxLifeTime = 40;
        } else if (p.forceType == PhysicsMetadata.EnumForceType.FIELD) {
            this.maxLifeTime = 400;
        } else {
            this.maxLifeTime = 60;
        }
    }

    /**
     * 視覚パラメータを設定する
     */
    public void setVisualMetadata(VisualMetadata v) {
        this.visualCache = v;
        this.entityData.set(VISUAL_DATA, v.toNBT()); // クライアント同期
        this.isVisualCacheDirty = false;
    }

    public PhysicsMetadata getPhysicsMetadata() {
        if (this.level.isClientSide && isPhysicsCacheDirty) {
            this.physicsCache = PhysicsMetadata.fromNBT(this.entityData.get(PHYSICS_DATA));
            isPhysicsCacheDirty = false;
        }
        return this.physicsCache;
    }

    public VisualMetadata getVisualMetadata() {
        if (this.level.isClientSide && isVisualCacheDirty) {
            this.visualCache = VisualMetadata.fromNBT(this.entityData.get(VISUAL_DATA));
            isVisualCacheDirty = false;
        }
        return this.visualCache;
    }

    public ListTag getCommandList() {
        CompoundTag wrapper = this.entityData.get(COMMAND_LIST_DATA);
        if (wrapper.contains("Commands", 9)) {
            return wrapper.getList("Commands", 10);
        }
        return new ListTag();
    }

    public void setDynamicTexture(ResourceLocation location) {
        this.dynamicTextureLocation = location;
    }

    public ResourceLocation getDynamicTexture() {
        return this.dynamicTextureLocation;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (VISUAL_DATA.equals(key)) {
            this.isVisualCacheDirty = true;
        } else if (PHYSICS_DATA.equals(key)) {
            this.isPhysicsCacheDirty = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.lifeTime++;

        if (!this.level.isClientSide) {
            if (this.lifeTime > this.maxLifeTime) {
                this.discard();
                return;
            }
        }

        PhysicsMetadata phy = getPhysicsMetadata();
        VisualMetadata vis = getVisualMetadata();

        if (phy.velocity > 0 || phy.gravity) {
            Vec3 motion = this.getDeltaMovement();
            if (phy.gravity) {
                motion = motion.add(0, -0.04, 0);
            }
            motion = motion.scale(0.98);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
        }

        if (!this.level.isClientSide) {
            float radius = vis.scale * 0.5f;
            if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL) {
                float progress = Math.min(1.0f, (float) lifeTime / 10.0f);
                radius = vis.scale * progress * 2.0f;
            }

            AABB searchBox = this.getBoundingBox().inflate(radius);
            List<Entity> targets = this.level.getEntities(this, searchBox);
            int casterId = this.entityData.get(CASTER_ID);

            for (Entity target : targets) {
                if (target.getId() == casterId && lifeTime < 10)
                    continue;
                if (target instanceof EntitySciencePhenomenon)
                    continue;

                if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL) {
                    if (!hasExploded) {
                        applyPhysicsEffect(target, phy);
                    }
                } else if (phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL) {
                    applyPhysicsEffect(target, phy);
                    this.discard();
                    return;
                } else if (phy.forceType == PhysicsMetadata.EnumForceType.FIELD) {
                    if (lifeTime % 5 == 0) {
                        applyPhysicsEffect(target, phy);
                    }
                }
            }

            if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL && !targets.isEmpty()) {
                hasExploded = true;
            }

            // ■ ブロック影響
            applyBlockEffect(phy, vis, radius);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level.isClientSide && reason.shouldDestroy()) {
            // AIの学習トリガー
            PhysicsMetadata phy = getPhysicsMetadata();
            VisualMetadata vis = getVisualMetadata();
            float score = AnalysisEngine.calculateConsistencyScore(phy, vis);
            CardinalLearningManager.getInstance().recordExperience(vis.rawVector, phy, vis, score);

            // ■ Phase B: 残留効果フィールドの生成
            spawnResidualField(phy, vis);
        }
        super.remove(reason);
    }

    private void spawnResidualField(PhysicsMetadata phy, VisualMetadata vis) {
        ResidualField.FieldType fieldType = null;
        float damage = phy.energy * 0.05f;

        // 属性から残留効果を決定
        if (vis.rawVector != null && vis.rawVector.length >= 5) {
            float wHeat = vis.rawVector[0];
            float wCold = vis.rawVector[1];
            float wEntropy = vis.rawVector[3];
            float wDivine = vis.rawVector[4];

            if (wHeat > 0.5f || phy.temperature > 1500.0f) {
                fieldType = ResidualField.FieldType.HEAT;
            } else if (wCold > 0.5f || phy.temperature < 100.0f) {
                fieldType = ResidualField.FieldType.FROST;
            } else if (wEntropy > 0.5f) {
                fieldType = ResidualField.FieldType.CHAOS;
            } else if (wDivine > 0.5f) {
                fieldType = ResidualField.FieldType.HOLY;
            } else if (phy.energy > 80.0f) {
                fieldType = ResidualField.FieldType.ELECTRIC;
            }
        } else if (phy.temperature > 1500.0f) {
            fieldType = ResidualField.FieldType.HEAT;
        } else if (phy.temperature < 100.0f) {
            fieldType = ResidualField.FieldType.FROST;
        }

        if (fieldType != null) {
            float radius = Math.max(vis.scale, phy.areaOfEffect) * 0.8f;
            int duration = 60 + (int) (phy.energy * 0.5f);
            ResidualField field = new ResidualField(
                    com.COLLABOMOD.collabomod.register.EntityRegister.RESIDUAL_FIELD.get(),
                    this.level, fieldType, radius, duration, damage);
            field.setPos(this.getX(), this.getY(), this.getZ());
            this.level.addFreshEntity(field);
        }
    }

    public void setCommandList(ListTag commandList) {
        CompoundTag wrapper = new CompoundTag();
        wrapper.put("Commands", commandList);
        this.entityData.set(COMMAND_LIST_DATA, wrapper);
    }

    private void applyPhysicsEffect(Entity target, PhysicsMetadata phy) {
        // ■ Phase C: 属性ダメージシステム
        VisualMetadata vis = getVisualMetadata();
        float[] attrs = (vis.rawVector != null && vis.rawVector.length >= 5) ? vis.rawVector : new float[5];

        // --- 基本物理ダメージ ---
        if (phy.temperature > 1000.0F) {
            target.setSecondsOnFire(5);
            target.hurt(DamageSource.IN_FIRE, phy.energy * 0.05F);
        } else if (phy.temperature < 200.0F) {
            target.setTicksFrozen(200);
            target.hurt(DamageSource.FREEZE, phy.energy * 0.05F);
        }

        float impact = phy.mass * phy.velocity;
        if (impact > 1.0F) {
            target.hurt(DamageSource.GENERIC, impact);
            Vec3 knockbackDir = this.position().vectorTo(target.position()).normalize();
            if (phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL) {
                knockbackDir = this.getDeltaMovement().normalize();
            }
            target.push(knockbackDir.x * impact * 0.1, 0.2, knockbackDir.z * impact * 0.1);
        }

        if (phy.energy > 0) {
            target.hurt(DamageSource.MAGIC, phy.energy * 0.1F);
        }

        // --- 属性ボーナスダメージ ---
        if (target instanceof LivingEntity living) {
            float baseDmg = phy.energy * 0.08f;
            float wHeat = attrs[0];
            float wCold = attrs[1];
            float wMotion = attrs[2];
            float wEntropy = attrs[3];
            float wDivine = attrs[4];

            // 火属性: 燃焼延長 + DoTダメージ
            if (wHeat > 0.3f) {
                target.setSecondsOnFire((int) (wHeat * 10));
                target.hurt(DamageSource.IN_FIRE, baseDmg * wHeat);
            }

            // 氷属性: 深い凍結 + 移動速度低下
            if (wCold > 0.3f) {
                target.setTicksFrozen((int) (wCold * 300));
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, (int) (wCold * 100), 2));
            }

            // 雷属性 (Motion): 雷ダメージ + 発光
            if (wMotion > 0.3f) {
                target.hurt(DamageSource.LIGHTNING_BOLT, baseDmg * wMotion * 1.5f);
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, (int) (wMotion * 60), 0));
            }

            // 混沌属性: ランダム状態異常
            if (wEntropy > 0.3f) {
                int count = (int) (wEntropy * 3);
                for (int i = 0; i < count; i++) {
                    int dice = this.level.random.nextInt(5);
                    int dur = (int) (wEntropy * 80);
                    switch (dice) {
                        case 0 -> living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.POISON, dur, 0));
                        case 1 -> living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.LEVITATION, dur / 2, 0));
                        case 2 -> living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.BLINDNESS, dur, 0));
                        case 3 -> living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.CONFUSION, dur, 0));
                        case 4 -> living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.WEAKNESS, dur, 1));
                    }
                }
            }

            // 神聖属性: アンデッド特効、通常は回復
            if (wDivine > 0.3f) {
                if (living.isInvertedHealAndHarm()) {
                    // アンデッド: 大ダメージ
                    target.hurt(DamageSource.MAGIC, baseDmg * wDivine * 3.0f);
                } else {
                    // 通常: 回復 (味方効果)
                    living.heal(baseDmg * wDivine * 0.5f);
                }
            }
        }
    }

    /**
     * ■ Phase A: ブロック影響
     */
    private void applyBlockEffect(PhysicsMetadata phy, VisualMetadata vis, float radius) {
        if (this.level.isClientSide)
            return;
        // 毎tickではなく一定間隔で実行（サーバー負荷軽減）
        if (this.lifeTime % 5 != 0)
            return;

        BlockPos center = this.blockPosition();
        float effectRadius = Math.max(radius, phy.areaOfEffect);

        // 高温: 着火、溶岩化
        if (phy.temperature > 1500.0f) {
            WorldEffectHelper.applyHeatEffect(this.level, center, effectRadius, phy.temperature);
        }

        // 低温: 凍結、氷化
        if (phy.temperature < 100.0f) {
            WorldEffectHelper.applyFreezeEffect(this.level, center, effectRadius);
        }

        // 爆発: RADIAL + 高エネルギー
        if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL && phy.energy > 100.0f && !hasExploded) {
            WorldEffectHelper.applyExplosiveEffect(this.level, center, effectRadius, phy.energy);
        }

        // ビーム貫通: DIRECTIONAL + 高速度
        if (phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL && phy.velocity > 5.0f) {
            Vec3 dir = this.getDeltaMovement().normalize();
            if (dir.lengthSqr() > 0.01) {
                WorldEffectHelper.applyBeamEffect(this.level, this.position(), dir, effectRadius * 2.0f, phy.energy);
            }
        }

        // バリア: FIELD
        if (phy.forceType == PhysicsMetadata.EnumForceType.FIELD && phy.isSolid && this.lifeTime == 5) {
            WorldEffectHelper.applyBarrierEffect(this.level, center, effectRadius);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTime = tag.getInt("LifeTime");
        this.maxLifeTime = tag.getInt("MaxLifeTime");
        this.hasExploded = tag.getBoolean("HasExploded");

        if (tag.contains("PhysicsData")) {
            PhysicsMetadata p = PhysicsMetadata.fromNBT(tag.getCompound("PhysicsData"));
            this.setPhysicsMetadata(p);
        }
        if (tag.contains("VisualData")) {
            VisualMetadata v = VisualMetadata.fromNBT(tag.getCompound("VisualData"));
            this.setVisualMetadata(v);
        }
        if (tag.contains("CommandData")) {
            setCommandList(tag.getList("CommandData", 10));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTime", lifeTime);
        tag.putInt("MaxLifeTime", maxLifeTime);
        tag.putBoolean("HasExploded", hasExploded);

        tag.put("PhysicsData", getPhysicsMetadata().toNBT());
        tag.put("VisualData", getVisualMetadata().toNBT());
        tag.put("CommandData", getCommandList());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}