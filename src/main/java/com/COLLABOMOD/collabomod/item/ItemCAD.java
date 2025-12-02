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
    private final float hardwarePerformance = 0.8F;
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

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (!level.isClientSide && livingEntity instanceof Player player) {
            int duration = this.getUseDuration(stack) - count;
            if (duration % 5 == 0) { // 連射速度
                castMagic(level, player);
            }
        }
    }

    // 魔法発射のロジックを分離
    private void castMagic(Level level, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int cost = 20;

            // ■ 1. キャパシティオーバー（失敗）判定
            // ストレスが一定(70)を超えると確率で失敗
            int stress = stats.getMentalLoad();
            if (stress > 70) {
                // (ストレス - 70) * 2 % の確率で失敗
                // ストレス100なら 60% の確率で失敗
                if (level.random.nextInt(100) < (stress - 70) * 2) {
                    handleFizzle(level, player);
                    return; // 魔法中断
                }
            }

            if (stats.getCurrentPsion() >= cost) {
                stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                // ■ 2. ストレスの蓄積
                // 魔法を使うたびにストレスが増える
                stats.addMentalLoad(2); // 連射系なので少しずつ溜まる

                // ■ 3. 威力の計算
                // ダメージ = 基礎威力 * (演算規模 / 100) * CAD性能
                float baseDamage = 4.0F;
                float talentFactor = stats.getCalculationArea() / 100.0F;
                float finalDamage = baseDamage * talentFactor * this.hardwarePerformance;

                EntityGramDemolition projectile = new EntityGramDemolition(level, player);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 1.0F);

                // ★ 弾にダメージをセットするメソッドが必要
                // EntityGramDemolition側に setDamage(double) を追加するか、
                // Entity側のコンストラクタで計算するなどが必要ですが、
                // バニラのArrow系ではない独自Entityの場合、onHitEntity内でダメージ計算しています。
                // 簡易的に実装するため、弾に「ダメージ倍率」を持たせるのが良いです。
                projectile.setDamageMultiplier(talentFactor * this.hardwarePerformance);

                level.addFreshEntity(projectile);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);

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

    private void handleFizzle(Level level, Player player) {
        // 失敗音
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);

        // プレイヤーに反動ダメージ（キャパシティオーバー）
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);

        player.sendMessage(new net.minecraft.network.chat.TextComponent("§c演算領域オーバーヒート！"), Util.NIL_UUID);
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
