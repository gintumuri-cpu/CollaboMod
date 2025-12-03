package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketMaterialBurst {
    private final BlockPos targetPos;

    // コンストラクタ（送信側が使う）
    public PacketMaterialBurst(BlockPos pos) {
        this.targetPos = pos;
    }

    // バイト列からデータを復元（受信側が使う）
    public PacketMaterialBurst(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
    }

    // データをバイト列に変換（送信時）
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.targetPos);
    }

    // ■ パケット受信時の処理（サーバー側で実行）
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.getLevel();

            // コストと精神負荷の計算
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                int cost = 50; // 戦略級魔法のコスト

                if (stats.getCurrentPsion() >= cost) {
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                    // 負荷は少なめ（達也にとって分解は容易）
                    stats.addMentalLoad(5);

                    player.sendMessage(new TextComponent("§cマテリアル・バースト: 座標[" + targetPos.toShortString() + "]を熱量へ変換"), Util.NIL_UUID);

                    // ■ 戦略級爆発実行
                    // 半径30.0F (TNTの約7.5倍の半径、体積比で数百倍)
                    level.explode(null, targetPos.getX(), targetPos.getY(), targetPos.getZ(),
                            30.0F, true, Explosion.BlockInteraction.DESTROY);

                } else {
                    player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
