package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.learning.AIInferenceCache;
import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.learning.CardinalLearningManager;
import com.COLLABOMOD.collabomod.learning.ExternalAIConfig;
import com.COLLABOMOD.collabomod.learning.ExternalAIService;

import java.util.ArrayList;
import java.util.List;

public class SpellResolver {

    /**
     * スクリプトから魔法のコンテキスト（物理・視覚含む）を完全解決する。
     * 外部AI統合: キャッシュ確認 → ローカルフォールバック → 非同期外部AI推論
     */
    public static SpellContext resolve(List<String> scriptCode) {
        SpellContext ctx = new SpellContext();

        // スクリプトのセット（null安全）
        ctx.script = scriptCode != null ? scriptCode : new ArrayList<>();
        long seed = ctx.script.hashCode();
        int scriptHash = ctx.script.hashCode();

        // --- ステップ1: スクリプトから「属性ベクトル」を抽出 ---
        float[] attributes = AnalysisEngine.analyzeScript(ctx.script);

        // --- ステップ2: スクリプトと属性から「物理現象」を推論 ---
        ctx.physics = AnalysisEngine.derivePhysicsFromScript(ctx.script, attributes);

        // --- ステップ3: 描画の解決（外部AI統合） ---
        VisualMetadata cached = AIInferenceCache.getInstance().get(scriptHash);
        if (cached != null) {
            // キャッシュヒット: 外部AI推論済みの高品質描画を使用
            // rawVector/timeline が欠落している場合は物理パラメータから補完
            ctx.visuals = AnalysisEngine.completeVisualMetadata(cached, ctx.physics, attributes);
            System.out.println("[SpellResolver] Using cached AI visuals for hash: " + scriptHash);
        } else {
            // フォールバック: 既存ローカルAIで即時推論
            ctx.visuals = AnalysisEngine.deriveVisualsFromPhysics(ctx.physics, attributes, seed);

            // 非同期で外部AIに推論依頼（結果は次回以降に使用）
            if (ExternalAIConfig.getInstance().isReady()) {
                final float[] attrCopy = attributes.clone();
                final PhysicsMetadata phyCopy = ctx.physics;
                final List<String> scriptCopy = new ArrayList<>(ctx.script);

                ExternalAIService.getInstance().inferAsync(phyCopy, attrCopy, scriptCopy)
                        .thenAccept(result -> {
                            if (result != null) {
                                AIInferenceCache.getInstance().put(scriptHash, result);
                                // 外部AI結果をローカルAI脳にも逆学習
                                CardinalLearningManager.getInstance().learnFromExternalAI(attrCopy, result);
                                System.out.println("[SpellResolver] External AI result cached + reverse-learned.");
                            }
                        });
            }
        }

        // --- ステップ4: 詠唱時間の設定 ---
        ctx.castTime = (int) (ctx.physics.energy / 10.0f + ctx.physics.mass);

        return ctx;
    }

    // GUIのプレビューなど、見た目だけ欲しい場合用のヘルパー
    public static VisualMetadata resolveVisuals(List<String> scriptCode) {
        return resolve(scriptCode).visuals;
    }
}