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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemCAD extends Item implements ICAD{

    private final Random random = new Random();

    public ItemCAD() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    // ツールチップ
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        // NBTからスクリプト(文字列リスト)を読み込む
        List<String> script = getScriptFromNBT(stack);

        if (!script.isEmpty()) {
            // 最初の行を表示
            tooltip.add(new TextComponent("§b[Code] " + script.get(0)));
            if (script.size() > 1) {
                tooltip.add(new TextComponent("§7...他 " + (script.size() - 1) + " 行"));
            }
        } else {
            tooltip.add(new TextComponent("§7[未設定] 起動式が書き込まれていません"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        // 起動音
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        if (livingEntity instanceof Player player) {
            int duration = this.getUseDuration(stack) - count;
            if (duration % 4 == 0) {
                castMagic(level, player, stack);
            }
        }
    }

    private void castMagic(Level level, Player player, ItemStack stack) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(cadStats -> {

            // 1. スクリプト取得
            List<String> script = getScriptFromNBT(stack);

            // ★デバッグログ: 読み込めたか確認
            if (!level.isClientSide) {
                //System.out.println("DEBUG: Casting Magic... Script Lines: " + script.size());
                if (!script.isEmpty()) {
                    //System.out.println("DEBUG: Line 1: " + script.get(0));
                }
            }

            if (script.isEmpty()) return;

            // --- サーバー側の処理 ---
            if (!level.isClientSide) {

                // コスト計算
                SpellContext simCtx = MagicScriptEngine.simulate(script);
                int cost = simCtx.cost;

                //System.out.println("DEBUG: Calculated Cost: " + cost);

                if (cadStats.getCurrentPsion() >= cost) {
                    cadStats.setCurrentPsion(cadStats.getCurrentPsion() - cost);
                    cadStats.addMentalLoad(2);

                    SpellContext ctx = new SpellContext(level, player);

                    // ターゲット取得
                    EntityHitResult hitResult = getTargetEntityResult(level, player, 30.0D);
                    if (hitResult != null && hitResult.getEntity() instanceof LivingEntity target) {
                        ctx.target = target;
                    }

                    try {
                        // 実行
                        //System.out.println("DEBUG: Executing Script...");
                        MagicScriptEngine.execute(ctx, script);
                        //System.out.println("DEBUG: Execution Finished.");

                    } catch (MagicScriptEngine.ScriptExecutionException e) {
                        //System.out.println("DEBUG: Script Error! " + e.getMessage());
                        handleFizzle(level, player);
                        player.sendMessage(new TextComponent("§c起動式エラー [行 " + e.line + "]: " + e.getMessage()), Util.NIL_UUID);
                    }

                } else {
                    if (player.tickCount % 20 == 0) {
                        player.sendMessage(new TextComponent("想子不足 (必要: " + cost + ")"), Util.NIL_UUID);
                    }
                }
            }

            // --- クライアント側の処理 ---
            if (level.isClientSide) {
                SpellContext simCtx = MagicScriptEngine.simulate(script);
                if (cadStats.getCurrentPsion() >= simCtx.cost) {
                    Vec3 look = player.getLookAngle();
                    Vec3 muzzlePos = player.getEyePosition().add(look.scale(0.8));
                    PsionParticleUtil.spawnPsionRing(level, muzzlePos, look, 0.2F, 10);
                }
            }
        });
    }

    // --- Helper Methods ---

    // NBTから文字列リストを取得
    private List<String> getScriptFromNBT(ItemStack stack) {
        List<String> script = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.contains("ScriptCode", Tag.TAG_LIST)) {
            ListTag listTag = tag.getList("ScriptCode", Tag.TAG_STRING);
            for (int i = 0; i < listTag.size(); i++) {
                script.add(listTag.getString(i));
            }
        }
        return script;
    }

    private List<MagicComponentType> getInstalledComponents(ItemStack stack) {
        List<MagicComponentType> list = new ArrayList<>();
        CompoundTag tag = stack.getOrCreateTag();

        if (tag.contains("Components", Tag.TAG_LIST)) {
            ListTag tagList = tag.getList("Components", Tag.TAG_STRING);
            for (int i = 0; i < tagList.size(); i++) {
                try {
                    list.add(MagicComponentType.valueOf(tagList.getString(i)));
                } catch (Exception ignored) {}
            }
        }
        // ■ 修正: elseブロック（デフォルトでエアバレット追加）を削除しました
        // これで NBT がない時は size 0 のリストが返ります

        return list;
    }

    private EntityHitResult getTargetEntityResult(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable());
    }

    private Vec3 getTargetPosition(Level level, Player player, double range) {
        EntityHitResult entityResult = getTargetEntityResult(level, player, range);
        if (entityResult != null) {
            LivingEntity target = (LivingEntity) entityResult.getEntity();
            return target.position().add(0, target.getBbHeight() / 2.0, 0);
        }
        HitResult blockResult = player.pick(range, 0.0F, false);
        return blockResult.getLocation();
    }

    private Vec3 getRandomSpawnPos(Vec3 targetPos) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 3.0 + random.nextDouble() * 2.0;
        double heightOffset = (random.nextDouble() - 0.5) * 4.0;
        return targetPos.add(Math.cos(angle) * dist, heightOffset + 2.0, Math.sin(angle) * dist);
    }

    private void lookAt(EntityMagicSequence sequence, Vec3 target) {
        double dX = target.x - sequence.getX();
        double dY = target.y - sequence.getY();
        double dZ = target.z - sequence.getZ();
        double dist2d = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) (Math.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dY, dist2d) * (180 / Math.PI));
        sequence.setYRot(yaw);
        sequence.setXRot(pitch);
    }

    private void handleFizzle(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);
        player.sendMessage(new TextComponent("§c演算領域オーバーヒート！"), Util.NIL_UUID);
    }
}