package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
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
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // ここはクライアントサイドのスレッドで実行される
            // クライアントのプレイヤーを取得
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                    stats.setCurrentPsion(this.currentPsion);
                    stats.setMaxPsion(this.maxPsion);
                    // 演算規模は本来固定だが、表示用に同期する
                    // ※Setterがない場合はMagicStatsに追加するか、直接フィールドを操作する必要があるが、
                    // 前回のコードにはSetterがないため、MagicStats.javaに以下のSetterを追加してください：
                    // public void setCalculationArea(int val) { this.calculationArea = val; }

                    // ここではとりあえずSetterがある前提、もしくはcopyFromのようなメソッドで更新します
                    // 今回は個別にセットする形を想定
                    stats.setMentalLoad(this.mentalLoad);

                    // MagicStats側に同期専用のメソッドを作るとスマートです（後述）
                    stats.syncClient(this.currentPsion, this.maxPsion, this.calculationArea, this.mentalLoad);
                });
            }
        });
        return true;
    }
}
