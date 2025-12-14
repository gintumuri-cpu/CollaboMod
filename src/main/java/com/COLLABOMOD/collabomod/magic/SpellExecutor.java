package com.COLLABOMOD.collabomod.magic;

//import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
//import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
//import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
//import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
//import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
//import com.COLLABOMOD.collabomod.physics.PhysicsSystem;
//import com.COLLABOMOD.collabomod.world.cardinal.CardinalSpaceManager; // 追加
//import com.COLLABOMOD.collabomod.world.idea.EidosData;
//import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.ListTag;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.phys.Vec3;
//
//public class SpellExecutor {
//
//    public static void execute(SpellContext ctx) {
//        if (ctx.level.isClientSide) return;
//
//        // ■■■ 魔法陣展開ロジック (初回実行時) ■■■
//        if (ctx.castTime > 0 && !ctx.fromSequence) {
//
//            // ■ 修正: CardinalSpaceManager に座標計算を委譲
//            // これにより「前方空間への分散配置」が適用されます
//            Vec3 spawnPos = CardinalSpaceManager.findOptimalPosition(ctx.caster, ctx.target, ctx.science.visuals);
//
//            // 魔法陣生成
//            EntityMagicSequence sequence = new EntityMagicSequence(
//                    ctx.level,
//                    ctx.caster,
//                    ctx.script,
//                    ctx.science.visuals,
//                    ctx.castTime,
//                    spawnPos
//            );
//
//            // 向きの自動設定
//            EnumMagicAnchor anchor = ctx.science.visuals.anchorType;
//
//            if (anchor == EnumMagicAnchor.TARGET_ANCHORED) {
//                sequence.setXRot(-90.0F);
//                sequence.setYRot(0.0F);
//            }
//            else if (ctx.target != null) {
//                // ターゲットの方向を向く (Look At)
//                double dX = ctx.target.getX() - spawnPos.x;
//                double dY = ctx.target.getEyePosition().y - spawnPos.y;
//                double dZ = ctx.target.getZ() - spawnPos.z;
//                double dist = Math.sqrt(dX * dX + dZ * dZ);
//
//                float yaw = (float)(Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
//                float pitch = (float)-(Math.atan2(dY, dist) * (180D / Math.PI));
//
//                sequence.setYRot(yaw);
//                sequence.setXRot(pitch);
//            }
//            else {
//                // ターゲットがいなければ、術者の体の向きに合わせる
//                sequence.setYRot(ctx.caster.getYRot());
//                sequence.setXRot(ctx.caster.getXRot());
//            }
//
//            ctx.level.addFreshEntity(sequence);
//            ctx.level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
//                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
//
//            return;
//        }
//
//        // ■■■ 即時発動ロジック (変更なし) ■■■
//
//        // 1. 射撃タイプ
//        if (ctx.action == SpellContext.EnumAction.PROJECTILE) {
//            float f = 0.017453292F;
//            double x = -Math.sin(ctx.rotY * f) * Math.cos(ctx.rotX * f);
//            double y = -Math.sin(ctx.rotX * f);
//            double z = Math.cos(ctx.rotY * f) * Math.cos(ctx.rotX * f);
//
//            if (ctx.target != null) {
//                Vec3 toTarget = ctx.target.getEyePosition().subtract(ctx.origin).normalize();
//                x = toTarget.x; y = toTarget.y; z = toTarget.z;
//            }
//
//            if (ctx.science.compMatter > 0.5F) {
//                EntityGramDemolition bullet = new EntityGramDemolition(ctx.level, ctx.caster);
//                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
//                bullet.shoot(x, y, z, ctx.speed * 4.0F, 0.5F);
//                bullet.setDamageMultiplier(ctx.power);
//                ctx.level.addFreshEntity(bullet);
//                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
//            } else {
//                EntityAirBullet bullet = new EntityAirBullet(ctx.level, ctx.caster);
//                bullet.setPos(ctx.origin.x, ctx.origin.y, ctx.origin.z);
//                bullet.shoot(x, y, z, ctx.speed * 3.0F, 0.5F);
//                ctx.level.addFreshEntity(bullet);
//                ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
//            }
//        }
//
//        // 2. 爆発タイプ
//        else if (ctx.action == SpellContext.EnumAction.EXPLOSION) {
//            EntitySciencePhenomenon burst = new EntitySciencePhenomenon(ctx.level, ctx.origin, ctx.science, ctx.caster);
//            ctx.level.addFreshEntity(burst);
//            ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z,
//                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 1.0F);
//        }
//
//        // 3. 回復タイプ
//        else if (ctx.action == SpellContext.EnumAction.RESTORE) {
//            executeRestore(ctx);
//        }
//
//        // 4. 防御タイプ
//        else if (ctx.action == SpellContext.EnumAction.DEFEND) {
//            Vec3 pos = (ctx.target != null) ? ctx.target.position() : ctx.origin;
//            EntitySciencePhenomenon shield = new EntitySciencePhenomenon(ctx.level, pos, ctx.science, ctx.caster);
//            ctx.level.addFreshEntity(shield);
//            ctx.level.playSound(null, pos.x, pos.y, pos.z,
//                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
//        }
//
//        // 5. 移動タイプ
//        else if (ctx.action == SpellContext.EnumAction.MOVE) {
//            PhysicsSystem.applyPhysics(ctx.level, ctx.origin, ctx.science, ctx.caster, ctx.target);
//            ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z,
//                    SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0F, 1.5F);
//        }
//    }
//
//    private static void executeRestore(SpellContext ctx) {
//        if (ctx.target == null || !(ctx.level instanceof ServerLevel serverLevel)) return;
//        IdeaDimensionData idea = IdeaDimensionData.get(serverLevel);
//        EidosData backup = idea.getOptimalEntityState(ctx.target.getUUID());
//        if (backup == null) return;
//        float currentHP = ctx.target.getHealth();
//        float oldHP = backup.getEntityData().contains("Health") ? backup.getEntityData().getFloat("Health") : ctx.target.getMaxHealth();
//        if (currentHP >= oldHP) return;
//        CompoundTag oldData = backup.getEntityData();
//        ListTag posList = new ListTag();
//        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getX()));
//        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getY()));
//        posList.add(net.minecraft.nbt.DoubleTag.valueOf(ctx.target.getZ()));
//        oldData.put("Pos", posList);
//        ctx.target.load(oldData);
//        ctx.target.setPos(ctx.target.getX(), ctx.target.getY(), ctx.target.getZ());
//        ctx.target.invulnerableTime = 20;
//        idea.clearHistory(ctx.target.getUUID());
//        ctx.level.playSound(null, ctx.target.getX(), ctx.target.getY(), ctx.target.getZ(),
//                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.5F);
//    }
//}


