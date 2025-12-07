package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.MagicComponentType; // 新Enum
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.magic.SpellResolver; // 追加
import com.COLLABOMOD.collabomod.magic.VisualMetadata; // 追加
import net.minecraft.nbt.ListTag; // 追加
import net.minecraft.nbt.StringTag; // 追加
import net.minecraft.nbt.Tag; // 追加
import java.util.ArrayList; // 追加
import com.COLLABOMOD.collabomod.util.MagicSpellType; // 旧Enum(EntityMagicSequence互換のため一時使用)
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

import java.util.List;
import java.util.Random;

public class ItemCAD extends Item {

    private final Random random = new Random();

    public ItemCAD() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    // ツールチップ
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        // メソッド名を修正 (getInstalledComponents)
        List<MagicComponentType> components = getInstalledComponents(stack);

        if (!components.isEmpty()) {
            // 先頭の魔法名を表示
            String mainName = components.get(0).name();
            // 複数ある場合は「+他」と表示
            if (components.size() > 1) {
                tooltip.add(new TextComponent("インストール: " + mainName + " (他 " + (components.size() - 1) + " 個)"));
            } else {
                tooltip.add(new TextComponent("インストール: " + mainName));
            }
        } else {
            tooltip.add(new TextComponent("インストール: なし"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ■ インストール切替 (Shift + 右クリック)
        // 仮機能：コンポーネントを切り替えてNBTに保存する
        if (player.isCrouching()) {
            if (!level.isClientSide) {
                cycleComponent(stack, player);
            }
            return InteractionResultHolder.success(stack);
        }

        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
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

            // ■ NBTからコンポーネントリストを取得
            List<MagicComponentType> components = getInstalledComponents(stack);
            if (components.isEmpty()) return;

            // リゾルバーを使ってコストなどを計算
            int cost = SpellResolver.calculateTotalCost(components);
            VisualMetadata visuals = SpellResolver.resolveVisuals(components);
            int castTime = SpellResolver.calculateCastTime(components);

            if (!level.isClientSide) {
                int stress = cadStats.getMentalLoad();
                if (stress > 70 && level.getRandom().nextInt(100) < (stress - 70) * 2) {
                    handleFizzle(level, player);
                    return;
                }

                if (cadStats.getCurrentPsion() >= cost) {
                    cadStats.setCurrentPsion(cadStats.getCurrentPsion() - cost);
                    cadStats.addMentalLoad(2);

                    // --- エア・バレット的な魔法陣展開 ---
                    Vec3 targetPos = getTargetPosition(level, player, 30.0D);
                    Vec3 spawnPos = getRandomSpawnPos(targetPos);

                    // 魔法式エンティティの生成（リストを渡す）
                    EntityMagicSequence sequence = new EntityMagicSequence(
                            level, player, components, visuals, castTime, spawnPos
                    );

                    lookAt(sequence, targetPos);
                    level.addFreshEntity(sequence);

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 1.0F, 2.0F);

                } else {
                    if (player.tickCount % 20 == 0) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
                        player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                    }
                }
            }

            if (level.isClientSide) {
                if (cadStats.getCurrentPsion() >= cost) {
                    Vec3 look = player.getLookAngle();
                    Vec3 muzzlePos = player.getEyePosition().add(look.scale(0.8));
                    PsionParticleUtil.spawnPsionRing(level, muzzlePos, look, 0.2F, 10);
                }
            }
        });
    }

    // --- Helper Methods ---

    // NBTからコンポーネントを読み込む（なければエアバレット）
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
        } else {
            // デフォルト: エア・バレット単体
            list.add(MagicComponentType.PROJECTILE_AIR);
        }
        return list;
    }

    // コンポーネントの切り替え（インストール）
    private void cycleComponent(ItemStack stack, Player player) {
        List<MagicComponentType> current = getInstalledComponents(stack);
        ListTag newList = new ListTag();
        String msg = "";

        // ロジック: エア・バレット単体なら -> 複合（エア＋グラム）にする
        // それ以外なら -> エア・バレット単体に戻す
        if (current.size() == 1 && current.get(0) == MagicComponentType.PROJECTILE_AIR) {
            newList.add(StringTag.valueOf(MagicComponentType.PROJECTILE_AIR.name()));
            newList.add(StringTag.valueOf(MagicComponentType.PROJECTILE_GRAM.name()));
            msg = "§5[複合] エア・バレット + グラム";
        } else {
            newList.add(StringTag.valueOf(MagicComponentType.PROJECTILE_AIR.name()));
            msg = "§b[単体] エア・バレット";
        }
//        if (current.size() == 1 && current.get(0) == MagicComponentType.PROJECTILE_AIR) {
//            // ■ 実験: エア・バレット と マテリアル・バースト を合成
//            newList.add(StringTag.valueOf(MagicComponentType.PROJECTILE_AIR.name()));
//            newList.add(StringTag.valueOf(MagicComponentType.MATERIAL_BURST.name()));
//            msg = "§5[実験] エア・バレット + 戦略級";
//        } else {
//            newList.add(StringTag.valueOf(MagicComponentType.PROJECTILE_AIR.name()));
//            msg = "§b[単体] エア・バレット";
//        }

        stack.getOrCreateTag().put("Components", newList);

        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.COMPARATOR_CLICK, SoundSource.PLAYERS, 1.0F, 1.5F);
        player.displayClientMessage(new TextComponent("インストール: " + msg), true);
    }

    private Vec3 getTargetPosition(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable());
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