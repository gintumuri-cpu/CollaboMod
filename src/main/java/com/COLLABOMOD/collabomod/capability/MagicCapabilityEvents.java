package com.COLLABOMOD.collabomod.capability;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketSyncMagicStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID)
public class MagicCapabilityEvents {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MagicStats.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).isPresent()) {
                event.addCapability(new ResourceLocation(CollaboMod.MOD_ID, "magic_stats"), new MagicStatsProvider());
            }
        }
    }

    // ■ 修正: ログイン時に確実に同期
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            syncData(player);
        }
    }

    // ■ 修正: リスポーン時（死亡後）に同期
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            syncData(player);
        }
    }

    // ■ 修正: ディメンション移動時に同期
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            syncData(player);
        }
    }

    // クローン（死亡時のデータ引き継ぎ）
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(oldStats -> {
                event.getPlayer().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(newStats -> {
                    newStats.copyFrom(oldStats);
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            event.player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                // 初回生成
                if (event.player.tickCount % 20 == 0) {
                    stats.generateTalent(event.player.getUUID());
                }

                // 回復処理
                if (event.player.tickCount % 20 == 0 && stats.getCurrentPsion() < stats.getMaxPsion()) {
                    stats.setCurrentPsion(stats.getCurrentPsion() + 1);
                }
                if (event.player.tickCount % 40 == 0 && stats.getMentalLoad() > 0) {
                    stats.setMentalLoad(stats.getMentalLoad() - 1);
                }

                // ■ 定期同期: 数値が変わっていなくても、念のため定期的にクライアントへ送る
                // 通信量を減らすなら「値が変わった時だけ」にするのが理想ですが、まずはバグ修正優先で定期送信します
                if (event.player.tickCount % 20 == 0) {
                    if (event.player instanceof ServerPlayer serverPlayer) {
                        syncData(serverPlayer);
                    }
                }
            });
        }
    }

    private static void syncData(ServerPlayer player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new PacketSyncMagicStats(
                            stats.getCurrentPsion(),
                            stats.getMaxPsion(),
                            stats.getCalculationArea(),
                            stats.getMentalLoad()
                    )
            );
        });
    }
}
