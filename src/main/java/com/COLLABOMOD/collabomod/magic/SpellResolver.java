package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import java.util.List;

public class SpellResolver {

    // 複数のコンポーネントから「最終的な見た目」を決定する
    public static VisualMetadata resolveVisuals(List<MagicComponentType> components) {
        VisualMetadata finalMeta = new VisualMetadata();
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

    // ■ 修正: 新しいコンポーネントに対応した時間計算
    public static int calculateCastTime(List<MagicComponentType> components) {
        int maxTime = 20; // 基本キャストタイム (1秒)

        for (MagicComponentType comp : components) {

            // 1. 戦略級モジュールがある場合 -> 詠唱時間を長くする
            if (comp == MagicComponentType.MOD_STRATEGIC) {
                maxTime = Math.max(maxTime, 100); // 5秒
            }

            // 2. 分解属性がある場合（グラム） -> 即時発動にする
            // グラムは「魔法式を展開せずに発射する」魔法なので 0 にする
            else if (comp == MagicComponentType.ATTRIB_DECOMPOSITION) {
                return 0; // 強制的に即時発動
            }

            // その他: 必要なら他のコンポーネントでも時間を調整
            // 例: ACT_RESTORE (回復) は少し長い、など
            else if (comp == MagicComponentType.ACT_RESTORE) {
                maxTime = Math.max(maxTime, 40);
            }
        }
        return maxTime;
    }
}
