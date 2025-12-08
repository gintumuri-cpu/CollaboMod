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
            float energyDensity = ctx.energy / (ctx.radius * ctx.radius);
            if (energyDensity > 5.0F || ctx.compMatter > 0.8F) {
                v.rendererID = "material_burst"; // 特殊レンダラーID
                v.shape = EnumMagicShape.SPHERE;
                v.hasLightning = true;
            } else {
                v.rendererID = "explosion_sphere";
                v.shape = EnumMagicShape.SPHERE;
            }
        } else {
            v.shape = EnumMagicShape.RING;
        }

        // 2. 色決定
        Vector3f baseColor;
        if (ctx.temperature < 1000.0F) baseColor = new Vector3f(1.0F, 0.4F, 0.0F);
        else if (ctx.temperature < 4000.0F) baseColor = new Vector3f(1.0F, 1.0F, 1.0F);
        else baseColor = new Vector3f(0.2F, 0.9F, 1.0F);

        if (ctx.compMatter > 0.5F) lerpColor(baseColor, new Vector3f(0.1F, 0.1F, 0.8F), ctx.compMatter * 0.8F);
        if (ctx.compWave > 0.5F) lerpColor(baseColor, new Vector3f(0.0F, 1.0F, 0.8F), ctx.compWave * 0.6F);

        v.mainColor = baseColor;

        // 3. サイズ
        if (ctx.type == PhenomenonType.SPHERE_EXPANSION) {
            v.scale = ctx.radius / 25.0F;
        } else {
            v.scale = 1.0F;
        }
    }

    private static void lerpColor(Vector3f a, Vector3f b, float t) {
        float inv = 1.0F - t;
        a.set(a.x()*inv + b.x()*t, a.y()*inv + b.y()*t, a.z()*inv + b.z()*t);
    }
}
