package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class ItemCAD extends Item{
    public ItemCAD() {
        // タブ設定、スタック数1（武器なので）
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    // 2. 最大使用時間（右クリックを押し続けられる時間）
    // 72000tick = 1時間。実質無限に構えていられる設定
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 3. 右クリック「開始時」の処理（起動式の展開）
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 使用状態（構え）を開始する必須メソッド
        player.startUsingItem(hand);

        // 起動音（システム起動のような音）
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResultHolder.consume(itemstack);
    }

    // 4. 右クリック「継続中」の処理（ループ・キャスト本体）
    // 毎tick（1/20秒ごと）に呼ばれ続けます
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        // サーバー側かつ、使っているのがプレイヤーである場合のみ実行
        if (!level.isClientSide && livingEntity instanceof Player player) {

            // 押し始めからの経過tick数を計算
            // getUseDuration(72000) から count(減っていく数値) を引く
            int duration = this.getUseDuration(stack) - count;

            // --- 連射速度の設定 ---
            // 「5tickに1回」発射する（0.25秒間隔）
            // 数値を小さくすると連射が速くなり、大きくすると遅くなる
            if (duration % 5 == 0) {
                castMagic(level, player);
            }
        }
    }

    // 魔法発射のロジックを分離
    private void castMagic(Level level, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {

            int cost = 20; // 連射するのでコストは少し安めに設定

            // MPチェック
            if (stats.getCurrentPsion() >= cost) {
                // 消費
                stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                // --- 弾の発射処理 ---

                // ★将来的にここを「EntityGramDemolition」などの自作弾丸に差し替えます
                // 今は仮で「光の矢」を発射
                EntityGramDemolition projectile = new EntityGramDemolition(level, player);

                // 向きと速度設定 (速度を 3.0F -> 4.0F に上げて、魔法っぽい高速弾にする)
                // 最後の引数(1.0F)はバラけ具合。連射するので少しバラけさせると制圧射撃っぽくなる
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 1.0F);

                // ワールドに追加
                level.addFreshEntity(projectile);

                // 音を変更：トライデントの「ズガッ」という音が重量感があって合う
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F); // ピッチを下げて重くする

            } else {
                // MP不足時
                // 連続でメッセージが出るとうるさいので、一定間隔（1秒に1回など）だけ警告音を鳴らす
                if (player.tickCount % 20 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
                    player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                }
            }
        });
    }

    // 5. 右クリックを離した時の処理（終了）
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (!level.isClientSide) {
            // 終了音（システムダウンのような音）
            level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
