package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnimation;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class AttributePreference {

    // 各属性（0~5）ごとの学習データ
    // [0:Fire, 1:Ice, 2:Air, 3:Entropy, 4:Divine]
    public final AttributeNode[] nodes = new AttributeNode[5];

    public AttributePreference() {
        // 初期化：最初は真っ白（または開発者定義のデフォルト値を入れても良い）
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new AttributeNode();
            // 初期バイアス（コールドスタート対策）として、少しだけ色味を入れておく
            // これがないと最初は全部「黒」からスタートになってしまうため
            setDefaultBias(i, nodes[i]);
        }
    }

    // 学習：入力(属性) と 出力(描画) と 評価(スコア) を受け取り、脳を更新する
    public void train(float[] inputAttributes, VisualSettings output, float learningSignal) {
        // 学習率（一度にどれくらい性格を変えるか）。0.1くらいが妥当
        float learningRate = 0.1f;

        // ■ 修正: 配列の長さを Math.min で比較し、範囲外アクセスを完全に防ぐ
        // これにより、万が一 inputAttributes と nodes の長さが異なっていてもクラッシュしなくなる
        int loopCount = Math.min(nodes.length, inputAttributes.length);
        for (int i = 0; i < loopCount; i++) {
            float attrStrength = inputAttributes[i];
            if (attrStrength <= 0.0f)
                continue;

            // 「その属性が強かった」かつ「評価が高かった」場合、その属性の理想像を更新
            // 更新量 = 入力強度 * 学習信号 * 学習率
            float delta = attrStrength * learningSignal * learningRate;

            if (Math.abs(delta) > 0.001f) {
                nodes[i].update(output, delta);
            }
        }
    }

    /**
     * 属性ベクトルから、脳が考える「理想の色」を推論する
     * 
     * @param attributes 魔法の属性ベクトル
     * @return 推論された色
     */
    public Vector3f inferColor(float[] attributes) {
        Vector3f inferredColor = new Vector3f();
        float totalWeight = 0.0f;

        for (int i = 0; i < Math.min(attributes.length, nodes.length); i++) {
            float weight = attributes[i];
            if (weight > 0.01f) {
                inferredColor.add(nodes[i].idealMainColor.x() * weight, nodes[i].idealMainColor.y() * weight,
                        nodes[i].idealMainColor.z() * weight);
                totalWeight += weight;
            }
        }

        if (totalWeight > 0) {
            inferredColor.mul(1.0f / totalWeight);
        } else {
            inferredColor.set(0.5f, 0.5f, 0.5f); // 何も属性がなければ灰色
        }
        return inferredColor;
    }

    /**
     * 属性ベクトルと物理タイプから、脳が好む形状を推論する。
     * 学習データがある属性については好みスコアを重み付き集計し、
     * データ不足時はforceTypeベースのデフォルトにフォールバック。
     */
    public EnumMagicShape preferShape(float[] attributes, PhysicsMetadata.EnumForceType forceType) {
        Map<EnumMagicShape, Float> aggregated = new HashMap<>();

        for (int i = 0; i < Math.min(attributes.length, nodes.length); i++) {
            float weight = attributes[i];
            if (weight <= 0.01f)
                continue;
            for (Map.Entry<EnumMagicShape, Float> entry : nodes[i].shapeScores.entrySet()) {
                aggregated.merge(entry.getKey(), entry.getValue() * weight, Float::sum);
            }
        }

        if (!aggregated.isEmpty()) {
            return aggregated.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(defaultShapeForForce(forceType));
        }
        return defaultShapeForForce(forceType);
    }

    /**
     * 属性ベクトルと形状から、脳が好むアニメーションを推論する。
     */
    public EnumMagicAnimation preferAnimation(float[] attributes, EnumMagicShape shape) {
        Map<EnumMagicAnimation, Float> aggregated = new HashMap<>();

        for (int i = 0; i < Math.min(attributes.length, nodes.length); i++) {
            float weight = attributes[i];
            if (weight <= 0.01f)
                continue;
            for (Map.Entry<EnumMagicAnimation, Float> entry : nodes[i].animScores.entrySet()) {
                aggregated.merge(entry.getKey(), entry.getValue() * weight, Float::sum);
            }
        }

        if (!aggregated.isEmpty()) {
            return aggregated.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(defaultAnimForShape(shape));
        }
        return defaultAnimForShape(shape);
    }

    private EnumMagicShape defaultShapeForForce(PhysicsMetadata.EnumForceType forceType) {
        switch (forceType) {
            case DIRECTIONAL:
                return EnumMagicShape.BEAM;
            case RADIAL:
                return EnumMagicShape.SPHERE;
            case FIELD:
                return EnumMagicShape.RING;
            default:
                return EnumMagicShape.SPHERE;
        }
    }

    private EnumMagicAnimation defaultAnimForShape(EnumMagicShape shape) {
        switch (shape) {
            case BEAM:
            case CYLINDER:
                return EnumMagicAnimation.BEAM_EXTEND;
            case RING:
            case CUBE:
                return EnumMagicAnimation.SUSTAIN_SPIN;
            default:
                return EnumMagicAnimation.EXPAND_FADE;
        }
    }

    private void setDefaultBias(int index, AttributeNode node) {
        // 初期状態の定義（以前のAnalysisEngineのswitch文に相当）
        // ここを定義しておくと「最初からある程度それっぽい」状態になる
        switch (index) {
            case 0:
                node.idealMainColor = new Vector3f(1.0f, 0.2f, 0.0f);
                break; // Fire
            case 1:
                node.idealMainColor = new Vector3f(0.4f, 0.8f, 1.0f);
                break; // Ice
            case 2:
                node.idealMainColor = new Vector3f(0.2f, 1.0f, 0.6f);
                break; // Air
            case 3:
                node.idealMainColor = new Vector3f(0.5f, 0.0f, 0.8f);
                break; // Entropy (紫)
            case 4:
                node.idealMainColor = new Vector3f(1.0f, 1.0f, 0.8f);
                break; // Divine (白金)
            default:
                node.idealMainColor = new Vector3f(0.5f, 0.5f, 0.5f);
        }
    }

    // --- 内部クラス：1つの属性に対する好み ---
    public static class AttributeNode {
        public Vector3f idealMainColor = new Vector3f(0.5f, 0.5f, 0.5f);
        public Vector3f idealSubColor = new Vector3f(0.5f, 0.5f, 0.5f);

        // 各フラグが「Trueであるべき」スコア (-1.0 ~ 1.0)
        public float spikyScore = 0.0f;
        public float wavyScore = 0.0f;
        public float lightningScore = 0.0f;

        // 形状・アニメーションの好み（累積スコア）
        public Map<EnumMagicShape, Float> shapeScores = new HashMap<>();
        public Map<EnumMagicAnimation, Float> animScores = new HashMap<>();

        public void update(VisualSettings settings, float delta) {
            // 色の学習 (ベクトル補間)
            // positiveならその色に近づき、negativeなら遠ざかる
            lerpColor(idealMainColor, settings.color, delta);
            // subColorはVisualSettingsにないので学習しない

            // パラメータの学習はVisualSettingsにないので一旦コメントアウト
            // if (meta.isSpiky) spikyScore += delta; else spikyScore -= delta;
            // if (meta.isWavy) wavyScore += delta; else wavyScore -= delta;
            // if (meta.hasLightning) lightningScore += delta; else lightningScore -= delta;

            // 形状の学習
            shapeScores.put(settings.shape, shapeScores.getOrDefault(settings.shape, 0.0f) + delta);

            // アニメーションの学習
            animScores.put(settings.animation, animScores.getOrDefault(settings.animation, 0.0f) + delta);
        }

        private void lerpColor(Vector3f current, Vector3f target, float t) {
            // 単純な線形補間だが、tがマイナスの場合は「逆方向」に動く
            // Clamp処理を入れて0~1の範囲を維持する
            float nx = clamp(current.x() + (target.x() - current.x()) * t);
            float ny = clamp(current.y() + (target.y() - current.y()) * t);
            float nz = clamp(current.z() + (target.z() - current.z()) * t);
            current.set(nx, ny, nz);
        }

        private float clamp(float v) {
            return Math.max(0.0f, Math.min(1.0f, v));
        }
    }
}