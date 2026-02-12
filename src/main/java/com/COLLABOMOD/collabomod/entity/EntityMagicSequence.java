package com.COLLABOMOD.collabomod.entity;

import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.magic.SpellExecutor;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

public class EntityMagicSequence extends Entity {

    private static final EntityDataAccessor<Integer> CASTER_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> PHYSICS_DATA = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> VISUAL_DATA = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(EntityMagicSequence.class, EntityDataSerializers.INT);

    // 詠唱時間 (tick)
    private int castTime = 0;

    private int tickCounter = 0;
    private List<String> script = new ArrayList<>();
    private PhysicsMetadata physics = new PhysicsMetadata();
    private VisualMetadata visuals = new VisualMetadata();

    public EntityMagicSequence(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityMagicSequence(SpellContext ctx, int castTime) {
        this(EntityRegister.MAGIC_SEQUENCE.get(), ctx.level);
        if (ctx.caster != null) {
            this.entityData.set(CASTER_ID, ctx.caster.getId());
        }
        if (ctx.target != null) {
            this.entityData.set(TARGET_ID, ctx.target.getId());
        }
        this.script = ctx.script;
        this.physics = ctx.physics;
        this.visuals = ctx.visuals;
        this.castTime = castTime;

        // ■ 追加: SynchedDataに初期値をセット
        this.entityData.set(PHYSICS_DATA, this.physics.toNBT());
        this.entityData.set(VISUAL_DATA, this.visuals.toNBT());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CASTER_ID, -1);
        this.entityData.define(PHYSICS_DATA, new CompoundTag());
        this.entityData.define(VISUAL_DATA, new CompoundTag());
        this.entityData.define(TARGET_ID, -1);
    }

    // ■ 追加: レンダラーからアクセスするためのGetter
    public int getCastTime() {
        return this.castTime;
    }

    // クライアント側でNBT同期データからVisualMetadataを復元するメソッド
    public VisualMetadata getVisualMetadata() {
        // ■ 修正: サーバーから同期されたデータからVisualMetadataを復元する
        CompoundTag tag = this.entityData.get(VISUAL_DATA);
        if (tag.isEmpty()) {
            return new VisualMetadata(); // まだ同期されていない場合はデフォルトを返す
        }
        return VisualMetadata.fromNBT(tag);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            tickCounter++;
            if (tickCounter >= castTime) {
                executeSpell();
                this.discard();
            }
        }
    }

    private void executeSpell() {
        if (!(this.level instanceof ServerLevel)) return;

        Entity casterEntity = this.level.getEntity(this.entityData.get(CASTER_ID));
        LivingEntity caster = (casterEntity instanceof LivingEntity) ? (LivingEntity) casterEntity : null;

        Entity targetEntity = this.level.getEntity(this.entityData.get(TARGET_ID));
        LivingEntity target = (targetEntity instanceof LivingEntity) ? (LivingEntity) targetEntity : null;

        SpellContext ctx = new SpellContext();
        ctx.level = this.level;
        ctx.caster = caster;
        ctx.origin = this.position();
        ctx.script = this.script;
        ctx.target = target; // ■ 追加: ターゲット情報を引き継ぐ

        // ■ 修正: SynchedDataからPhysicsとVisualsを復元
        ctx.physics = PhysicsMetadata.fromNBT(this.entityData.get(PHYSICS_DATA));
        ctx.visuals = VisualMetadata.fromNBT(this.entityData.get(VISUAL_DATA));

        ctx.fromSequence = true;
        SpellExecutor.execute(ctx);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCounter = tag.getInt("Tick");
        this.castTime = tag.getInt("CastTime"); // 保存データの読み込み
        this.entityData.set(TARGET_ID, tag.getInt("TargetID"));

        if (tag.contains("Script")) {
            ListTag list = tag.getList("Script", 8);
            script.clear();
            for (int i = 0; i < list.size(); i++) {
                script.add(list.getString(i));
            }
        }
        // ■ 追加: チャンクアンロード対策でメタデータも読み込む
        if (tag.contains("Physics")) {
            this.physics = PhysicsMetadata.fromNBT(tag.getCompound("Physics"));
        }
        if (tag.contains("Visuals")) {
            this.visuals = VisualMetadata.fromNBT(tag.getCompound("Visuals"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Tick", tickCounter);
        tag.putInt("CastTime", castTime); // 保存
        tag.putInt("TargetID", this.entityData.get(TARGET_ID));

        ListTag list = new ListTag();
        for (String s : script) {
            list.add(StringTag.valueOf(s));
        }
        tag.put("Script", list);

        // ■ 追加: チャンクアンロード対策でメタデータも保存
        tag.put("Physics", this.physics.toNBT());
        tag.put("Visuals", this.visuals.toNBT());
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}