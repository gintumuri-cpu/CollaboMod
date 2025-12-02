package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMistDispersion;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemSilverHorn extends Item{
    private final float hardwarePerformance = 1.5F;
    public ItemSilverHorn() {
        // スタック不可、耐久値なし
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }
    // シルバー・ホーンは高性能


    // トライデントは「長押し連射」ではなく「精密射撃（単発）」
    // 右クリックした瞬間に発動する処理に戻します
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                // ■ 失敗判定（必殺技なので負荷が高いとさらに失敗しやすい）
                int stress = stats.getMentalLoad();
                if (stress > 80) {
                    if (level.random.nextInt(100) < (stress - 80) * 2) {
                        handleFizzle(level, player);
                        return; // 中断
                    }
                }

                int cost = 30;
                if (stats.getCurrentPsion() >= cost) {
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                    // ■ ストレス蓄積（大技なので一気に溜まる）
                    stats.addMentalLoad(4);

                    // ■ 威力計算（分解魔法は即死ですが、貫通力などに影響させるイメージ）
                    // ここでは弾速や精度にボーナスを与えるなどにしても良い
                    float talentFactor = stats.getCalculationArea() / 100.0F;

                    EntityMistDispersion magic = new EntityMistDispersion(level, player);
                    // 性能が良いので弾速も速い
                    float speed = 5.0F * this.hardwarePerformance;
                    magic.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, speed, 0.0F);
                    level.addFreshEntity(magic);

                    // 発射音：鋭い音
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.5F, 2.5F);

                    player.sendMessage(new TextComponent("対象を消去します"), Util.NIL_UUID);

                } else {
//                    player.sendMessage(new TextComponent("想子不足：分解魔法には300が必要です"), Util.NIL_UUID);
//                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
//                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (player.tickCount % 20 == 0) { // 連打した時にうるさくないように
                        player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                    }
                }
            });
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
    private void handleFizzle(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);
        player.sendMessage(new net.minecraft.network.chat.TextComponent("§c術式解散失敗！逆流が発生！"), Util.NIL_UUID);
    }
}
