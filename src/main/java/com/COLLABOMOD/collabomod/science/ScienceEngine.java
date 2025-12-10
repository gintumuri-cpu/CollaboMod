package com.COLLABOMOD.collabomod.science;

import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.mojang.math.Vector3f;
import net.minecraft.world.phys.Vec3;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

public class ScienceEngine {

    public static void simulateVisuals(ScienceContext ctx) {
        VisualMetadata v = ctx.visuals;

        // 1. 形状決定
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            // マテリアルバースト級
            float energyDensity = ctx.energy / (Math.max(1.0F, ctx.radius * ctx.radius));
            if (energyDensity > 5.0F || ctx.compMatter > 0.8F) {
                v.rendererID = "material_burst";
                v.shape = EnumMagicShape.SPHERE;
                v.hasLightning = true;
            }
            // ■ 追加: 防御魔法 (Shield) なら必ず球体
            else if (ctx.compShield > 0.0F) {
                v.rendererID = "shield_dome";
                v.shape = EnumMagicShape.SPHERE;
                v.isSolid = true; // 実体感を持たせる
            }
            else {
                v.rendererID = "explosion_sphere";
                v.shape = EnumMagicShape.SPHERE;
            }
        } else {
            // デフォルトは魔法陣
            v.shape = EnumMagicShape.RING;
        }

        // 2. 色決定
        Vector3f baseColor = new Vector3f(v.mainColor.x(), v.mainColor.y(), v.mainColor.z());
        // 温度による加算 (白く光らせる)
        if (ctx.temperature > 1000.0F) {
            // 温度が高いほど白(1,1,1)に近づける
            float t = Math.min(1.0F, (ctx.temperature - 1000.0F) / 5000.0F);
            lerpColor(baseColor, new Vector3f(1.0F, 1.0F, 1.0F), t);
        }

        // 成分による色干渉 (元の色にさらに混ぜる)
        if (ctx.compMatter > 0.5F) lerpColor(baseColor, new Vector3f(0.1F, 0.1F, 0.8F), ctx.compMatter * 0.5F); // 分解: 青
        if (ctx.compWave > 0.5F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 0.8F), ctx.compWave * 0.5F);   // 振動: シアン
        if (ctx.compShield > 0.0F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 1.0F), 0.8F); // 防御: 明るいシアン

        v.mainColor = baseColor;
        v.subColor = new Vector3f(baseColor.x(), baseColor.y(), baseColor.z());

        // 3. サイズ
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            v.scale = ctx.radius / 2.0F; // サイズ調整 (少し大きめに)
            // マテリアルバーストなら更に巨大化
            if (ctx.energy > 1000.0F) v.scale = ctx.radius / 25.0F;
        } else {
            // 魔法陣のサイズ
            v.scale = 1.0F + (ctx.energy / 100.0F);
        }
    }

    private static void lerpColor(Vector3f a, Vector3f b, float t) {
        float inv = 1.0F - t;
        a.set(a.x()*inv + b.x()*t, a.y()*inv + b.y()*t, a.z()*inv + b.z()*t);
    }
}
