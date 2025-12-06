package com.COLLABOMOD.collabomod.util;
import com.mojang.math.Vector3f;
public enum MagicSpellType {
    // 名前(キャスト時間, 色, 手元追従するか)
    GRAM_DEMOLITION(10, new Vector3f(0.2F, 0.9F, 1.0F), true),

    // ■ 追加: エア・バレット (0.75秒, 白, 設置型=false)
    AIR_BULLET(30, new Vector3f(0.9F, 0.9F, 1.0F), false),

    MIST_DISPERSION(20, new Vector3f(1.0F, 0.2F, 0.2F), false),
    REGROWTH(40, new Vector3f(0.2F, 1.0F, 0.5F), false),
    MATERIAL_BURST(100, new Vector3f(1.0F, 0.6F, 0.0F), false);

    public final int castTime;
    public final Vector3f color;
    public final boolean isAttachedToCaster;

    MagicSpellType(int castTime, Vector3f color, boolean isAttachedToCaster) {
        this.castTime = castTime;
        this.color = color;
        this.isAttachedToCaster = isAttachedToCaster;
    }
}
