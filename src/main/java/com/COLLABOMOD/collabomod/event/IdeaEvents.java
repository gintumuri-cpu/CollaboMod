package com.COLLABOMOD.collabomod.event;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID)
public class IdeaEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntityLiving();

        if (entity.level.isClientSide) return;

        // ■ 1. エイドスの記録（再成用バックアップ）
        if (entity.level instanceof ServerLevel serverLevel) {
            IdeaDimensionData idea = IdeaDimensionData.get(serverLevel);
            // ダメージを受ける直前の状態を保存
            idea.recordEntityState(entity, serverLevel.getGameTime());
        }

        // ■ 2. 自己修復術式（プレイヤー専用オートヒール）
        if (entity instanceof Player player) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                // ダメージ量
                float damage = event.getAmount();

                // 奈落ダメージなどは修復不可能として除外（即死回避のため）
                if (event.getSource().isBypassInvul()) return;

                // コスト計算：ダメージ1につきサイオン10
                int cost = (int)(damage * 10);

                // サイオンが足りていれば発動
                if (stats.getCurrentPsion() >= cost) {

                    // ダメージ無効化（＝エイドス変更の拒絶）
                    event.setCanceled(true);

                    // コスト消費
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                    // ■ 代償：精神負荷（苦痛）
                    // 痛みは消せないので、ダメージに応じてストレスが溜まる
                    // ダメージが大きいほど強烈な負荷
                    int pain = (int)(damage * 5);
                    stats.addMentalLoad(pain);

                    // 演出：自分にだけ聞こえる音
                    // "キンッ"という弾くような音ではなく、"シュウ..."という修復音
                    player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.5F, 2.0F);

                    // デバッグ用ログ（後で消してください）
                    // player.sendMessage(new net.minecraft.network.chat.TextComponent("§b自己修復: " + damage + "dmg 無効化 (Stress+" + pain + ")"), net.minecraft.Util.NIL_UUID);
                }
            });
        }
    }
}
