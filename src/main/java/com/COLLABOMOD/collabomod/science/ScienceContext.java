package com.COLLABOMOD.collabomod.science;

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

    // 出力される見た目
    public VisualMetadata visuals = new VisualMetadata();

    // 計算用一時変数
    public Vector3f coreColor = new Vector3f(1,1,1);
    public Vector3f outerColor = new Vector3f(1,1,1);
    public float lightningIntensity = 0.0F;
    public float distortionIntensity = 0.0F;

    public ScienceContext() {}
}
