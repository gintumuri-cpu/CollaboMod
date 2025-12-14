package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;

public class LearningData {
    // --- 入力特徴量 (Context) ---
    public int scriptHash;           // 魔法式のハッシュ
    public float[] attributeVector;  // [火, 氷, 風, 土, 光, 闇] などの属性強度
    public float complexity;         // 魔法式の複雑度

    // --- 出力設定 (Action) ---
    public VisualMetadata visualSettings;

    // --- 評価 (Reward) ---
    // 開発者による手動評価スコア (-1.0: Bad, 0.0: Neutral, 1.0: Good)
    public float developerScore;

    public LearningData() {
        this.visualSettings = new VisualMetadata();
        this.attributeVector = new float[6];
    }

    public LearningData(int hash, float[] attributes, float complexity, VisualMetadata visuals) {
        this.scriptHash = hash;
        this.attributeVector = attributes;
        this.complexity = complexity;
        this.visualSettings = visuals;
        this.developerScore = 0.0f;
    }

    // --- 簡易シリアライズ (本来はGSON等を使うが、NBTで代用も可) ---
    // ここでは構造の定義のみ
}