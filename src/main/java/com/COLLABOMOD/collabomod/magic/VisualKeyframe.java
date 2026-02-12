package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;

public class VisualKeyframe {
    public float timeStamp; // 0.0 (開始) ~ 1.0 (終了)

    public Vector3f mainColor;
    public Vector3f subColor;
    public float scale;
    public float alpha;

    public EnumMagicShape shape;
    public boolean isSpiky;
    public boolean isWavy;
    public boolean hasLightning;
    public float[] rawVector; // ■ 追加: このキーフレーム時点での形状ベクトル

    public VisualKeyframe(float time, Vector3f main, Vector3f sub, float scale, float alpha, EnumMagicShape shape, boolean spiky, boolean wavy, boolean lightning, float[] rawVector) {
        this.timeStamp = time;
        this.mainColor = main;
        this.subColor = sub;
        this.scale = scale;
        this.alpha = alpha;
        this.shape = shape;
        this.isSpiky = spiky;
        this.isWavy = wavy;
        this.hasLightning = lightning;
        this.rawVector = rawVector;
    }
}