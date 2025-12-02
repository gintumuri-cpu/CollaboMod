package com.COLLABOMOD.collabomod.capability;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketSyncMagicStats;
import net.minecraft.resources.ResourceLocation;
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

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID)
public class MagicCapabilityEvents {
    // 1. キャパビリティの登録
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MagicStats.class);
    }

    // 2. プレイヤーにキャパビリティをくっつける (Attach)
    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).isPresent()) {
                event.addCapability(new ResourceLocation(CollaboMod.MOD_ID, "magic_stats"), new MagicStatsProvider());
            }
        }
    }
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            syncData(player);
        }
    }

    // 3. プレイヤー死亡時やディメンション移動時にデータを引き継ぐ (Clone)
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) { // 死亡時のみ（ディメンション移動は自動で維持される場合が多いが念の為確認）
            event.getOriginal().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(oldStats -> {
                event.getPlayer().getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(newStats -> {
                    newStats.copyFrom(oldStats);
                });
            });
        }
        if (event.getPlayer() instanceof ServerPlayer player) {
            // 少し遅延させないと同期しない場合があるため、本来は遅延処理を入れることが多いですが、まずは直接呼び出します
            syncData(player);
        }
    }

    // 4. 毎Tickごとの処理（自然回復や負荷の減少）
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // サーバー側でのみ処理する
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            event.player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                // 初回ログイン時の才能生成
                if (event.player.tickCount % 20 == 0) { // 1秒に1回チェック
                    stats.generateTalent(event.player.getUUID());
                }

                // サイオンの自然回復 (例: 1秒に1回復)
                if (event.player.tickCount % 20 == 0 && stats.getCurrentPsion() < stats.getMaxPsion()) {
                    stats.setCurrentPsion(stats.getCurrentPsion() + 1);
                }

                // 精神負荷の自然減少 (例: 2秒に1減少)
                if (event.player.tickCount % 40 == 0 && stats.getMentalLoad() > 0) {
                    stats.setMentalLoad(stats.getMentalLoad() - 1);

                    // ※デバッグ用ログ：実装確認できたら消してください
                    // System.out.println("Psion: " + stats.getCurrentPsion() + " / Load: " + stats.getMentalLoad());
                }
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
