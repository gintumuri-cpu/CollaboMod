package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.magic.MagicComponentType;
import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.magic.SpellExecutor;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

public class EntityMagicSequence extends Entity {

    // 同期データ
    private static final EntityDataAccessor<String> VISUAL_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);

    // 実行用コンポーネントリスト
    private final List<MagicComponentType> components = new ArrayList<>();
    private int castTime = 20;
    private int age = 0;

    public EntityMagicSequence(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityMagicSequence(Level level, LivingEntity caster, List<MagicComponentType> components, VisualMetadata meta, int castTime, Vec3 pos) {
        this(EntityRegister.MAGIC_SEQUENCE.get(), level);
        this.setPos(pos);

        this.components.addAll(components);
        this.castTime = castTime;

        this.entityData.set(VISUAL_ID, meta.rendererID);
        this.entityData.set(COLOR_R, meta.mainColor.x());
        this.entityData.set(COLOR_G, meta.mainColor.y());
        this.entityData.set(COLOR_B, meta.mainColor.z());
        this.entityData.set(CASTER_ID, caster.getId());

        this.setXRot(caster.getXRot());
        this.setYRot(caster.getYRot());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VISUAL_ID, "default");
        this.entityData.define(COLOR_R, 1.0F);
        this.entityData.define(COLOR_G, 1.0F);
        this.entityData.define(COLOR_B, 1.0F);
        this.entityData.define(CASTER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
    }

    public void setTarget(LivingEntity target) {
        if (target != null) this.entityData.set(TARGET_ID, target.getId());
    }

    public String getRendererID() { return this.entityData.get(VISUAL_ID); }
    public Vector3f getColor() { return new Vector3f(this.entityData.get(COLOR_R), this.entityData.get(COLOR_G), this.entityData.get(COLOR_B)); }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (!this.level.isClientSide) {
            Entity casterEntity = level.getEntity(this.entityData.get(CASTER_ID));
            if (casterEntity == null || !casterEntity.isAlive()) {
                this.discard();
                return;
            }

            if (this.age >= this.castTime) {
                LivingEntity caster = (LivingEntity)casterEntity;

                // Context作成
                SpellContext ctx = new SpellContext(level, caster);
                ctx.setLocation(this.position(), this.getXRot(), this.getYRot());

                int targetId = this.entityData.get(TARGET_ID);
                if (targetId != -1) {
                    Entity t = level.getEntity(targetId);
                    if (t instanceof LivingEntity livingTarget) ctx.target = livingTarget;
                }

                // コンポーネント適用
                for (MagicComponentType comp : components) {
                    comp.apply(ctx);
                }

                // ■ 実行
                SpellExecutor.execute(ctx);

                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.castTime = tag.getInt("CastTime");

        this.components.clear();
        ListTag list = tag.getList("Components", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                this.components.add(MagicComponentType.valueOf(list.getString(i)));
            } catch (Exception ignored) {}
        }

        this.entityData.set(VISUAL_ID, tag.getString("VisID"));
        this.entityData.set(COLOR_R, tag.getFloat("CR"));
        this.entityData.set(COLOR_G, tag.getFloat("CG"));
        this.entityData.set(COLOR_B, tag.getFloat("CB"));
        this.entityData.set(CASTER_ID, tag.getInt("Caster"));
        this.entityData.set(TARGET_ID, tag.getInt("TargetID"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("CastTime", this.castTime);

        ListTag list = new ListTag();
        for (MagicComponentType comp : components) {
            list.add(StringTag.valueOf(comp.name()));
        }
        tag.put("Components", list);

        tag.putString("VisID", getRendererID());
        tag.putFloat("CR", this.entityData.get(COLOR_R));
        tag.putFloat("CG", this.entityData.get(COLOR_G));
        tag.putFloat("CB", this.entityData.get(COLOR_B));
        tag.putInt("Caster", this.entityData.get(CASTER_ID));
        tag.putInt("TargetID", this.entityData.get(TARGET_ID));
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}