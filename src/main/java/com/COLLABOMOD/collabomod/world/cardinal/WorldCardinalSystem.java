package com.COLLABOMOD.collabomod.world.cardinal;

import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketSyncCardinalData;
import com.COLLABOMOD.collabomod.science.PhenomenonType;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class WorldCardinalSystem extends SavedData {

    private static final String DATA_NAME = "collabomod_world_cardinal";
    private final Map<Long, EnvironmentChunkData> chunkDataMap = new HashMap<>();
    private final Random random = new Random();

    // オートバランサー用変数
    private float currentLoadStress = 0.0F; // 0.0(快適) ~ 1.0(限界)

    public static WorldCardinalSystem get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                WorldCardinalSystem::load,
                WorldCardinalSystem::create,
                DATA_NAME
        );
    }

    public static WorldCardinalSystem create() {
        return new WorldCardinalSystem();
    }

    public void tick(ServerLevel level) {
        // ■ Phase 4: 自律負荷監視 (Auto-Balancer)
        // MSPT (Milliseconds Per Tick) を取得。50msを超えるとラグ発生。
        float mspt = level.getServer().getAverageTickTime();

        // 負荷ストレス値の計算 (40msから警戒開始、60msでストレスMAX)
        this.currentLoadStress = Math.max(0.0F, Math.min(1.0F, (mspt - 40.0F) / 20.0F));

        // ストレスが高い場合、ログを出す（デバッグ用、完成後は削除可）
        if (this.currentLoadStress > 0.8F && level.getGameTime() % 100 == 0) {
            System.out.println("Cardinal System: High Load Detected! Engaging Emergency Cooling. (Stress: " + currentLoadStress + ")");
        }

        for (long chunkKey : new HashSet<>(chunkDataMap.keySet())) {
            EnvironmentChunkData data = chunkDataMap.get(chunkKey);
            processChunk(level, chunkKey, data);

            if (data.isDirty()) {
                syncChunkToClients(level, chunkKey, data);
                data.clearDirty();
                this.setDirty();
            }
        }
    }

    private void processChunk(ServerLevel level, long chunkKey, EnvironmentChunkData data) {
        Map<Long, Float> temps = data.getAllTemperatures();
        ChunkPos cp = new ChunkPos(chunkKey);
        HashSet<Long> toRemove = new HashSet<>();

        // ■ 動的パラメータ設定
        // 負荷が高いほど「減衰（冷却）」を速くし、「生成（アノマリー）」を抑制する
        float baseDecay = 0.05F; // 基本5%
        float dynamicDecay = baseDecay * (1.0F + (currentLoadStress * 20.0F)); // ストレスMAXなら20倍速で冷却

        float baseChance = 0.0001F; // 基本0.01%
        float dynamicChance = baseChance * (1.0F - currentLoadStress); // ストレスMAXなら生成ゼロ

        for (long localPosLong : new HashSet<>(temps.keySet())) {
            float currentTemp = temps.get(localPosLong);

            // 1. 動的熱拡散
            float diff = 300.0F - currentTemp;
            float nextTemp = currentTemp + (diff * dynamicDecay);

            // 2. 異常高温処理
            if (currentTemp > 2000.0F) {
                // 強制冷却も負荷に応じて強化
                float forcedCooling = 0.2F + (currentLoadStress * 0.8F); // 最大100%冷却（即時消去）
                nextTemp -= (currentTemp - 2000.0F) * forcedCooling;

                // アノマリー生成判定 (負荷に応じて確率変動)
                if (dynamicChance > 0 && random.nextFloat() < dynamicChance) {
                    int lx = BlockPos.getX(localPosLong);
                    int ly = BlockPos.getY(localPosLong);
                    int lz = BlockPos.getZ(localPosLong);
                    BlockPos worldPos = cp.getBlockAt(lx, ly, lz);

                    spawnAnomaly(level, worldPos, currentTemp);

                    // エネルギー消費（即時冷却）
                    nextTemp = 300.0F;
                }
            }
            // 3. 環境干渉 (ここも負荷が高い時はスキップして軽量化)
            else if (currentLoadStress < 0.5F && random.nextFloat() < 0.1F) {
                int lx = BlockPos.getX(localPosLong);
                int ly = BlockPos.getY(localPosLong);
                int lz = BlockPos.getZ(localPosLong);
                BlockPos worldPos = cp.getBlockAt(lx, ly, lz);

                if (currentTemp > 1000.0F) {
                    if (level.isEmptyBlock(worldPos) && level.getBlockState(worldPos.below()).isFlammable(level, worldPos.below(), net.minecraft.core.Direction.UP)) {
                        level.setBlockAndUpdate(worldPos, Blocks.FIRE.defaultBlockState());
                    } else if (level.getBlockState(worldPos).is(Blocks.ICE)) {
                        level.setBlockAndUpdate(worldPos, Blocks.WATER.defaultBlockState());
                    }
                    // 水蒸発ロジック
                    else if (level.getBlockState(worldPos).is(Blocks.WATER)) {
                        level.setBlockAndUpdate(worldPos, Blocks.AIR.defaultBlockState());
                        level.sendParticles(ParticleTypes.CLOUD, worldPos.getX()+0.5, worldPos.getY()+0.5, worldPos.getZ()+0.5, 3, 0.2, 0.2, 0.2, 0.05);
                    }
                } else if (currentTemp < 200.0F) {
                    if (level.getBlockState(worldPos).is(Blocks.WATER)) {
                        level.setBlockAndUpdate(worldPos, Blocks.ICE.defaultBlockState());
                    } else if (level.getBlockState(worldPos).is(Blocks.FIRE)) {
                        level.removeBlock(worldPos, false);
                    }
                }
            }

            if (Math.abs(300.0F - nextTemp) < 1.0F) {
                toRemove.add(localPosLong);
            } else {
                data.setTemperature(BlockPos.of(localPosLong), nextTemp);
            }
        }

        for (long key : toRemove) {
            data.setTemperature(BlockPos.of(key), 300.0F);
        }
    }

    private void spawnAnomaly(ServerLevel level, BlockPos pos, float temp) {
        // ... (前回と同じ) ...
        ScienceContext ctx = new ScienceContext();
        ctx.energy = temp / 500.0F;
        ctx.temperature = temp;
        ctx.radius = 1.5F;
        ctx.velocity = 0.0F;
        ctx.type = PhenomenonType.SPHERE_EXPANSION;

        EntitySciencePhenomenon phenomenon = new EntitySciencePhenomenon(
                level,
                Vec3.atCenterOf(pos),
                ctx,
                null
        );
        level.addFreshEntity(phenomenon);
    }

    // ... (同期、NBTメソッドは変更なし) ...
    // 以下、前回のコードと同様の内容を維持してください
    private void syncChunkToClients(ServerLevel level, long chunkKey, EnvironmentChunkData data) {
        ChunkPos cp = new ChunkPos(chunkKey);
        LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
        if (chunk != null) {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> chunk),
                    new PacketSyncCardinalData(chunkKey, data.getAllTemperatures())
            );
        }
    }
    public void addTemperature(Level level, BlockPos pos, float amount) {
        ChunkPos chunkPos = new ChunkPos(pos);
        long chunkKey = chunkPos.toLong();
        EnvironmentChunkData data = chunkDataMap.computeIfAbsent(chunkKey, k -> new EnvironmentChunkData());
        float current = data.getTemperature(pos);
        data.setTemperature(pos, current + amount);
        this.setDirty();
    }
    public static WorldCardinalSystem load(CompoundTag nbt) {
        WorldCardinalSystem system = new WorldCardinalSystem();
        if (nbt.contains("Chunks")) {
            ListTag list = nbt.getList("Chunks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag chunkTag = list.getCompound(i);
                long key = chunkTag.getLong("K");
                EnvironmentChunkData data = new EnvironmentChunkData();
                data.deserializeNBT(chunkTag.getCompound("D"));
                system.chunkDataMap.put(key, data);
            }
        }
        return system;
    }
    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        chunkDataMap.forEach((key, data) -> {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("K", key);
            chunkTag.put("D", data.serializeNBT());
            list.add(chunkTag);
        });
        nbt.put("Chunks", list);
        return nbt;
    }
}