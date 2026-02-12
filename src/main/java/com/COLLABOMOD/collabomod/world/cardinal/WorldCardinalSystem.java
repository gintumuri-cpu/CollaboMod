package com.COLLABOMOD.collabomod.world.cardinal;

import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class WorldCardinalSystem extends SavedData {

    private static final String DATA_NAME = "collabomod_cardinal_system";

    // チャンクごとの環境データを保持
    private final Map<Long, EnvironmentChunkData> chunkDataMap = new HashMap<>();
    private final ServerLevel level;

    // 新規作成用コンストラクタ
    public WorldCardinalSystem(ServerLevel level) {
        this.level = level;
    }

    // ロード用コンストラクタ
    public WorldCardinalSystem(CompoundTag tag, ServerLevel level) {
        this.level = level;
        load(tag);
    }

    // インスタンス取得 (Get or Create)
    public static WorldCardinalSystem get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                tag -> new WorldCardinalSystem(tag, level),
                () -> new WorldCardinalSystem(level),
                DATA_NAME
        );
    }

    // 毎Tickの処理 (CardinalTickHandlerから呼ばれる)
    public void tick(ServerLevel level) {
        // 各チャンクデータの更新処理 (EnvironmentChunkData#tick を呼び出し)
        chunkDataMap.forEach((key, data) -> data.tick());

        // 変更があれば保存フラグを立てる
        setDirty();
    }

    // チャンクデータの取得
    public EnvironmentChunkData getChunkData(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        return chunkDataMap.computeIfAbsent(chunkKey, k -> new EnvironmentChunkData());
    }

    /**
     * 物理現象エンティティをスポーンさせる
     * 修正: EntitySciencePhenomenonの最新仕様（コンストラクタ変更）に対応
     */
    public void spawnPhenomenon(BlockPos pos, PhysicsMetadata physics, VisualMetadata visuals) {
        // 1. エンティティ生成 (Environment起因のためCasterはnull)
        EntitySciencePhenomenon phenomenon = new EntitySciencePhenomenon(level, null);

        // 2. 位置設定
        phenomenon.setPos(Vec3.atCenterOf(pos));

        // 3. パラメータ注入
        phenomenon.setPhysicsMetadata(physics);
        phenomenon.setVisualMetadata(visuals);

        // 4. ワールドに追加
        level.addFreshEntity(phenomenon);
    }

    // 環境起因の簡易爆発などを起こすヘルパー
    public void createEnvironmentalBurst(BlockPos pos, float energy, float radius) {
        PhysicsMetadata physics = new PhysicsMetadata();
        physics.energy = energy;
        physics.forceType = PhysicsMetadata.EnumForceType.RADIAL;
        physics.velocity = 0.5f;
        physics.temperature = 1000.0f; // 高温

        VisualMetadata visuals = new VisualMetadata();
        visuals.scale = radius;
        visuals.mainColor = new Vector3f(1.0f, 0.4f, 0.0f); // オレンジ
        visuals.shape = com.COLLABOMOD.collabomod.magic.EnumMagicShape.SPHERE;
        // VisualMetadataのフィールド仕様変更に伴い、アニメーションタイプ等はTimelineに含まれるか、
        // もしくはVisualMetadataの互換フィールドとして設定します。
        // ここではVisualMetadataにanimationTypeフィールドが残っている前提（前回の修正準拠）で記述します。
        visuals.animationType = com.COLLABOMOD.collabomod.magic.EnumMagicAnimation.EXPAND_FADE;

        spawnPhenomenon(pos, physics, visuals);
    }

    public void addTemperature(Level level, BlockPos pos, float amount) {
        // クライアント側や、管理外のLevelであれば無視
        if (level != this.level) return;

        EnvironmentChunkData data = getChunkData(pos);
        float current = data.getTemperature(pos);
        // 新しい温度をセット (EnvironmentChunkData内でDirtyフラグ等は処理される)
        data.setTemperature(pos, current + amount);
    }

    // --- NBT Save/Load ---

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        chunkDataMap.forEach((key, data) -> {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("ChunkKey", key);
            // EnvironmentChunkData#serializeNBT を呼び出し
            chunkTag.put("Data", data.serializeNBT());
            list.add(chunkTag);
        });
        tag.put("ChunkData", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("ChunkData")) {
            ListTag list = tag.getList("ChunkData", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag chunkTag = list.getCompound(i);
                long key = chunkTag.getLong("ChunkKey");

                EnvironmentChunkData data = new EnvironmentChunkData();
                // EnvironmentChunkData#deserializeNBT を呼び出し
                data.deserializeNBT(chunkTag.getCompound("Data"));

                chunkDataMap.put(key, data);
            }
        }
    }
}