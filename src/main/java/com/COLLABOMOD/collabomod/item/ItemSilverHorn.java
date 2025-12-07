package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMistDispersion;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.util.PsionParticleUtil;
import com.COLLABOMOD.collabomod.world.idea.EidosData;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemSilverHorn extends Item implements ICAD{

    private final float hardwarePerformance = 2.0F;

    // 定数: モードID
    private static final int MODE_DECOMPOSITION = 0;
    private static final int MODE_REGROWTH = 1;

    public ItemSilverHorn() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    // ■ ツールチップ（マウスホバー時にモードを表示）
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = getMode(stack);
        String modeName = (mode == MODE_DECOMPOSITION) ? "§c[分解 - Mist Dispersion]" : "§a[再成 - Regrowth]";
        tooltip.add(new TextComponent("起動術式: " + modeName));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    // ■ 右クリック（空撃ち or モード切替）
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // --- モード切替処理 (Shift + 右クリック) ---
        if (player.isCrouching()) {
            if (!level.isClientSide) {
                cycleMode(stack, player);
            }
            return InteractionResultHolder.success(stack);
        }

        // --- 魔法発動処理 ---
        int mode = getMode(stack);

        // 1. 分解モードの場合 -> 発射
        if (mode == MODE_DECOMPOSITION) {
            executeDecomposition(level, player);
            return InteractionResultHolder.success(stack);
        }

        // 2. 再成モードの場合 -> 空撃ち不可（対象が必要）
        else {
            if (!level.isClientSide) {
                player.sendMessage(new TextComponent("§e再成モード: 対象に向けて使用してください"), Util.NIL_UUID);
            }
            return InteractionResultHolder.fail(stack);
        }
    }

    // ■ エンティティへの右クリック（再成の発動）
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        int mode = getMode(stack);

        // 再成モードの時だけ、エンティティに対する処理を行う
        if (mode == MODE_REGROWTH) {
            if (!player.level.isClientSide && player.level instanceof ServerLevel serverLevel) {
                castRegrowth(serverLevel, player, target);
            }
            return InteractionResult.SUCCESS;
        }

        // 分解モードなら何もしない（useメソッドに処理を流して撃つ）
        return InteractionResult.PASS;
    }

    // --- ヘルパーメソッド: NBT操作 ---

    private int getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("CADMode");
    }

    private void cycleMode(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentMode = tag.getInt("CADMode");
        int newMode = (currentMode == MODE_DECOMPOSITION) ? MODE_REGROWTH : MODE_DECOMPOSITION;
        tag.putInt("CADMode", newMode);

        // 切替音とメッセージ
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.COMPARATOR_CLICK, SoundSource.PLAYERS, 1.0F, 1.5F);

        String modeName = (newMode == MODE_DECOMPOSITION) ? "§c起動術式: 分解 (Mist Dispersion)" : "§a起動術式: 再成 (Regrowth)";
        player.displayClientMessage(new TextComponent(modeName), true); // アクションバーに表示
    }

    // --- 魔法ロジック (分解) ---
    private void executeDecomposition(Level level, Player player) {
        // サーバー側
        if (!level.isClientSide) {
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

        // クライアント側（パーティクル）
        if (level.isClientSide) {
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
    }

    // --- 魔法ロジック (再成) ---
    private void castRegrowth(ServerLevel level, Player player, LivingEntity target) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(rStats -> {
            IdeaDimensionData idea = IdeaDimensionData.get(level);
            EidosData backup = idea.getOptimalEntityState(target.getUUID());

            if (backup == null) {
                player.sendMessage(new TextComponent("§c修復可能なエイドスが存在しません"), Util.NIL_UUID);
                return;
            }

            int cost = 100;
            if (rStats.getCurrentPsion() >= cost) {
                rStats.setCurrentPsion(rStats.getCurrentPsion() - cost);

                float currentHP = target.getHealth();
                // NBTから取得したHP（過去の最大の状態）
                float oldHP = backup.getEntityData().contains("Health") ? backup.getEntityData().getFloat("Health") : target.getMaxHealth();

                // ■ ここで比較: もし現在のHPが、バックアップのHPと同じかそれ以上なら「回復の必要なし」
                if (currentHP >= oldHP) {
                    player.sendMessage(new TextComponent("§e対象のエイドスは書き換え出来ません"), Util.NIL_UUID);
                    // コストを返還してもいいですが、発動した時点で消費するのが通例
                    return;
                }

                float damageDiff = oldHP - currentHP;

                CompoundTag oldData = backup.getEntityData();
                net.minecraft.nbt.ListTag posList = new net.minecraft.nbt.ListTag();
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(target.getX()));
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(target.getY()));
                posList.add(net.minecraft.nbt.DoubleTag.valueOf(target.getZ()));
                oldData.put("Pos", posList);

                target.load(oldData);
                // load直後は位置情報が不安定な場合があるため再セット
                target.setPos(target.getX(), target.getY(), target.getZ());

                // ■ 修正: 再成直後の攻撃判定を防ぐため、1秒間（20tick）の無敵時間を付与
                target.invulnerableTime = 20;

                // ■ 修正: イデアから履歴を確実に消去し、連続使用を不可にする
                idea.clearHistory(target.getUUID());

                level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.5F);
                player.sendMessage(new TextComponent("§b再成完了"), Util.NIL_UUID);

                idea.clearHistory(target.getUUID());

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