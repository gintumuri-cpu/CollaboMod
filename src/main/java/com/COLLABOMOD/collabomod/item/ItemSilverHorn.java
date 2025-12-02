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
    public ItemSilverHorn() {
        // スタック不可、耐久値なし
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    // トライデントは「長押し連射」ではなく「精密射撃（単発）」
    // 右クリックした瞬間に発動する処理に戻します
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

                int cost = 300; // 必殺技なのでコスト激重

                if (stats.getCurrentPsion() >= cost) {
                    stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                    // 分解魔法弾の発射
                    EntityMistDispersion magic = new EntityMistDispersion(level, player);
                    // 精度：ブレなし（0.0F）、速度：超高速（5.0F）
                    magic.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 5.0F, 0.0F);
                    level.addFreshEntity(magic);

                    // 発射音：鋭い音
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0F, 2.0F);

                    player.sendMessage(new TextComponent("対象を消去します"), Util.NIL_UUID);

                } else {
                    player.sendMessage(new TextComponent("想子不足：分解魔法には300が必要です"), Util.NIL_UUID);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            });
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