//教師データ作成用↓
import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.learning.AnalysisEngine; // 追加
import com.COLLABOMOD.collabomod.learning.CardinalLearningManager; // 追加
import com.COLLABOMOD.collabomod.learning.FeedbackLoop; // 追加
import com.COLLABOMOD.collabomod.learning.LearningData; // 追加
import com.COLLABOMOD.collabomod.physics.PhysicsSystem;
import com.COLLABOMOD.collabomod.world.cardinal.CardinalSpaceManager;
import com.COLLABOMOD.collabomod.world.idea.EidosData;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

        // ■ 学習用データの作成準備
        // 実際に使用された VisualMetadata は EntityMagicSequence 内などで保持されているはずだが、
        // ここでは ctx.science.visuals が最新であると仮定する。
        // もし SpellResolver で生成されたものが ctx に入っていないなら、ここで再生成する必要があるが、
        // ItemCAD.castMagic で SpellContext が作られる流れであれば、そこに情報を渡すのがベスト。

        // 簡易的に、ここで現在のスクリプトを解析してデータを作る
        float[] attributes = AnalysisEngine.analyzeScript(ctx.script);
        float complexity = AnalysisEngine.calculateComplexity(ctx.script);
        int scriptHash = ctx.script.hashCode();

        // 学習データオブジェクト (まだ評価はなし)
        LearningData learningData = new LearningData(
                scriptHash,
                attributes,
                complexity,
                ctx.science.visuals // 実際に使われる描画設定
        );

        // --- 魔法発動ロジック ---

        if (ctx.castTime > 0 && !ctx.fromSequence) {
            Vec3 spawnPos = CardinalSpaceManager.findOptimalPosition(ctx.caster, ctx.target, ctx.science.visuals);
            EntityMagicSequence sequence = new EntityMagicSequence(
                    ctx.level,
                    ctx.caster,
                    ctx.script,
                    ctx.science.visuals,
                    ctx.castTime,
                    spawnPos
            );

            // ... (Anchor処理等はそのまま) ...
            EnumMagicAnchor anchor = ctx.science.visuals.anchorType;
            if (anchor == EnumMagicAnchor.TARGET_ANCHORED) {
                sequence.setXRot(-90.0F);
                sequence.setYRot(0.0F);
            } else if (ctx.target != null) {
                double dX = ctx.target.getX() - spawnPos.x;
                double dY = ctx.target.getEyePosition().y - spawnPos.y;
                double dZ = ctx.target.getZ() - spawnPos.z;
                double dist = Math.sqrt(dX * dX + dZ * dZ);
                float yaw = (float)(Math.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
                float pitch = (float)-(Math.atan2(dY, dist) * (180D / Math.PI));
                sequence.setYRot(yaw);
                sequence.setXRot(pitch);
            } else {
                sequence.setYRot(ctx.caster.getYRot());
                sequence.setXRot(ctx.caster.getXRot());
            }

            ctx.level.addFreshEntity(sequence);
            ctx.level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);

            // シーケンス生成だけなので、ここではまだ物理結果は出ないが、
            // 「発動した」という事実を学習マネージャーに送る
            CardinalLearningManager.getInstance().registerInteraction(learningData);
            return;
        }

        // --- 即時発動ロジック ---
        // (省略されている部分は既存コードを維持してください)

        // ... (Projectile, Explosion, Restore, Defend, Move の各処理) ...
        // ここで物理現象が発生する
        if (ctx.action == SpellContext.EnumAction.PROJECTILE) {
            // ...
        } else if (ctx.action == SpellContext.EnumAction.EXPLOSION) {
            EntitySciencePhenomenon burst = new EntitySciencePhenomenon(ctx.level, ctx.origin, ctx.science, ctx.caster);
            ctx.level.addFreshEntity(burst);
            ctx.level.playSound(null, ctx.origin.x, ctx.origin.y, ctx.origin.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 1.0F);
        }
        // ... (他も同様) ...

        // ■ フィードバックループの実行
        FeedbackLoop.analyzeResult(learningData, ctx.science);

        // ■ 学習マネージャーに「評価待ち」として登録
        CardinalLearningManager.getInstance().registerInteraction(learningData);
    }

    private static void executeRestore(SpellContext ctx) {
        // (変更なし)
        if (ctx.target == null || !(ctx.level instanceof ServerLevel serverLevel)) return;
        IdeaDimensionData idea = IdeaDimensionData.get(serverLevel);
        EidosData backup = idea.getOptimalEntityState(ctx.target.getUUID());
        if (backup == null) return;
        float currentHP = ctx.target.getHealth();
        float oldHP = backup.getEntityData().contains("Health") ? backup.getEntityData().getFloat("Health") : ctx.target.getMaxHealth();
        if (currentHP >= oldHP) return;
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
    }
}