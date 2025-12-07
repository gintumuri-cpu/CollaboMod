package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import java.util.List;

public class SpellResolver {

    // 複数のコンポーネントから「最終的な見た目」を決定する
    public static VisualMetadata resolveVisuals(List<MagicComponentType> components) {
        VisualMetadata finalMeta = new VisualMetadata();

        // 全てのマージ処理
        for (MagicComponentType comp : components) {
            finalMeta.merge(comp.visuals);
        }
        return finalMeta;
    }

    // 合計コストの計算
    public static int calculateTotalCost(List<MagicComponentType> components) {
        int cost = 0;
        for (MagicComponentType comp : components) {
            cost += comp.cost;
        }
        return cost;
    }

    // 最大キャスト時間の計算（一番遅い工程に合わせる）
    // ※今回はMagicComponentTypeにcastTimeフィールドがないため、仮計算とします
    // 本来はEnumにcastTimeを持たせるべきです
    public static int calculateCastTime(List<MagicComponentType> components) {
        int maxTime = 10; // 最低保証
        for (MagicComponentType comp : components) {
            // 仮: 戦略級なら長く、射撃なら短いなどの判定
            if (comp == MagicComponentType.MATERIAL_BURST) maxTime = Math.max(maxTime, 100);
            else if (comp == MagicComponentType.PROJECTILE_GRAM) maxTime = Math.max(maxTime, 0); // 即時
            else maxTime = Math.max(maxTime, 20); // 通常
        }
        return maxTime;
    }
}
