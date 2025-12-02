package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemCAD extends Item{
    public ItemCAD() {
        // タブ設定、スタック数1（武器なので）
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // サーバー側でのみ処理を行う（重要なデータ変更やスポーン処理のため）
        if (!level.isClientSide) {

            // Capability（魔法ステータス）を取得
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                int cost = 50; // 消費コスト

                // MPが足りているか判定
                if (stats.getCurrentPsion() >= cost) {
                    // 1. MPを消費
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                    // 2. 魔法発動（ここでは仮に「光の矢」を発射）
                    // 本来は独自の魔法エンティティを作りますが、まずはバニラの光る矢で代用
                    SpectralArrow magicProjectile = new SpectralArrow(level, player);
                    magicProjectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                    // ダメージを少し上げる（魔法っぽく）
                    magicProjectile.setBaseDamage(8.0);

                    level.addFreshEntity(magicProjectile);

                    // 3. 音を鳴らす（エンダーマンのテレポート音が魔法っぽい）
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.5F);

                    player.sendMessage(new TextComponent("術式解凍... 発動！"), Util.NIL_UUID);

                } else {
                    // MP不足のメッセージ
                    player.sendMessage(new TextComponent("想子（サイオン）不足！"), Util.NIL_UUID);
                    // 失敗音
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            });
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
