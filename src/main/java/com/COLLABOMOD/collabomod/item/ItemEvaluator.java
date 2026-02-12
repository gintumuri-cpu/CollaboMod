package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.learning.CardinalLearningManager;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemEvaluator extends Item {

    public ItemEvaluator() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 現在のスコアを取得 (デフォルト5点)
            int currentScore = getScore(stack);

            // A. スニーク + 右クリック : スコア変更 (サイクル)
            if (player.isCrouching()) {
                currentScore++;
                if (currentScore > 10) currentScore = 1;
                setScore(stack, currentScore);

                // アクションバーに表示
                player.displayClientMessage(new TextComponent("§e[評価設定] " + getStarDisplay(currentScore)), true);
            }
            // B. 通常右クリック : 評価送信
            else {
                // スコアを -1.0 (1点) から 1.0 (10点) の範囲に正規化する
                // 5.5点を中間(0.0)とする線形変換
                float normalizedScore = (currentScore - 5.5f) / 4.5f;

                CardinalLearningManager.getInstance().rateLastInteraction(normalizedScore);

                String msg = (currentScore <= 2) ? "§c[AI学習] 評価: Bad (" + currentScore + "点)" :
                             (currentScore >= 9) ? "§b[AI学習] 評価: Excellent! (" + currentScore + "点)" :
                                                 "§a[AI学習] 評価: " + currentScore + "点 で記録";
                player.sendMessage(new TextComponent(msg), player.getUUID());
            }
        }
        return InteractionResultHolder.success(stack);
    }

    // NBTヘルパー
    private int getScore(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Score")) tag.putInt("Score", 5);
        return tag.getInt("Score");
    }

    private void setScore(ItemStack stack, int score) {
        stack.getOrCreateTag().putInt("Score", score);
    }

    // ★などの文字列表現
    private String getStarDisplay(int score) {
        StringBuilder sb = new StringBuilder();
        sb.append("§6"); // 金色
        for (int i = 1; i <= 10; i++) {
            if (i <= score) sb.append("★");
            else sb.append("☆");
        }
        sb.append(" §f(").append(score).append("/10)");
        return sb.toString();
    }
}