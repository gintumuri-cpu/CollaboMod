package com.COLLABOMOD.collabomod.science;

import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

public class ScienceContext {

    // 基本物理量
    public float energy = 10.0F;
    public float temperature = 300.0F;
    public float mass = 0.0F;
    public float density = 1.0F;

    // 挙動
    public float velocity = 1.0F;
    public float radius = 1.0F;
    public PhenomenonType type = PhenomenonType.POINT;

    // 成分 (0.0~1.0)
    public float compMatter = 0.0F; // 物質分解
    public float compWave = 0.0F;   // 振動
    public float compLight = 0.0F;  // 光
    public float compShield = 0.0F; // ■ 追加: 防御・干渉成分

    // 出力される見た目
    public VisualMetadata visuals = new VisualMetadata();

    // 計算用一時変数
    public Vector3f coreColor = new Vector3f(1,1,1);
    public Vector3f outerColor = new Vector3f(1,1,1);
    public float lightningIntensity = 0.0F;
    public float distortionIntensity = 0.0F;

    public ScienceContext() {}

    public static ScienceContext createStrategicClass() {
        ScienceContext ctx = new ScienceContext();
        ctx.type = PhenomenonType.SPHERE_EXPANSION;
        ctx.energy = 10000.0F;
        ctx.radius = 50.0F;
        ctx.velocity = 0.2F;
        ctx.compMatter = 1.0F;
        ctx.temperature = 5000.0F;
        // VisualMetadataもここで設定しておくと安全
        ctx.visuals = new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F);
        ctx.visuals = new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F);
        ctx.visuals.shape = EnumMagicShape.SPHERE;
        ctx.visuals.hasLightning = true;

        return ctx;
    }
}
