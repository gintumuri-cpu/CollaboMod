package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMistDispersion;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.util.PsionParticleUtil;
import com.COLLABOMOD.collabomod.world.idea.EidosData;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ItemSilverHorn extends Item {

    private final float hardwarePerformance = 2.0F;

    public ItemSilverHorn() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        // --- サーバー側の処理（発射・判定） ---
        if (!level.isClientSide) {
            // 変数名を 'serverStats' に変更して重複回避
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(serverStats -> {

                int stress = serverStats.getMentalLoad();
                if (stress > 80) {
                    if (level.getRandom().nextInt(100) < (stress - 80) * 2) {
                        handleFizzle(level, player);
                        return;
                    }
                }

                int cost = 30;

                if (serverStats.getCurrentPsion() >= cost) {
                    serverStats.setCurrentPsion(serverStats.getCurrentPsion() - cost);
                    serverStats.addMentalLoad(4);

                    EntityMistDispersion magic = new EntityMistDispersion(level, player);
                    magic.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 7.0F, 0.0F);
                    level.addFreshEntity(magic);

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.5F, 2.5F);
                } else {
                    if (player.tickCount % 20 == 0) {
                        player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                    }
                }
            });
        }

        // --- クライアント側の処理（パーティクル演出） ---
        if (level.isClientSide) {
            // 変数名を 'clientStats' に変更して重複回避
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(clientStats -> {
                if (clientStats.getCurrentPsion() >= 30) {
                    Vec3 look = player.getLookAngle();
                    Vec3 pos = player.getEyePosition().add(look.scale(1.0));

                    for (int i = 0; i < 3; i++) {
                        float dist = 0.5F * i;
                        float radius = 0.2F + (0.2F * i);

                        PsionParticleUtil.spawnPsionRing(level, pos.add(look.scale(dist)), look, radius, 30);
                    }
                }
            });
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    // --- 再成魔法（Shift + 右クリック） ---
    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level.isClientSide && player.level instanceof ServerLevel serverLevel) {
            if (player.isShiftKeyDown()) {
                castRegrowth(serverLevel, player, target);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }

    private void castRegrowth(ServerLevel level, Player player, LivingEntity target) {
        // 変数名を 'rStats' (regrowthStats) に変更して重複回避
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(rStats -> {
            IdeaDimensionData idea = IdeaDimensionData.get(level);
            EidosData backup = idea.getPreviousEntityState(target.getUUID());

            if (backup == null) {
                player.sendMessage(new TextComponent("§c修復可能なエイドスが存在しません"), Util.NIL_UUID);
                return;
            }

            int cost = 100;
            if (rStats.getCurrentPsion() >= cost) {
                rStats.setCurrentPsion(rStats.getCurrentPsion() - cost);

                float currentHP = target.getHealth();
                float oldHP = backup.getEntityData().contains("Health") ? backup.getEntityData().getFloat("Health") : target.getMaxHealth();
                float damageDiff = oldHP - currentHP;
                if (damageDiff < 0) damageDiff = 0;

                CompoundTag oldData = backup.getEntityData();
                oldData.putDouble("Pos", target.getX());

                target.load(oldData);
                target.setPos(target.getX(), target.getY(), target.getZ());

                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.5F);
                player.sendMessage(new TextComponent("§b再成完了"), Util.NIL_UUID);

                int painLoad = (int)(damageDiff * 2);
                if (painLoad < 10) painLoad = 10;
                rStats.addMentalLoad(painLoad);

                int duration = painLoad * 10;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, duration, 0));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, duration, 0));

            } else {
                player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
            }
        });
    }

    private void handleFizzle(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);
        player.sendMessage(new TextComponent("§c演算遅延..."), Util.NIL_UUID);
    }
}
