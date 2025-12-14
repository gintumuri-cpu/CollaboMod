package com.COLLABOMOD.collabomod.science;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnchor;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

public class ScienceEngine {

    public static void simulateVisuals(ScienceContext ctx) {
        VisualMetadata v = ctx.visuals;

        // 1. 形状決定ロジックの高度化
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {

            // ■ 追加: 足元(TARGET_ANCHORED)からの爆発なら「柱」にする
            if (v.anchorType == EnumMagicAnchor.TARGET_ANCHORED) {
                v.shape = EnumMagicShape.CYLINDER;
                v.rendererID = "energy_pillar";
            }
            // 通常の球体爆発
            else {
                float energyDensity = ctx.energy / (Math.max(1.0F, ctx.radius * ctx.radius));
                if (energyDensity > 5.0F || ctx.compMatter > 0.8F) {
                    v.rendererID = "material_burst";
                    v.shape = EnumMagicShape.SPHERE;
                    v.hasLightning = true;
                } else if (ctx.compShield > 0.0F) {
                    v.rendererID = "shield_dome";
                    v.shape = EnumMagicShape.SPHERE;
                    v.isSolid = true;
                } else {
                    v.rendererID = "explosion_sphere";
                    v.shape = EnumMagicShape.SPHERE;
                }
            }
        } else {
            v.shape = EnumMagicShape.RING;
        }

        // 2. 色決定
        Vector3f baseColor = new Vector3f(v.mainColor.x(), v.mainColor.y(), v.mainColor.z());
        if (ctx.temperature > 1000.0F) {
            float t = Math.min(1.0F, (ctx.temperature - 1000.0F) / 5000.0F);
            lerpColor(baseColor, new Vector3f(1.0F, 1.0F, 1.0F), t);
        }
        if (ctx.compMatter > 0.5F) lerpColor(baseColor, new Vector3f(0.1F, 0.1F, 0.8F), ctx.compMatter * 0.5F);
        if (ctx.compWave > 0.5F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 0.8F), ctx.compWave * 0.5F);
        if (ctx.compShield > 0.0F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 1.0F), 0.8F);

        v.mainColor = baseColor;
        v.subColor = new Vector3f(baseColor.x(), baseColor.y(), baseColor.z());

        // 3. サイズ
        if (v.shape == EnumMagicShape.SPHERE || v.shape == EnumMagicShape.CYLINDER) {
            v.scale = ctx.radius; // 半径をそのまま反映
            if (ctx.energy > 1000.0F) v.scale = ctx.radius;
        } else {
            v.scale = 1.0F + (ctx.energy / 100.0F);
        }
    }

    private static void lerpColor(Vector3f a, Vector3f b, float t) {
        float inv = 1.0F - t;
        a.set(a.x()*inv + b.x()*t, a.y()*inv + b.y()*t, a.z()*inv + b.z()*t);
    }
}