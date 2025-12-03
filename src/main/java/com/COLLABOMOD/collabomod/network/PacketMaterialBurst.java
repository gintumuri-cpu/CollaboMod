package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Explosion;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketMaterialBurst {
    private final BlockPos targetPos;

    public PacketMaterialBurst(BlockPos pos) {
        this.targetPos = pos;
    }

    public PacketMaterialBurst(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.targetPos);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.getLevel();

            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                int cost = 50;

                if (stats.getCurrentPsion() >= cost) {
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);
                    stats.addMentalLoad(5);

                    player.sendMessage(new TextComponent("§cマテリアル・バースト: 質量エネルギー変換プロセス実行..."), Util.NIL_UUID);

                    // ■ 修正: 爆発ではなく、破壊エネルギー体を設置する
                    EntityMaterialBurst burst = new EntityMaterialBurst(level, targetPos.getX(), targetPos.getY(), targetPos.getZ());
                    level.addFreshEntity(burst);

                    // 音: 変換開始の音（雷）
                    level.playSound(null, targetPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 100.0F, 0.5F);

                } else {
                    player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}