package com.COLLABOMOD.collabomod.magic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpellContext {
    // ■ 基本パラメータ (部品によって加算・乗算される)
    public float power = 1.0F;       // 威力
    public float range = 10.0F;      // 射程・範囲
    public float speed = 1.0F;       // 弾速・発動速度
    public int cost = 0;             // 消費サイオン
    public int castTime = 0;         // 詠唱時間 (tick)

    // ■ 魔法の分類フラグ
    public EnumAction action = EnumAction.NONE;          // 作用 (撃つ、爆発、治す...)
    public EnumAttribute attribute = EnumAttribute.NONE; // 属性 (空気、振動、分解...)

    // ■ 実行時の環境情報
    public Level level;
    public LivingEntity caster;
    public Vec3 origin;    // 発動地点
    public Vec3 direction; // 向いている方向
    public float rotX;     // Pitch
    public float rotY;     // Yaw

    public LivingEntity target = null;

    // コンストラクタ
    public SpellContext(Level level, LivingEntity caster) {
        this.level = level;
        this.caster = caster;
        this.origin = caster.position();
        this.direction = caster.getLookAngle();
        this.rotX = caster.getXRot();
        this.rotY = caster.getYRot();
    }

    // 座標などの強制上書き用（遠隔発動などで使う）
    public void setLocation(Vec3 pos, float pitch, float yaw) {
        this.origin = pos;
        this.rotX = pitch;
        this.rotY = yaw;
        // directionも更新した方が良いが、今回は簡易的にそのまま
    }

    // --- 定義用Enum ---
    public enum EnumAction {
        NONE,
        PROJECTILE, // 射出
        EXPLOSION,  // 爆発
        RESTORE     // 修復
    }

    public enum EnumAttribute {
        NONE,
        AIR,           // 空気
        OSCILLATION,   // 振動
        DECOMPOSITION, // 分解
        MASS_ENERGY    // 質量エネルギー
    }
}
