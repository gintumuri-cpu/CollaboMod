package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnchor;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AnalysisEngine {

    private static final Random random = new Random();

    // 属性インデックス定義
    private static final int FIRE = 0;
    private static final int ICE = 1;
    private static final int AIR = 2;
    private static final int EARTH = 3;
    private static final int FORCE = 4;
    private static final int LIFE = 5;

    public static float[] analyzeScript(List<String> script) {
        float[] vector = new float[6];
        for (String line : script) {
            String l = line.toLowerCase();
            if (l.contains("fire") || l.contains("explode") || l.contains("burst")) vector[FIRE] += 1.0f;
            if (l.contains("ice") || l.contains("freeze") || l.contains("water"))   vector[ICE] += 1.0f;
            if (l.contains("air") || l.contains("fly") || l.contains("move") || l.contains("wind")) vector[AIR] += 1.0f;
            if (l.contains("rock") || l.contains("protect") || l.contains("defend") || l.contains("shield")) vector[EARTH] += 1.0f;
            if (l.contains("shoot") || l.contains("accel") || l.contains("gram"))  vector[FORCE] += 1.0f;
            if (l.contains("restore") || l.contains("heal") || l.contains("regrowth")) vector[LIFE] += 1.0f;
        }
        float sum = 0;
        for (float v : vector) sum += v;
        if (sum > 0) {
            for (int i = 0; i < vector.length; i++) vector[i] /= sum;
        }
        return vector;
    }

    public static float calculateComplexity(List<String> script) { return (float) script.size(); }

    public static VisualMetadata predictVisuals(float[] attributes) {
        // 学習データがあればそれを使う (既存ロジック)
        List<LearningData> neighbors = CardinalLearningManager.getInstance().findNearestNeighbors(attributes, 3);
        if (neighbors.isEmpty()) {
            // データがない場合は「ランダム」ではなく「スマート生成」を行う
            return deriveVisualsFromAttributes(attributes, random.nextLong());
        }
        // ... (既存の学習データ利用ロジックはそのまま残す) ...
        return deriveVisualsFromAttributes(attributes, random.nextLong()); // 一旦ここもスマート生成に委譲（マージロジックは後で戻しても良い）
    }

    // ■ 新設: 属性から「それっぽい」見た目を自動導出する (Semantic Logic)
    public static VisualMetadata deriveVisualsFromAttributes(float[] attributes, long seed) {
        Random rand = new Random(seed);
        VisualMetadata meta = new VisualMetadata();

        // 1. 最も強い属性を特定
        int dominant = -1;
        float maxVal = 0.0f;
        for(int i=0; i<attributes.length; i++) {
            if(attributes[i] > maxVal) {
                maxVal = attributes[i];
                dominant = i;
            }
        }
        // 属性がない場合は Force 扱い
        if (dominant == -1) dominant = FORCE;

        // 2. 属性ごとのスタイル定義 (Style Definition)
        switch (dominant) {
            case FIRE: // 赤・オレンジ・爆発・トゲ
                meta.mainColor = new Vector3f(1.0f, 0.3f, 0.0f); // Orange Red
                meta.subColor  = new Vector3f(1.0f, 0.8f, 0.0f); // Yellow
                meta.shape = EnumMagicShape.SPHERE;
                meta.isSpiky = true;
                meta.isWavy = true;
                meta.anchorType = EnumMagicAnchor.TARGET_ANCHORED; // 敵中心に爆発
                meta.rendererID = "material_burst";
                break;

            case ICE: // 水色・白・結晶・円柱
                meta.mainColor = new Vector3f(0.4f, 0.8f, 1.0f); // Ice Blue
                meta.subColor  = new Vector3f(0.8f, 0.9f, 1.0f); // White
                meta.shape = EnumMagicShape.CYLINDER; // 氷柱
                meta.isSpiky = true; // 鋭い
                meta.isWavy = false;
                meta.anchorType = EnumMagicAnchor.WORLD_FIXED;
                meta.rendererID = "ice_pillar";
                break;

            case AIR: // 緑・白・リング・風
                meta.mainColor = new Vector3f(0.2f, 1.0f, 0.6f); // Wind Green
                meta.subColor  = new Vector3f(1.0f, 1.0f, 1.0f); // White
                meta.shape = EnumMagicShape.RING;
                meta.isSpiky = false;
                meta.isWavy = true; // ゆらぎ
                meta.rotationSpeed = 2.0f; // 高速回転
                meta.rendererID = "wind_ring";
                break;

            case EARTH: // 茶・黄・球体・シールド
                meta.mainColor = new Vector3f(1.0f, 0.6f, 0.2f); // Amber
                meta.subColor  = new Vector3f(0.6f, 0.4f, 0.2f); // Brown
                meta.shape = EnumMagicShape.SPHERE;
                meta.isSpiky = false;
                meta.isWavy = false; // 安定
                meta.isSolid = true; // 硬そう
                meta.anchorType = EnumMagicAnchor.CASTER_ANCHORED;
                meta.rendererID = "shield_dome";
                break;

            case LIFE: // ピンク・緑・パルス
                meta.mainColor = new Vector3f(1.0f, 0.4f, 0.8f); // Pink
                meta.subColor  = new Vector3f(0.5f, 1.0f, 0.5f); // Green
                meta.shape = EnumMagicShape.RING;
                meta.isSpiky = false;
                meta.isWavy = true;
                meta.rendererID = "restore_circle";
                break;

            default: // FORCE (無属性): シアン・幾何学的
                meta.mainColor = new Vector3f(0.0f, 0.8f, 1.0f); // Cyan
                meta.subColor  = new Vector3f(0.0f, 0.2f, 0.8f); // Blue
                meta.shape = EnumMagicShape.SPHERE;
                meta.isSpiky = false;
                meta.isWavy = false;
                meta.rendererID = "default";
                break;
        }

        // 3. 微調整 (Mutation) - AIの「遊び」
        // 定義通りの色だと味気ないので、Seedに基づいて少しだけズラす
        float hueShift = (rand.nextFloat() - 0.5f) * 0.2f; // 色相を少しずらす
        meta.mainColor.add(hueShift, hueShift, hueShift);

        // サイズのばらつき
        meta.scale = 1.0f + rand.nextFloat() * 1.5f;

        // レイヤー数
        meta.layerCount = 1 + rand.nextInt(3);

        return meta;
    }

    // 互換性のために残すが、基本は deriveVisualsFromAttributes を使う
    public static VisualMetadata generateRandomVisuals(float[] attributes, long seed) {
        return deriveVisualsFromAttributes(attributes, seed);
    }
}