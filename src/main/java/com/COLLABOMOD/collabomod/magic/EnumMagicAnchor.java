package com.COLLABOMOD.collabomod.magic;

public enum EnumMagicAnchor {
    WORLD_FIXED,        // 空間固定（設置型・トラップ・爆心地）
    CASTER_ANCHORED,    // 術者追従（シールド・身体強化・詠唱中）
    TARGET_ANCHORED,    // ターゲット追従（デバフ・拘束・ロックオン）
    HARDPOINT_ANCHORED, // 銃口/右手固定（射撃魔法）
    RANDOM_AIR          // 術者周囲の空中浮遊（ファンネル・放出系）
}