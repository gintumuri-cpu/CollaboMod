package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.client.ClientCardinalSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncCardinalData {
    // 送信するデータ: チャンク内のローカル座標(Long) -> 温度(Float)
    private final Map<Long, Float> updateMap;
    private final long chunkKey;

    public PacketSyncCardinalData(long chunkKey, Map<Long, Float> map) {
        this.chunkKey = chunkKey;
        this.updateMap = map;
    }

    public PacketSyncCardinalData(FriendlyByteBuf buf) {
        this.chunkKey = buf.readLong();
        int size = buf.readInt();
        this.updateMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            this.updateMap.put(buf.readLong(), buf.readFloat());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(chunkKey);
        buf.writeInt(updateMap.size());
        updateMap.forEach((pos, temp) -> {
            buf.writeLong(pos);
            buf.writeFloat(temp);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアント側のシステムにデータを渡す
            ClientCardinalSystem.receiveUpdate(chunkKey, updateMap);
        });
        ctx.get().setPacketHandled(true);
    }
}
