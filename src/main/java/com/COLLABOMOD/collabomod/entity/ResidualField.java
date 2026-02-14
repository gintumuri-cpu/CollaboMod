package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.WorldEffectHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

/**
 * ■ Phase B: 残留効果フィールド
 * 魔法が消滅した後、一定時間エリアに効果が残る透明エンティティ。
 * 灼熱/凍結/帯電/混沌/神聖の5種類の残留効果をサポートする。
 */
public class ResidualField extends Entity {

    public enum FieldType {
        HEAT, // 灼熱: 継続ダメージ + 着火
        FROST, // 凍結: 減速 + 凍結ダメージ
        ELECTRIC, // 帯電: 周期的ダメージ + 発光
        CHAOS, // 混沌: ランダム状態異常
        HOLY // 神聖: 味方回復 + アンデッド特効
    }

    private static final EntityDataAccessor<Integer> FIELD_TYPE_ID = SynchedEntityData.defineId(ResidualField.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FIELD_RADIUS = SynchedEntityData.defineId(ResidualField.class,
            EntityDataSerializers.FLOAT);

    private int lifeTime = 0;
    private int maxLifeTime = 100;
    private float damage = 2.0f;

    public ResidualField(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ResidualField(EntityType<?> type, Level level, FieldType fieldType, float radius, int duration,
            float damage) {
        this(type, level);
        this.entityData.set(FIELD_TYPE_ID, fieldType.ordinal());
        this.entityData.set(FIELD_RADIUS, radius);
        this.maxLifeTime = duration;
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(FIELD_TYPE_ID, 0);
        this.entityData.define(FIELD_RADIUS, 2.0f);
    }

    public FieldType getFieldType() {
        int id = this.entityData.get(FIELD_TYPE_ID);
        FieldType[] types = FieldType.values();
        return (id >= 0 && id < types.length) ? types[id] : FieldType.HEAT;
    }

    public float getFieldRadius() {
        return this.entityData.get(FIELD_RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        lifeTime++;

        if (!this.level.isClientSide) {
            if (lifeTime > maxLifeTime) {
                this.discard();
                return;
            }

            // 5tickごとにエリア効果を適用
            if (lifeTime % 5 == 0) {
                applyFieldEffect();
            }

            // 10tickごとにブロック効果を適用 (負荷軽減)
            if (lifeTime % 10 == 0) {
                applyBlockFieldEffect();
            }
        }
    }

    private void applyFieldEffect() {
        FieldType type = getFieldType();
        float radius = getFieldRadius();
        AABB area = this.getBoundingBox().inflate(radius);
        List<Entity> entities = this.level.getEntities(this, area);

        for (Entity entity : entities) {
            if (entity instanceof ResidualField)
                continue;
            if (!(entity instanceof LivingEntity living))
                continue;

            switch (type) {
                case HEAT:
                    entity.setSecondsOnFire(3);
                    entity.hurt(DamageSource.IN_FIRE, damage * 0.3f);
                    break;

                case FROST:
                    entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + 40, 300));
                    entity.hurt(DamageSource.FREEZE, damage * 0.2f);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    break;

                case ELECTRIC:
                    entity.hurt(DamageSource.LIGHTNING_BOLT, damage * 0.4f);
                    living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
                    break;

                case CHAOS:
                    // ランダム状態異常
                    int dice = this.level.random.nextInt(5);
                    switch (dice) {
                        case 0 -> living.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                        case 1 -> living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        case 2 -> living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                        case 3 -> living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                        case 4 -> living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                    }
                    break;

                case HOLY:
                    if (living.isInvertedHealAndHarm()) {
                        // アンデッド: ダメージ
                        entity.hurt(DamageSource.MAGIC, damage * 0.5f);
                    } else {
                        // 通常: 回復
                        living.heal(damage * 0.2f);
                    }
                    break;
            }
        }
    }

    private void applyBlockFieldEffect() {
        FieldType type = getFieldType();
        float radius = getFieldRadius();
        BlockPos center = this.blockPosition();

        switch (type) {
            case HEAT:
                WorldEffectHelper.applyHeatEffect(this.level, center, radius * 0.5f, 1600.0f);
                break;
            case FROST:
                WorldEffectHelper.applyFreezeEffect(this.level, center, radius * 0.5f);
                break;
            default:
                break;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTime = tag.getInt("LifeTime");
        this.maxLifeTime = tag.getInt("MaxLifeTime");
        this.damage = tag.getFloat("Damage");
        this.entityData.set(FIELD_TYPE_ID, tag.getInt("FieldType"));
        this.entityData.set(FIELD_RADIUS, tag.getFloat("Radius"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTime", lifeTime);
        tag.putInt("MaxLifeTime", maxLifeTime);
        tag.putFloat("Damage", damage);
        tag.putInt("FieldType", this.entityData.get(FIELD_TYPE_ID));
        tag.putFloat("Radius", this.entityData.get(FIELD_RADIUS));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
