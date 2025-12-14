package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.magic.*;
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

    private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANCHOR_TYPE = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);

    private final List<String> scriptCode = new ArrayList<>();
    private int castTime = 20;
    private int age = 0;
    private VisualMetadata cachedMetadata = null;

    public EntityMagicSequence(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityMagicSequence(Level level, LivingEntity caster, List<String> script, VisualMetadata meta, int castTime, Vec3 pos) {
        this(EntityRegister.MAGIC_SEQUENCE.get(), level);
        this.setPos(pos);
        this.scriptCode.addAll(script);
        this.castTime = castTime;

        this.entityData.set(COLOR_R, meta.mainColor.x());
        this.entityData.set(COLOR_G, meta.mainColor.y());
        this.entityData.set(COLOR_B, meta.mainColor.z());
        this.entityData.set(CASTER_ID, caster.getId());
        this.entityData.set(ANCHOR_TYPE, meta.anchorType.ordinal());

        this.setXRot(caster.getXRot());
        this.setYRot(caster.getYRot());

        CompoundTag metaTag = meta.toNBT();
        this.getPersistentData().put("VisualMeta", metaTag);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR_R, 1.0F);
        this.entityData.define(COLOR_G, 1.0F);
        this.entityData.define(COLOR_B, 1.0F);
        this.entityData.define(CASTER_ID, -1);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(ANCHOR_TYPE, EnumMagicAnchor.WORLD_FIXED.ordinal());
    }

    public void setTarget(LivingEntity target) {
        if (target != null) this.entityData.set(TARGET_ID, target.getId());
    }

    public EnumMagicAnchor getAnchorType() {
        return EnumMagicAnchor.values()[this.entityData.get(ANCHOR_TYPE)];
    }

    public VisualMetadata getVisualMetadata() {
        if (cachedMetadata == null) {
            if (this.getPersistentData().contains("VisualMeta")) {
                cachedMetadata = VisualMetadata.fromNBT(this.getPersistentData().getCompound("VisualMeta"));
            } else {
                cachedMetadata = new VisualMetadata();
                cachedMetadata.mainColor = new Vector3f(this.entityData.get(COLOR_R), this.entityData.get(COLOR_G), this.entityData.get(COLOR_B));
                cachedMetadata.anchorType = getAnchorType();
            }
        }
        return cachedMetadata;
    }

    public String getRendererID() { return "default"; }
    public Vector3f getColor() { return new Vector3f(this.entityData.get(COLOR_R), this.entityData.get(COLOR_G), this.entityData.get(COLOR_B)); }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        EnumMagicAnchor anchor = getAnchorType();
        Entity caster = level.getEntity(this.entityData.get(CASTER_ID));
        Entity target = level.getEntity(this.entityData.get(TARGET_ID));

        // ■■■ アンカー追従処理 ■■■

        // A. 術者追従 (CASTER) のみ位置を更新する
        if (anchor == EnumMagicAnchor.CASTER_ANCHORED) {
            if (caster != null) {
                Vec3 look = caster.getLookAngle();
                Vec3 targetPos = caster.getEyePosition().add(look.scale(1.5)); // 目の前1.5m

                Vec3 current = this.position();
                Vec3 next = current.lerp(targetPos, 0.5); // 補間移動
                this.setPos(next);

                this.setYRot(caster.getYRot());
                this.setXRot(caster.getXRot());
            }
        }
        // B. ターゲット追従 (TARGET)
        else if (anchor == EnumMagicAnchor.TARGET_ANCHORED) {
            if (target != null) {
                this.setPos(target.getX(), target.getY() + 0.1, target.getZ());
                this.setXRot(-90.0F); // 地面に水平
            }
        }
        // C. RANDOM_AIR, WORLD_FIXED は動かない（生成された場所に留まる）

        if (!this.level.isClientSide) {
            if (caster == null || !caster.isAlive()) {
                this.discard();
                return;
            }

            // 発動タイミング
            if (this.age >= this.castTime) {
                LivingEntity livingCaster = (LivingEntity)caster;

                SpellContext ctx = new SpellContext(level, livingCaster);
                // ★重要: 発動地点を「現在の魔法陣の位置」に設定
                ctx.setLocation(this.position(), this.getXRot(), this.getYRot());
                ctx.script.addAll(this.scriptCode);
                ctx.fromSequence = true; // 再帰防止

                if (target instanceof LivingEntity livingTarget) {
                    ctx.target = livingTarget;
                }

                try {
                    MagicScriptEngine.execute(ctx, this.scriptCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                this.discard();
            }
        }
    }

    // ... (Save/Load/Packetメソッドは変更なし) ...
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.castTime = tag.getInt("CastTime");
        this.scriptCode.clear();
        if (tag.contains("Script", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Script", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) this.scriptCode.add(list.getString(i));
        }
        this.entityData.set(COLOR_R, tag.getFloat("CR"));
        this.entityData.set(COLOR_G, tag.getFloat("CG"));
        this.entityData.set(COLOR_B, tag.getFloat("CB"));
        this.entityData.set(CASTER_ID, tag.getInt("Caster"));
        this.entityData.set(TARGET_ID, tag.getInt("TargetID"));
        this.entityData.set(ANCHOR_TYPE, tag.getInt("Anchor"));
        if (tag.contains("VisualMeta")) {
            this.getPersistentData().put("VisualMeta", tag.getCompound("VisualMeta"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("CastTime", this.castTime);
        ListTag list = new ListTag();
        for (String s : scriptCode) list.add(StringTag.valueOf(s));
        tag.put("Script", list);
        tag.putFloat("CR", this.entityData.get(COLOR_R));
        tag.putFloat("CG", this.entityData.get(COLOR_G));
        tag.putFloat("CB", this.entityData.get(COLOR_B));
        tag.putInt("Caster", this.entityData.get(CASTER_ID));
        tag.putInt("TargetID", this.entityData.get(TARGET_ID));
        tag.putInt("Anchor", this.entityData.get(ANCHOR_TYPE));
        if (this.getPersistentData().contains("VisualMeta")) {
            tag.put("VisualMeta", this.getPersistentData().getCompound("VisualMeta"));
        }
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}