package com.COLLABOMOD.collabomod.entity;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import com.COLLABOMOD.collabomod.util.MagicSpellType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class EntityMagicSequence extends Entity {

    private static final EntityDataAccessor<Integer> SPELL_TYPE_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);

    private int age = 0;

    public EntityMagicSequence(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityMagicSequence(Level level, LivingEntity caster, MagicSpellType type, Vec3 pos) {
        this(EntityRegister.MAGIC_SEQUENCE.get(), level);
        this.setPos(pos);
        this.entityData.set(SPELL_TYPE_ID, type.ordinal());
        this.entityData.set(CASTER_ID, caster.getId());

        if (type.isAttachedToCaster) {
            this.setXRot(caster.getXRot());
            this.setYRot(caster.getYRot());
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SPELL_TYPE_ID, 0);
        this.entityData.define(CASTER_ID, -1);
    }

    public MagicSpellType getSpellType() {
        int id = this.entityData.get(SPELL_TYPE_ID);
        if (id < 0 || id >= MagicSpellType.values().length) return MagicSpellType.AIR_BULLET;
        return MagicSpellType.values()[id];
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        MagicSpellType type = getSpellType();

        if (!this.level.isClientSide) {
            Entity casterEntity = level.getEntity(this.entityData.get(CASTER_ID));
            if (casterEntity == null || !casterEntity.isAlive()) {
                this.discard();
                return;
            }

            // 手元追従型の場合の位置更新（グラムデモリッションなど）
            if (type.isAttachedToCaster && casterEntity instanceof LivingEntity livingCaster) {
                Vec3 look = livingCaster.getLookAngle();
                Vec3 pos = livingCaster.getEyePosition().add(look.scale(1.5));
                this.setPos(pos);
                this.setXRot(livingCaster.getXRot());
                this.setYRot(livingCaster.getYRot());
            }

            if (this.age >= type.castTime) {
                executeSpell(type, (LivingEntity)casterEntity);
                this.discard();
            }
        }
    }

    private void executeSpell(MagicSpellType type, LivingEntity caster) {
        switch (type) {
            // ■ エア・バレットの発射処理
            case AIR_BULLET:
                EntityAirBullet bullet = new EntityAirBullet(level, caster);
                bullet.setPos(this.getX(), this.getY(), this.getZ()); // 魔法陣の位置から

                // ■ 修正: 魔法陣の向き（this.get...）を使って発射ベクトルを決める
                // これで「魔法陣が向いている方向＝ターゲット」に飛びます
                bullet.shootFromRotation(this, this.getXRot(), this.getYRot(), 0.0F, 3.0F, 0.5F);

                level.addFreshEntity(bullet);
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
                break;

            default:
                break;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.entityData.set(SPELL_TYPE_ID, tag.getInt("SpellType"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("SpellType", this.entityData.get(SPELL_TYPE_ID));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}