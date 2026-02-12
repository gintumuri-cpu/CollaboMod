package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.learning.AnalysisEngine;

import java.util.ArrayList;
import java.util.List;

public class SpellResolver {

    /**
     * 【新】スクリプトから魔法のコンテキスト（物理・視覚含む）を完全解決する。
     * これが二段階推論フローの本体となる。
     */
    public static SpellContext resolve(List<String> scriptCode) {
        SpellContext ctx = new SpellContext();

        // スクリプトのセット（null安全）
        ctx.script = scriptCode != null ? scriptCode : new ArrayList<>();
        long seed = ctx.script.hashCode();

        // --- ステップ1: スクリプトから「属性ベクトル」を抽出 ---
        // これは魔法全体の雰囲気（色やテーマ）に影響する
        float[] attributes = AnalysisEngine.analyzeScript(ctx.script);

        // --- ステップ2: スクリプトと属性から「物理現象」を推論 ---
        // "damage(10)" や "knockback(5)" といったロジックを解釈し、PhysicsMetadataを構築する
        ctx.physics = AnalysisEngine.derivePhysicsFromScript(ctx.script, attributes);

        // --- ステップ3: 確定した「物理現象」と属性から「描画」を連想 ---
        // AIに、物理現象に最もふさわしい見た目をデザインさせる
        ctx.visuals = AnalysisEngine.deriveVisualsFromPhysics(ctx.physics, attributes, seed);

        // --- ステップ4: 詠唱時間の設定 ---
        // 物理現象の複雑さ（エネルギーや質量）に応じて詠唱時間を決定する
        ctx.castTime = (int) (ctx.physics.energy / 10.0f + ctx.physics.mass);

        return ctx;
    }

    // GUIのプレビューなど、見た目だけ欲しい場合用のヘルパー
    public static VisualMetadata resolveVisuals(List<String> scriptCode) {
        return resolve(scriptCode).visuals;
    }
}