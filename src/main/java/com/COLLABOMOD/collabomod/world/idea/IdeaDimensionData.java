package com.COLLABOMOD.collabomod.world.idea;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class IdeaDimensionData extends SavedData {

    // シングルトン的にアクセスするためのID
    private static final String DATA_NAME = "collabo_mod_idea_dimension";

    // ■ エイドスの履歴保管庫 (イデア)
    // UUID (エンティティ) -> 履歴リスト
    private final Map<UUID, LinkedList<EidosData>> entityHistory = new HashMap<>();

    // BlockPos (ブロック) -> 履歴リスト
    // ※本来は全ブロックを記録すべきですが、負荷対策で「破壊された場所」のみ一時記録します
    private final Map<BlockPos, EidosData> blockHistory = new HashMap<>();

    // 履歴の保持数上限（再成できる限界）
    private static final int MAX_HISTORY = 10;

    // データの取得（なければ新規作成）
    public static IdeaDimensionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                IdeaDimensionData::load,
                IdeaDimensionData::create,
                DATA_NAME
        );
    }

    public static IdeaDimensionData create() {
        return new IdeaDimensionData();
    }

    public static IdeaDimensionData load(CompoundTag nbt) {
        // サーバー再起動時にデータを復元する処理（今回は複雑になるので省略、メモリ内のみで管理）
        return new IdeaDimensionData();
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        // セーブ処理（今回は省略）
        return nbt;
    }

    // ■■■ 事象の記録（エイドスの保存） ■■■

    // エンティティがダメージを受けた時などに呼ぶ
    public void recordEntityState(LivingEntity entity, long time) {
        UUID id = entity.getUUID();
        entityHistory.putIfAbsent(id, new LinkedList<>());

        LinkedList<EidosData> history = entityHistory.get(id);
        // 現在の状態を保存
        history.addFirst(new EidosData(entity, time));

        // 古いデータは消す（イデアの容量節約）
        if (history.size() > MAX_HISTORY) {
            history.removeLast();
        }
        this.setDirty(); // 保存フラグ
    }

    // ■■■ 事象の読み出し（再成用） ■■■

    // 指定したエンティティの「1つ前」のエイドスを取り出す
    public EidosData getPreviousEntityState(UUID uuid) {
        if (entityHistory.containsKey(uuid)) {
            LinkedList<EidosData> history = entityHistory.get(uuid);
            if (!history.isEmpty()) {
                // 最新の履歴（直前の無傷の状態）を返す
                return history.getFirst();
            }
        }
        return null;
    }
    // 履歴を削除するメソッド
    public void clearHistory(UUID uuid) {
        if (entityHistory.containsKey(uuid)) {
            entityHistory.remove(uuid);
            this.setDirty();
        }
    }
}
