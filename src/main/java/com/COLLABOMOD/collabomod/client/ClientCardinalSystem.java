package com.COLLABOMOD.collabomod.client;

import com.COLLABOMOD.collabomod.world.cardinal.EnvironmentChunkData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Map;

public class ClientCardinalSystem {

    // クライアント用のデータキャッシュ
    private static final Map<Long, EnvironmentChunkData> clientChunkMap = new HashMap<>();

    public static void receiveUpdate(long chunkKey, Map<Long, Float> updates) {
        EnvironmentChunkData data = clientChunkMap.computeIfAbsent(chunkKey, k -> new EnvironmentChunkData());

        updates.forEach((localPos, temp) -> {
            // ローカル座標キーからBlockPosを復元してセット（EnvironmentChunkDataの実装に依存）
            // EnvironmentChunkDataは内部でキー管理しているので、ここでは値を注入する手段が必要
            // 今回はEnvironmentChunkDataに直接putするメソッドを追加するか、
            // 簡易的に以下のヘルパーを通してセットする想定
            data.directSet(localPos, temp);
        });
    }

    public static float getTemperature(BlockPos pos) {
        long chunkKey = new ChunkPos(pos).toLong();
        if (clientChunkMap.containsKey(chunkKey)) {
            return clientChunkMap.get(chunkKey).getTemperature(pos);
        }
        return 300.0F; // データがなければ常温
    }

    // レンダラー用: アクティブな全チャンクデータを取得
    public static Map<Long, EnvironmentChunkData> getAllData() {
        return clientChunkMap;
    }
}
