package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import com.COLLABOMOD.collabomod.world.idea.EidosData;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SpellExecutor {

    public static void execute(SpellContext ctx) {
        if (ctx.level.isClientSide) return;

        // ■ 1. 射撃タイプ (PROJECTILE)
        if (ctx.action == SpellContext.EnumAction.PROJECTILE) {

            // 発射ベクトル計算
            float f = 0.017453292F;
            double x = -Math.sin(ctx.rotY * f) * Math.cos(ctx.rotX * f);
            double y = -Math.sin(ctx.rotX * f);
            double z = Math.cos(ctx.rotY * f) * Math.cos(ctx.rotX * f);

            // 属性による分岐
            if (ctx.attribute == SpellContext.EnumAttribute.AIR) {
                // エア・バレット
                EntityAirBullet bullet = new EntityAirBullet(ctx.level, ctx.caster);
                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
                bullet.shoot(x, y, z, ctx.speed * 3.0F, 0.5F);
                ctx.level.addFreshEntity(bullet);
                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
            }
            else if (ctx.attribute == SpellContext.EnumAttribute.DECOMPOSITION) {
                // グラム・デモリッション (ミスト弾)
                EntityGramDemolition bullet = new EntityGramDemolition(ctx.level, ctx.caster);
                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
                bullet.shoot(x, y, z, ctx.speed * 4.0F, 0.5F);
                bullet.setDamageMultiplier(ctx.power);
                ctx.level.addFreshEntity(bullet);
                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            // 将来的に: else if (VIBRATION) -> フォノンメーザー
        }

        // ■ 2. 爆発タイプ (EXPLOSION)
        else if (ctx.action == SpellContext.EnumAction.EXPLOSION) {

            if (ctx.attribute == SpellContext.EnumAttribute.MASS_ENERGY) {
                // マテリアル・バースト
                EntityMaterialBurst burst = new EntityMaterialBurst(ctx.level, ctx.origin.x, ctx.origin.y, ctx.origin.z);
                // コンテキストの範囲を反映させたい場合、Entity側に setter を作ってここで呼び出す
                // burst.setMaxRadius(ctx.range);
                ctx.level.addFreshEntity(burst);
                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 100.0F, 0.5F);
            }
            // ここに「振動爆発」などを追加可能
        }

        // ■ 3. 回復タイプ (RESTORE)
        else if (ctx.action == SpellContext.EnumAction.RESTORE) {
            executeRestore(ctx);
        }
    }

    private static void executeRestore(SpellContext ctx) {
        if (ctx.target == null || !(ctx.level instanceof ServerLevel serverLevel)) return;
        IdeaDimensionData idea = IdeaDimensionData.get(serverLevel);
        EidosData backup = idea.getOptimalEntityState(ctx.target.getUUID());
        if (backup == null) return;
        float currentHP = ctx.target.getHealth();
        float oldHP = backup.getEntityData().contains("Health") ? backup.getEntityData().getFloat("Health") : ctx.target.getMaxHealth();
        if (currentHP >= oldHP) return;
        float damageDiff = oldHP - currentHP;
        CompoundTag oldData = backup.getEntityData();
        ListTag posList = new ListTag();
        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getX()));
        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getY()));
        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getZ()));
        oldData.put("Pos", posList);
        ctx.target.load(oldData);
        ctx.target.setPos(ctx.target.getX(), ctx.target.getY(), ctx.target.getZ());
        ctx.target.invulnerableTime = 20;
        idea.clearHistory(ctx.target.getUUID());
        ctx.level.playSound(null, ctx.target.getX(), ctx.target.getY(), ctx.target.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.5F);
        if (ctx.caster instanceof Player player) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                int pain = (int)(damageDiff * 2);
                if (pain < 10) pain = 10;
                stats.addMentalLoad(pain);
                int duration = pain * 10;
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
                player.sendMessage(new TextComponent("§b再成完了 (復元量: " + (int)damageDiff + ")"), net.minecraft.Util.NIL_UUID);
            });
        }
    }
}
