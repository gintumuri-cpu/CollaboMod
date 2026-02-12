package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.*;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import com.COLLABOMOD.collabomod.util.PsionParticleUtil;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ItemCAD extends Item {

    private final Random random = new Random();

    public ItemCAD() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            castMagic(level, player, stack);
            // 魔法使用時のクールダウンなどを設定する場合はここに記述
            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResultHolder.success(stack);
    }

    private void castMagic(Level level, Player player, ItemStack stack) {
        // ■ 追加: サイオン消費ロジック
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            CompoundTag tag = stack.getOrCreateTag();
            List<String> script = new ArrayList<>();

            if (tag.contains("Script")) {
                ListTag list = tag.getList("Script", Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    script.add(list.getString(i));
                }
            } else {
                // デフォルトのスクリプト (テスト用)
                script.add("explode(10)");
                script.add("fire");
            }

            // ■ 新フロー: SpellResolver を使ってスクリプトを完全解決する
            SpellContext ctx = SpellResolver.resolve(script);

            // ■ 追加: コスト計算 (簡易)
            int cost = (int) (ctx.physics.energy + ctx.physics.mass * 5 + ctx.physics.areaOfEffect * 2);
            if (cost < 5) cost = 5; // 最低コスト

            if (stats.getCurrentPsion() >= cost) {
                // サイオンを消費して魔法を発動
                stats.setCurrentPsion(stats.getCurrentPsion() - cost);

                // 実行に必要な情報を追加
                ctx.level = level; ctx.caster = player; ctx.origin = player.getEyePosition();

                // ターゲット取得 (オプション: 視線の先のエンティティやブロックをターゲットにする場合)
                HitResult result = getRayTraceResult(level, player, 20.0);
                if (result.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityResult = (EntityHitResult) result;
                    if (entityResult.getEntity() instanceof LivingEntity) {
                        ctx.target = (LivingEntity) entityResult.getEntity();
                    }
                }

                // 実行
                SpellExecutor.execute(ctx);

                // フィードバック (デバッグ用)
                player.sendMessage(new TextComponent("Magic Executed! (Cost: " + cost + ")"), Util.NIL_UUID);

            } else {
                // サイオン不足
                player.sendMessage(new TextComponent("§c想子(サイオン)不足"), Util.NIL_UUID);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.5f);
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        if (stack.hasTag() && stack.getTag().contains("Script")) {
            tooltip.add(new TextComponent("§aScript Loaded"));
            ListTag list = stack.getTag().getList("Script", Tag.TAG_STRING);
            for(int i=0; i<Math.min(list.size(), 3); i++) {
                tooltip.add(new TextComponent("§7" + list.getString(i)));
            }
            if(list.size() > 3) tooltip.add(new TextComponent("§7..."));
        } else {
            tooltip.add(new TextComponent("§cNo Script"));
        }
    }

    // レイキャスト用ヘルパー
    private HitResult getRayTraceResult(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        // まずエンティティ判定
        AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D);
        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable()
        );

        if (entityResult != null) {
            return entityResult;
        }

        // なければブロック判定
        return player.pick(range, 0.0F, false);
    }

    // アニメーション (手に持った時の動き)
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }
}