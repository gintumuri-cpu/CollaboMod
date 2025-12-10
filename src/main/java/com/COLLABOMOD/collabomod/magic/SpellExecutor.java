package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.world.idea.EidosData;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import com.COLLABOMOD.collabomod.physics.PhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;

public class SpellExecutor {

    public static void execute(SpellContext ctx) {
        if (ctx.level.isClientSide) return;

        // ■ 1. 射撃タイプ (PROJECTILE)
        // 射撃は「単発の弾丸エンティティ」なので、ここは個別のエンティティを使います
        if (ctx.action == SpellContext.EnumAction.PROJECTILE) {

            float f = 0.017453292F;
            double x = -Math.sin(ctx.rotY * f) * Math.cos(ctx.rotX * f);
            double y = -Math.sin(ctx.rotX * f);
            double z = Math.cos(ctx.rotY * f) * Math.cos(ctx.rotX * f);

            // 分解成分が高い -> グラム
            if (ctx.science.compMatter > 0.5F) {
                EntityGramDemolition bullet = new EntityGramDemolition(ctx.level, ctx.caster);
                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
                bullet.shoot(x, y, z, ctx.speed * 4.0F, 0.5F);
                bullet.setDamageMultiplier(ctx.power);
                ctx.level.addFreshEntity(bullet);
                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            // それ以外 -> エア・バレット
            else {
                EntityAirBullet bullet = new EntityAirBullet(ctx.level, ctx.caster);
                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
                bullet.shoot(x, y, z, ctx.speed * 3.0F, 0.5F);
                ctx.level.addFreshEntity(bullet);
                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
            }
        }

        // ■ 2. 爆発タイプ (EXPLOSION)
        else if (ctx.action == SpellContext.EnumAction.EXPLOSION) {
            // ■ 修正: 全ての爆発現象を EntityMaterialBurst (万能現象エンティティ) に任せる
            // これにより、ScienceEngineで計算された「色」や「形状」が反映され、物理エンジンも継続的に動作します
            EntitySciencePhenomenon burst = new EntitySciencePhenomenon(ctx.level, ctx.origin, ctx.science, ctx.caster);
            ctx.level.addFreshEntity(burst);

            // 初動の音
            ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 1.0F);
        }

        // ■ 3. 回復タイプ (RESTORE)
        else if (ctx.action == SpellContext.EnumAction.RESTORE) {
            executeRestore(ctx);
        }

        // ■ 4. 防御タイプ (DEFEND)
        else if (ctx.action == SpellContext.EnumAction.DEFEND) {
            // ターゲット指定があればそちらを中心に展開
            Vec3 pos = (ctx.target != null) ? ctx.target.position() : ctx.origin;

            // ■ 修正: 防御結界も EntityMaterialBurst で表現
            // compShield成分が含まれているため、ScienceEngineが自動的に「シールドドーム」として描画します
            EntitySciencePhenomenon shield = new EntitySciencePhenomenon(ctx.level, pos, ctx.science, ctx.caster);
            ctx.level.addFreshEntity(shield);

            ctx.level.playSound(null, pos.x, pos.y, pos.z,
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
        }

        // ■ 5. 移動タイプ (MOVE)
        else if (ctx.action == SpellContext.EnumAction.MOVE) {
            // 移動（加速や加重）は一瞬のベクトル操作なので、即座に物理エンジンを呼ぶ
            // もし「持続的な重力場」を作りたい場合は EntityMaterialBurst を使いますが、
            // 「アクセラレーション（自分を飛ばす）」などは即時実行が適しています
            PhysicsSystem.applyPhysics(ctx.level, ctx.origin, ctx.science, ctx.caster, ctx.target); // ターゲット判定などはPhysicsSystem内で行う

            ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z,
                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0F, 1.5F);
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

        ctx.level.playSound(null, ctx.target.getX(), ctx.target.getY(), ctx.target.getZ(),
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.5F);

        if (ctx.caster instanceof Player player) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                int pain = (int)(damageDiff * 2);
                if (pain < 10) pain = 10;
                stats.addMentalLoad(pain);
                int duration = pain * 10;
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 4));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
                player.sendMessage(new TextComponent("§b再成完了"), net.minecraft.Util.NIL_UUID);
            });
        }
    }
}
