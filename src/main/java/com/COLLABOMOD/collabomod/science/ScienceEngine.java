package com.COLLABOMOD.collabomod.science;

import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.magic.EnumMagicAnchor;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

public class ScienceEngine {


//    public static void simulateVisuals(ScienceContext ctx) {
//        VisualMetadata v = ctx.visuals;
//
//        float energyFactor = Math.min(1.0F, ctx.energy / 10000.0F);
//        float tempFactor = Math.min(1.0F, (ctx.temperature - 300.0F) / 4700.0F);
//        v.normalizedIntensity = Math.max(energyFactor, tempFactor);
//
//        float entropy = 0.0F;
//        if (ctx.compMatter > 0) entropy += 0.5F;
//        if (ctx.compWave > 0) entropy += 0.3F;
//        if (ctx.temperature > 2000.0F) entropy += 0.2F;
//        v.entropy = Math.min(1.0F, entropy);
//
//        // 1. 形状決定ロジックの高度化
//        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
//
//            // ■ 追加: 足元(TARGET_ANCHORED)からの爆発なら「柱」にする
//            if (v.anchorType == EnumMagicAnchor.TARGET_ANCHORED) {
//                v.shape = EnumMagicShape.CYLINDER;
//                v.rendererID = "energy_pillar";
//            }
//            // 通常の球体爆発
//            else {
//                float energyDensity = ctx.energy / (Math.max(1.0F, ctx.radius * ctx.radius));
//                if (energyDensity > 5.0F || ctx.compMatter > 0.8F) {
//                    v.rendererID = "material_burst";
//                    v.shape = EnumMagicShape.SPHERE;
//                    v.hasLightning = true;
//                } else if (ctx.compShield > 0.0F) {
//                    v.rendererID = "shield_dome";
//                    v.shape = EnumMagicShape.SPHERE;
//                    v.isSolid = true;
//                } else {
//                    v.rendererID = "explosion_sphere";
//                    v.shape = EnumMagicShape.SPHERE;
//                }
//            }
//        } else {
//            v.shape = EnumMagicShape.RING;
//        }
//
//        // 2. 色決定
//        Vector3f baseColor = new Vector3f(v.mainColor.x(), v.mainColor.y(), v.mainColor.z());
//        if (ctx.temperature > 1000.0F) {
//            float t = Math.min(1.0F, (ctx.temperature - 1000.0F) / 5000.0F);
//            lerpColor(baseColor, new Vector3f(1.0F, 1.0F, 1.0F), t);
//        }
//        if (ctx.compMatter > 0.5F) lerpColor(baseColor, new Vector3f(0.1F, 0.1F, 0.8F), ctx.compMatter * 0.5F);
//        if (ctx.compWave > 0.5F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 0.8F), ctx.compWave * 0.5F);
//        if (ctx.compShield > 0.0F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 1.0F), 0.8F);
//
//        v.mainColor = baseColor;
//        v.subColor = new Vector3f(baseColor.x(), baseColor.y(), baseColor.z());
//
//        // 3. サイズ
//        if (v.shape == EnumMagicShape.SPHERE || v.shape == EnumMagicShape.CYLINDER) {
//            v.scale = ctx.radius; // 半径をそのまま反映
//            if (ctx.energy > 1000.0F) v.scale = ctx.radius;
//        } else {
//            v.scale = 1.0F + (ctx.energy / 100.0F);
//        }
//    }
//
//    private static void lerpColor(Vector3f a, Vector3f b, float t) {
//        float inv = 1.0F - t;
//        a.set(a.x()*inv + b.x()*t, a.y()*inv + b.y()*t, a.z()*inv + b.z()*t);
//    }
//}




//ランダム生成
// 互換性用 (Seedなしで呼ばれたらランダム)
public static void simulateVisuals(ScienceContext ctx) {
    simulateVisuals(ctx, new java.util.Random().nextInt());
}

    // ■ 本命: Seedに基づく描画生成
    public static void simulateVisuals(ScienceContext ctx, int seed) {

        // 1. AI (AnalysisEngine) にベースとなる描画設定を生成させる
        // ここで Shape, Color, Spiky, Wavy などが Seed に基づいて決定される
        // 属性ベクトルはダミー (new float[6]) で渡すが、将来的には ctx.temperature 等から作成しても良い
        VisualMetadata aiMeta = AnalysisEngine.generateRandomVisuals(new float[6], seed);

        // 生成されたメタデータを Context に適用
        ctx.visuals = aiMeta;
        VisualMetadata v = ctx.visuals;

        // 2. 物理量による「スケーリング」のみ適用 (形状や色はAIが決めたものを尊重)

        // 強度計算
        float energyFactor = Math.min(1.0F, ctx.energy / 10000.0F);
        float tempFactor = Math.min(1.0F, (ctx.temperature - 300.0F) / 4700.0F);
        v.normalizedIntensity = Math.max(energyFactor, tempFactor);

        // サイズ計算 (物理的な半径に合わせる)
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            v.scale = ctx.radius; // 半径を適用
            // ※ここで v.shape = SPHERE と書いていた行を削除！
            // AIが CYLINDER を選んだなら、CYLINDER のまま半径だけ適用される。
        }
        else {
            v.scale = 1.0F + (ctx.energy / 100.0F);
        }

        // 3. 物理的な微調整 (補正)
        // AIが決定した色に対して、極端な温度なら少しだけ加算する程度に留める
        if (ctx.temperature > 2000.0F) {
            // 白熱 (少し白を混ぜる)
            v.mainColor.add(0.2f, 0.2f, 0.2f);
        }

        // シールド成分がある場合、透明度や質感をいじる (形状は変えない)
        if (ctx.compShield > 0.0F) {
            v.isSolid = true;
        }
    }
}