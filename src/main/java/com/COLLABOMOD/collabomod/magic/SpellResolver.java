package com.COLLABOMOD.collabomod.magic;

//import com.COLLABOMOD.collabomod.science.ScienceEngine; // 科学エンジンを使用
//import java.util.List;
//
//public class SpellResolver {
//
//    // 見た目の解決
//    public static VisualMetadata resolveVisuals(List<MagicComponentType> components) {
//        // 1. ダミー設計図を作成
//        SpellContext ctx = new SpellContext();
//
//        // 2. 適用
//        for (MagicComponentType comp : components) {
//            comp.apply(ctx);
//        }
//
//        // 3. 科学エンジンで見た目を自動生成 (パラメータから色などを決定)
//        ScienceEngine.simulateVisuals(ctx.science);
//
//        // 4. 強制的な見た目指定（Material Burstなど）があればマージ
//        for (MagicComponentType comp : components) {
//            ctx.science.visuals.merge(comp.visuals);
//        }
//
//        return ctx.science.visuals;
//    }
//
//    // ■ 修正: コスト計算をシミュレーションベースに変更
//    public static int calculateTotalCost(List<MagicComponentType> components) {
//        // ダミー設計図を作成
//        SpellContext ctx = new SpellContext();
//
//        // 全コンポーネントを適用
//        for (MagicComponentType comp : components) {
//            comp.apply(ctx); // これにより ctx.cost が加算・乗算される
//        }
//
//        return ctx.cost;
//    }
//
//    // キャスト時間の計算
//    public static int calculateCastTime(List<MagicComponentType> components) {
//        SpellContext ctx = new SpellContext();
//        for (MagicComponentType comp : components) {
//            comp.apply(ctx); // これにより ctx.castTime が加算される
//        }
//        return ctx.castTime;
//    }
//}


//教師データ作成用↓
import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.learning.LearningData;
import com.COLLABOMOD.collabomod.science.ScienceEngine;
import java.util.List;
import java.util.ArrayList;

public class SpellResolver {

    public static VisualMetadata resolveVisuals(List<MagicComponentType> components, List<String> scriptCode) {
        SpellContext ctx = new SpellContext();

        // 1. 物理演算
        for (MagicComponentType comp : components) {
            comp.apply(ctx);
        }
        ScienceEngine.simulateVisuals(ctx.science);

        // 2. AI推論
        List<String> analysisTarget = scriptCode != null ? scriptCode : new ArrayList<>();
        if (analysisTarget.isEmpty()) {
            for(MagicComponentType c : components) analysisTarget.add(c.name());
        }

        float[] attributes = AnalysisEngine.analyzeScript(analysisTarget);

        long seed = analysisTarget.hashCode();
        VisualMetadata aiVisuals = AnalysisEngine.deriveVisualsFromAttributes(attributes, seed);

        // 3. 物理統制
        aiVisuals.scale = ctx.science.visuals.scale;

        return aiVisuals;
    }

    public static VisualMetadata resolveVisuals(List<MagicComponentType> components) {
        return resolveVisuals(components, null);
    }

    public static int calculateTotalCost(List<MagicComponentType> components) {
        SpellContext ctx = new SpellContext();
        for (MagicComponentType comp : components) {
            comp.apply(ctx);
        }
        return ctx.cost;
    }

    public static int calculateCastTime(List<MagicComponentType> components) {
        SpellContext ctx = new SpellContext();
        for (MagicComponentType comp : components) {
            comp.apply(ctx);
        }
        return ctx.castTime;
    }
}