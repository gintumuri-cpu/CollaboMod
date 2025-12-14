package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.science.ScienceContext;

public class FeedbackLoop {

    /**
     * 魔法実行後の物理結果を解析し、学習データに情報を付加・補正する
     */
    public static void analyzeResult(LearningData data, ScienceContext result) {
        if (data == null || result == null) return;

        // 1. 物理現象の記録 (コンソール出力で確認)
        // 実際には LearningData に「物理スコア」フィールドを作って保存するのが理想ですが、
        // まずは「開発者が評価する際の参考情報」として処理します。

        float tempDiff = Math.abs(result.temperature - 300.0F);
        float energyUsed = result.energy;

        // 例: 予想以上のエネルギーが発生した場合のログ
        if (energyUsed > 10000.0F) {
            // System.out.println("[Feedback] High Energy Event Detected.");
        }

        // 2. 自動補正の予備動作 (将来的な拡張用)
        // 例: 「爆発」タグがあるのに半径が小さすぎた場合、次回へのペナルティ情報を記録するなど
        // if (result.type == PhenomenonType.SPHERE_EXPANSION && result.radius < 2.0F) { ... }

        // 今回はシンプルにデータを受け取るだけとする
    }
}