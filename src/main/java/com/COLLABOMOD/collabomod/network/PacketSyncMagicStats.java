package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.client.ClientPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
public class PacketSyncMagicStats {
    private final int currentPsion;
    private final int maxPsion;
    private final int calculationArea;
    private final int mentalLoad;

    // コンストラクタ：データをセットする
    public PacketSyncMagicStats(int currentPsion, int maxPsion, int calculationArea, int mentalLoad) {
        this.currentPsion = currentPsion;
        this.maxPsion = maxPsion;
        this.calculationArea = calculationArea;
        this.mentalLoad = mentalLoad;
    }

    // デコード：バイト列からデータを取り出す（受信時）
    public PacketSyncMagicStats(FriendlyByteBuf buf) {
        this.currentPsion = buf.readInt();
        this.maxPsion = buf.readInt();
        this.calculationArea = buf.readInt();
        this.mentalLoad = buf.readInt();
    }

    // エンコード：データをバイト列に変換する（送信時）
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(currentPsion);
        buf.writeInt(maxPsion);
        buf.writeInt(calculationArea);
        buf.writeInt(mentalLoad);
    }

    // ハンドル：データを受け取った後の処理（クライアント側で実行）

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // クライアント側で受信した場合のみ処理
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 専用ハンドラーに丸投げする（これでサーバー側でのクラッシュを回避）
                ClientPacketHandler.handlePacket(this.currentPsion, this.maxPsion, this.calculationArea, this.mentalLoad);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
