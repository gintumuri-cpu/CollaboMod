package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.learning.CardinalLearningManager;
import com.COLLABOMOD.collabomod.learning.LearningData;
import com.COLLABOMOD.collabomod.learning.VisualSettings;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class SpellExecutor {

    public static void execute(SpellContext ctx) {
        if (ctx.level.isClientSide) return;

        // SpellResolverによって、ctx.physics と ctx.visuals は解決済みのはず。

        // 学習データの記録準備
        float[] attrVec = AnalysisEngine.analyzeScript(ctx.script);
        VisualSettings visualSettings = new VisualSettings(ctx.visuals.mainColor, ctx.visuals.shape, ctx.visuals.animationType);
        LearningData learningData = new LearningData(attrVec, visualSettings);

        // 実行分岐: 遅延発動 (Sequence) か 即時発動 (Phenomenon) か

        if (ctx.castTime > 0 && !ctx.fromSequence) {
            // --- A. 遅延発動 (EntityMagicSequence) ---
            EntityMagicSequence sequence = new EntityMagicSequence(ctx, ctx.castTime);

            // ■ 修正: ターゲットの状況に応じて魔法陣の位置と向きを決定
            Vec3 spawnPos;
            if (ctx.target != null && ctx.caster != null) {
                if (ctx.target.isOnGround()) {
                    // ターゲットが地面にいる -> ターゲットの足元に展開
                    spawnPos = ctx.target.position();
                } else {
                    // ターゲットが空中 -> 術者とターゲットの中間地点に展開
                    spawnPos = ctx.caster.getEyePosition().lerp(ctx.target.getEyePosition(), 0.5);
                }
                sequence.setPos(spawnPos);
                // 魔法陣をターゲットの方向に向ける
                sequence.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, ctx.target.getEyePosition());
            } else if (ctx.caster != null) {
                // ターゲットがいない -> 術者の少し前に展開
                spawnPos = ctx.caster.getEyePosition().add(ctx.caster.getLookAngle().scale(1.5));
                sequence.setPos(spawnPos);
                sequence.setYRot(ctx.caster.getYRot());
                sequence.setXRot(ctx.caster.getXRot());
            } else {
                // 術者もターゲットもいない場合 (環境魔法など)
                spawnPos = ctx.origin;
                sequence.setPos(spawnPos);
            }

            ctx.level.addFreshEntity(sequence);

            // 詠唱開始音
            if (spawnPos != null) {
                ctx.level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
            }

        } else {
            // --- B. 即時発動 (EntitySciencePhenomenon) ---
            EntitySciencePhenomenon phenomenon = new EntitySciencePhenomenon(ctx.level, ctx.caster);

            phenomenon.setPhysicsMetadata(ctx.physics);
            phenomenon.setVisualMetadata(ctx.visuals);

            // 出現位置の決定
            Vec3 spawnPos = ctx.origin;
            // ターゲット指定や射出系の場合は位置調整
            if (ctx.physics.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL && ctx.caster != null) {
                spawnPos = ctx.caster.getEyePosition().add(ctx.caster.getLookAngle().scale(0.5));
            } else if (ctx.target != null) {
                spawnPos = ctx.target.position().add(0, 0.5, 0);
            } else if (spawnPos == null && ctx.caster != null) {
                spawnPos = ctx.caster.position();
            }

            if (spawnPos != null) {
                phenomenon.setPos(spawnPos);
            }

            // 初速の適用 (Directionalなら向いている方向へ)
            if (ctx.physics.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL && ctx.caster != null) {
                Vec3 look = ctx.caster.getLookAngle();
                phenomenon.setDeltaMovement(look.scale(Math.max(0.5, ctx.physics.velocity)));
            }

            ctx.level.addFreshEntity(phenomenon);

            // 発動音
            if (spawnPos != null) {
                ctx.level.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                        SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            // 自己評価 & 学習システムへの登録
            // 1. プレイヤー評価用のデータを登録
            CardinalLearningManager.getInstance().registerInteraction(learningData);
            // 2. AIによる自己評価を実行
            float consistency = AnalysisEngine.calculateConsistencyScore(ctx.physics, ctx.visuals);
            CardinalLearningManager.getInstance().selfEvaluate(learningData, consistency);
        }
    }
}