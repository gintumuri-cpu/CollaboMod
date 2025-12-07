package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.IMagicSpell;
import com.COLLABOMOD.collabomod.magic.SpellRegistry;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.util.MagicSpellType;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemCAD extends Item {

    // モード定数
    private static final int MODE_AIR_BULLET = 0;
    private static final int MODE_GRAM_DEMOLITION = 1;

    public ItemCAD() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        int mode = getMode(stack);
        String modeName = (mode == MODE_AIR_BULLET) ? "§b[爆裂 - Air Bullet]" : "§9[術式解散 - Gram Demolition]";
        tooltip.add(new TextComponent("起動術式: " + modeName));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.BOW; }
    @Override
    public int getUseDuration(ItemStack stack) { return 72000; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide) cycleMode(stack, player);
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

    // ■ 修正: ロジックを排除し、Registryに委譲するだけのメソッド
    private void castMagic(Level level, Player player, ItemStack stack) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(cadStats -> {

            // 1. モードから魔法タイプを決定
            int mode = getMode(stack);
            MagicSpellType type = (mode == MODE_AIR_BULLET) ? MagicSpellType.AIR_BULLET : MagicSpellType.GRAM_DEMOLITION;

            // 2. Registryから魔法の実体を取得
            IMagicSpell spell = SpellRegistry.getSpell(type);
            if (spell == null) return;

            int cost = spell.getCost();

            // --- サーバー側 ---
            if (!level.isClientSide) {
                int stress = cadStats.getMentalLoad();
                if (stress > 70 && level.getRandom().nextInt(100) < (stress - 70) * 2) {
                    handleFizzle(level, player);
                    return;
                }

                if (cadStats.getCurrentPsion() >= cost) {
                    cadStats.setCurrentPsion(cadStats.getCurrentPsion() - cost);
                    // 負荷加算は魔法ごとに違うなら spell.getMentalLoad() を作るべきですが、一旦固定で
                    cadStats.addMentalLoad(2);

                    // ★重要: 具体的な処理はすべて spell.initiate に丸投げ
                    spell.initiate(level, player, cadStats);

                } else {
                    if (player.tickCount % 20 == 0) {
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5F, 1.0F);
                        player.sendMessage(new TextComponent("想子不足"), Util.NIL_UUID);
                    }
                }
            }

            // --- クライアント側（共通演出） ---
            if (level.isClientSide) {
                if (cadStats.getCurrentPsion() >= cost) {
                    Vec3 look = player.getLookAngle();
                    Vec3 muzzlePos = player.getEyePosition().add(look.scale(0.8));
                    PsionParticleUtil.spawnPsionRing(level, muzzlePos, look, 0.2F, 10);
                }
            }
        });
    }

    // ヘルパーメソッド（変更なし）
    private int getMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.getInt("CADMode");
    }
    private void cycleMode(ItemStack stack, Player player) {
        CompoundTag tag = stack.getOrCreateTag();
        int currentMode = tag.getInt("CADMode");
        int newMode = (currentMode == MODE_AIR_BULLET) ? MODE_GRAM_DEMOLITION : MODE_AIR_BULLET;
        tag.putInt("CADMode", newMode);
        player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.COMPARATOR_CLICK, SoundSource.PLAYERS, 1.0F, 1.5F);
        String modeName = (newMode == MODE_AIR_BULLET) ? "§b起動術式: 爆裂 (Air Bullet)" : "§9起動術式: 術式解散 (Gram Demolition)";
        player.displayClientMessage(new TextComponent(modeName), true);
    }
    private void handleFizzle(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.hurt(net.minecraft.world.damagesource.DamageSource.MAGIC, 2.0F);
        player.sendMessage(new TextComponent("§c演算領域オーバーヒート！"), Util.NIL_UUID);
    }
}