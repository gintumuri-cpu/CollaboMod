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
                // スコアを 0.0 ~ 1.0 に正規化して渡す (1点=0.1, 10点=1.0)
                // ※ 1点などの低評価は「0.1」として記録され、AIへの影響力が極めて小さくなる
                // もし「二度と出すな」という強い否定をしたい場合は、1点のときだけ -1.0 を送るロジックにしても良い

                float normalizedScore;
                if (currentScore == 1) {
                    normalizedScore = -1.0f; // 1点は「完全否定（禁止）」として扱う
                } else {
                    normalizedScore = currentScore / 10.0f; // 2~10点は「重み」として扱う
                }

                CardinalLearningManager.getInstance().rateLastInteraction(normalizedScore);

                String msg = (normalizedScore < 0) ? "§c[AI学習] 評価: Bad (除外対象)" : "§a[AI学習] 評価: " + currentScore + "点 で記録";
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