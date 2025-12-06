package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.util.MagicSpellType;
import com.COLLABOMOD.collabomod.util.PsionParticleUtil;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3; // ★追加

import java.util.Random;

public class ItemCAD extends Item {
    private final Random random = new Random();
    private final float hardwarePerformance = 0.8F;

    public ItemCAD() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    // ★重要変更: !level.isClientSide の制限を外し、両方で動くようにする
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (livingEntity instanceof Player player) {
            int duration = this.getUseDuration(stack) - count;

            // 連射速度 (5tick = 0.25秒ごと)
            if (duration % 6 == 0) {
                castMagic(level, player);
            }
        }
    }

//    private void castMagic(Level level, Player player) {
//        // 変数名重複回避のため 'cadStats' に変更
//        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(cadStats -> {
//            int cost = 20;
//
//            // --- サーバー側の処理（計算・発射） ---
//            if (!level.isClientSide) {
//                int stress = cadStats.getMentalLoad();
//                if (stress > 70) {
//                    if (level.getRandom().nextInt(100) < (stress - 70) * 2) {
//                        handleFizzle(level, player);
//                        return;
//                    }
//                }
//
//                if (cadStats.getCurrentPsion() >= cost) {
//                    cadStats.setCurrentPsion(cadStats.getCurrentPsion() - cost);
//                    cadStats.addMentalLoad(2);
//
//                    float baseDamage = 4.0F;
//                    float talentFactor = cadStats.getCalculationArea() / 100.0F;
//
//                    EntityGramDemolition projectile = new EntityGramDemolition(level, player);
//                    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 1.0F);
//                    projectile.setDamageMultiplier(talentFactor * this.hardwarePerformance);
//
//                    level.addFreshEntity(projectile);
//                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
//                            SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
//                } else {
//                    if (player.tickCount % 20 == 0) {
//                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
//                                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
//                        player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
//                    }
//                }
//            }
//
//            // --- クライアント側の処理（パーティクル演出） ---
//            if (level.isClientSide) {
//                // クライアント側でもMPチェック（演出を同期させるため）
//                if (cadStats.getCurrentPsion() >= cost) {
//                    Vec3 look = player.getLookAngle();
//                    // 銃口の位置（少し前）
//                    Vec3 muzzlePos = player.getEyePosition().add(look.scale(0.8));
//
//                    // リング描画（半径0.3、密度20）
//                    PsionParticleUtil.spawnPsionRing(level, muzzlePos, look, 0.3F, 20);
//                    // 2つ目のリング（少し離して、半径0.5）
//                    PsionParticleUtil.spawnPsionRing(level, muzzlePos.add(look.scale(0.5)), look, 0.5F, 20);
//                }
//            }
//        });
//    }
private void castMagic(Level level, Player player) {
    player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(cadStats -> {
        int cost = 20;

        if (!level.isClientSide) {
            int stress = cadStats.getMentalLoad();
            if (stress > 70) {
                if (level.getRandom().nextInt(100) < (stress - 70) * 2) {
                    handleFizzle(level, player);
                    return;
                }
            }

            if (cadStats.getCurrentPsion() >= cost) {
                cadStats.setCurrentPsion(cadStats.getCurrentPsion() - cost);
                cadStats.addMentalLoad(2);

                // ■■■ 修正: ターゲット捕捉とランダム配置 ■■■

                // 1. 視線の先のエンティティを探す (距離30m)
                double range = 30.0D;
                Vec3 eyePos = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                Vec3 endPos = eyePos.add(look.scale(range));
                AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);

                EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                        level, player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable());

                Vec3 targetPos;
                LivingEntity targetEntity = null;

                if (entityResult != null) {
                    // 敵が見つかった場合 -> その敵を中心に
                    targetEntity = (LivingEntity) entityResult.getEntity();
                    targetPos = targetEntity.position().add(0, targetEntity.getBbHeight() / 2.0, 0); // 体の中心
                } else {
                    // 敵がいない場合 -> 視線の先の空中に
                    HitResult blockResult = player.pick(range, 0.0F, false);
                    targetPos = blockResult.getLocation();
                }

                // 2. 魔法陣の出現位置をランダムに決定
                // ターゲットから半径 3～5 ブロック離れた位置
                double angle = random.nextDouble() * Math.PI * 2; // 0~360度
                double dist = 3.0 + random.nextDouble() * 2.0;  // 距離
                double heightOffset = (random.nextDouble() - 0.5) * 4.0; // 高さのブレ

                double offsetX = Math.cos(angle) * dist;
                double offsetZ = Math.sin(angle) * dist;

                Vec3 spawnPos = targetPos.add(offsetX, heightOffset + 2.0, offsetZ); // 少し上から撃ち下ろす感じに

                // 3. 魔法式生成
                EntityMagicSequence sequence = new EntityMagicSequence(
                        level, player, MagicSpellType.AIR_BULLET, spawnPos
                );

                // ■ 重要: 魔法陣をターゲットの方に向ける
                // (EntityMagicSequence側で計算させるため、ターゲットIDを渡すか、ここで計算してRotationセットする)
                // ここでは簡易的に計算してセットします
                double dX = targetPos.x - spawnPos.x;
                double dY = targetPos.y - spawnPos.y;
                double dZ = targetPos.z - spawnPos.z;
                double dist2d = Math.sqrt(dX * dX + dZ * dZ);

                float yaw = (float) (Math.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
                float pitch = (float) -(Math.atan2(dY, dist2d) * (180 / Math.PI));

                sequence.setYRot(yaw);
                sequence.setXRot(pitch);

                // ターゲット情報を渡しておくと、追尾などに使える（今回は座標固定射撃なので不要だが念のため）
                if (targetEntity != null) {
                    // 必要なら sequence.setTarget(targetEntity); などを実装
                }

                level.addFreshEntity(sequence);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 1.0F, 2.0F);

            } else {
                if (player.tickCount % 20 == 0) {
                    player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                }
            }
        }

        // 手元の演出
        if (level.isClientSide) {
            if (cadStats.getCurrentPsion() >= cost) {
                Vec3 look = player.getLookAngle();
                Vec3 muzzlePos = player.getEyePosition().add(look.scale(0.8));
                PsionParticleUtil.spawnPsionRing(level, muzzlePos, look, 0.2F, 10);
            }
        }
    });
}

    private void handleFizzle(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);
        player.sendMessage(new TextComponent("§c演算領域オーバーヒート！"), Util.NIL_UUID);
    }
}
