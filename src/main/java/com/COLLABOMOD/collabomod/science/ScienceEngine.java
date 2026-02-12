package com.COLLABOMOD.collabomod.science;

import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.util.Mth;

public class ScienceEngine {

    // 互換性用 (Seedなしで呼ばれたらランダム)
    public static void simulateVisuals(ScienceContext ctx) {
        simulateVisuals(ctx, new java.util.Random().nextInt());
    }

    /**
     * 物理コンテキストとSeed値に基づいて、AIに最適な描画を推論させる
     * @param ctx 物理現象のコンテキスト
     * @param seed 描画のランダム性を決定するシード値
     */
    public static void simulateVisuals(ScienceContext ctx, int seed) {

        // 1. ScienceContextからPhysicsMetadataを構築
        PhysicsMetadata phy = new PhysicsMetadata();
        phy.energy = ctx.energy;
        phy.temperature = ctx.temperature;
        phy.areaOfEffect = ctx.radius;
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            phy.forceType = PhysicsMetadata.EnumForceType.RADIAL;
        }
        phy.isSolid = ctx.compShield > 0.0F;

        // 2. 属性ベクトルを生成 (物理量から簡易的に)
        float[] attributes = new float[5];
        attributes[0] = Mth.clamp((ctx.temperature - 300f) / 2000f, 0, 1); // Heat
        attributes[1] = Mth.clamp((300f - ctx.temperature) / 300f, 0, 1);   // Cold

        // 3. AI (AnalysisEngine) にベースとなる描画設定を生成させる
        VisualMetadata aiMeta = AnalysisEngine.deriveVisualsFromPhysics(phy, attributes, seed);

        // 生成されたメタデータを Context に適用
        ctx.visuals = aiMeta;
        VisualMetadata v = ctx.visuals;

        // 4. 物理量による「スケーリング」を適用 (形状や色はAIが決めたものを尊重)
        float energyFactor = Mth.clamp(ctx.energy / 10000.0F, 0, 1);
        float tempFactor = Mth.clamp((ctx.temperature - 300.0F) / 4700.0F, 0, 1);
        v.normalizedIntensity = Math.max(energyFactor, tempFactor);

        // サイズ計算 (物理的な半径に合わせる)
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            v.scale = ctx.radius; // 半径を適用
        } else {
            v.scale = 1.0F + (ctx.energy / 100.0F);
        }

        // 5. 物理的な微調整 (補正)
        // AIが決定した色に対して、極端な温度なら少しだけ加算する
        if (ctx.temperature > 2000.0F) {
            v.mainColor.add(0.2f, 0.2f, 0.2f);
        }

        // シールド成分がある場合、AIの判断に加えてisSolidフラグを立てる
        if (ctx.compShield > 0.0F) {
            v.isSolid = true;
        }
    }
}