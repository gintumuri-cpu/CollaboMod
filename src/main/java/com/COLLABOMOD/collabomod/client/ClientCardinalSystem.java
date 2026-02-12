package com.COLLABOMOD.collabomod.client;

import com.COLLABOMOD.collabomod.world.cardinal.EnvironmentChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class ClientCardinalSystem {

    // クライアント側で保持するチャンクごとの環境データキャッシュ
    private static final Map<Long, EnvironmentChunkData> clientChunkMap = new HashMap<>();

    // サーバーからのパケット受信処理 (温度更新)
    public static void receiveUpdate(long chunkKey, Map<Long, Float> updates) {
        EnvironmentChunkData data = clientChunkMap.computeIfAbsent(chunkKey, k -> new EnvironmentChunkData());
        updates.forEach((localPos, temp) -> {
            // ■ 修正: 専用のSetterを使用
            data.setTemperatureDirect(localPos, temp);
        });
    }

    public static float getTemperature(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        if (clientChunkMap.containsKey(chunkKey)) {
            return clientChunkMap.get(chunkKey).getTemperature(pos);
        }
        return 300.0F; // データがない場合は常温
    }

    // 温度勾配ベクトルを取得 (Flow Field)
    // 温度が高い方向へのベクトルを返す
    public static Vec3 getThermalGradient(BlockPos pos) {
        float tCenter = getTemperature(pos);
        float tX = getTemperature(pos.east());
        float tX_ = getTemperature(pos.west());
        float tY = getTemperature(pos.above());
        float tY_ = getTemperature(pos.below());
        float tZ = getTemperature(pos.south());
        float tZ_ = getTemperature(pos.north());

        // 勾配計算 (Central Difference)
        double dx = (tX - tX_) * 0.5;
        double dy = (tY - tY_) * 0.5;
        double dz = (tZ - tZ_) * 0.5;

        // 温度差が小さい場合はゼロベクトル
        if (Math.abs(dx) < 1.0 && Math.abs(dy) < 1.0 && Math.abs(dz) < 1.0) {
            return Vec3.ZERO;
        }

        return new Vec3(dx, dy, dz).normalize();
    }

    public static float getPsionDensity(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        if (clientChunkMap.containsKey(chunkKey)) {
            return clientChunkMap.get(chunkKey).getPsionDensity(pos);
        }
        return 0.0F; // デフォルトは0
    }

    public static Map<Long, EnvironmentChunkData> getAllData() {
        return clientChunkMap;
    }

    public static int getMagicHash(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        if (clientChunkMap.containsKey(chunkKey)) {
            return clientChunkMap.get(chunkKey).getMagicHash(pos);
        }
        return 0;
    }
}