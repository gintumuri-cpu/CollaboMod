package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;

public class VisualMetadata {
    public String rendererID; // レンダラーの種類 ("default", "material_burst" 等)
    public int priority;      // 優先度 (高い方が勝つ)
    public Vector3f color;    // 色
    public float scale;       // サイズ倍率

    // デフォルトコンストラクタ
    public VisualMetadata() {
        this.rendererID = "default";
        this.priority = 0;
        this.color = new Vector3f(1.0F, 1.0F, 1.0F);
        this.scale = 1.0F;
    }

    // 指定コンストラクタ
    public VisualMetadata(String rendererID, int priority, Vector3f color, float scale) {
        this.rendererID = rendererID;
        this.priority = priority;
        this.color = color;
        this.scale = scale;
    }

    // ■ 情報を合成するメソッド (Merge)
    public void merge(VisualMetadata other) {
        // 1. 優先度が高い方のレンダラーIDを採用（上書き）
        if (other.priority > this.priority) {
            this.rendererID = other.rendererID;
            this.priority = other.priority;
            // 優先度が高い場合、ベースサイズもそちらに合わせる
            this.scale = other.scale;
        }

        // 2. 色は混ぜ合わせる（加算平均）
        // (R1+R2)/2
        this.color.add(other.color);
        this.color.mul(0.5F);
    }
}