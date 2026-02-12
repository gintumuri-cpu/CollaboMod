package com.COLLABOMOD.collabomod.magic;

import net.minecraft.nbt.CompoundTag;

public class PhysicsMetadata {
    // 基本物理量
    public float temperature = 300.0F; // 温度 (ケルビン)
    public float mass = 1.0F;          // 質量
    public float velocity = 0.0F;      // 初速
    public float hardness = 0.0F;      // 硬度 (シールド強度や貫通力)
    public float energy = 0.0F;        // 純粋エネルギー量 (ダメージ計算用)
    public float areaOfEffect = 0.0F;  // 効果範囲

    // 挙動フラグ
    public boolean isSolid = false;    // 物理的な当たり判定を持つか
    public boolean isHoming = false;   // 誘導するか
    public boolean gravity = false;    // 重力の影響を受けるか

    // 力の性質
    public EnumForceType forceType = EnumForceType.NONE;

    public enum EnumForceType {
        NONE,
        RADIAL,      // 放射状 (爆発)
        DIRECTIONAL, // 指向性 (射撃)
        FIELD        // 領域 (設置)
    }

    public PhysicsMetadata() {}

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Temp", temperature);
        tag.putFloat("Mass", mass);
        tag.putFloat("Vel", velocity);
        tag.putFloat("Hard", hardness);
        tag.putFloat("Energy", energy);
        tag.putFloat("AoE", areaOfEffect);
        tag.putBoolean("Solid", isSolid);
        tag.putBoolean("Homing", isHoming);
        tag.putBoolean("Grav", gravity);
        tag.putInt("FType", forceType.ordinal());
        return tag;
    }

    public static PhysicsMetadata fromNBT(CompoundTag tag) {
        PhysicsMetadata meta = new PhysicsMetadata();
        if (tag.contains("Temp")) meta.temperature = tag.getFloat("Temp");
        if (tag.contains("Mass")) meta.mass = tag.getFloat("Mass");
        if (tag.contains("Vel")) meta.velocity = tag.getFloat("Vel");
        if (tag.contains("Hard")) meta.hardness = tag.getFloat("Hard");
        if (tag.contains("Energy")) meta.energy = tag.getFloat("Energy");
        if (tag.contains("AoE")) meta.areaOfEffect = tag.getFloat("AoE");
        if (tag.contains("Solid")) meta.isSolid = tag.getBoolean("Solid");
        if (tag.contains("Homing")) meta.isHoming = tag.getBoolean("Homing");
        if (tag.contains("Grav")) meta.gravity = tag.getBoolean("Grav");
        if (tag.contains("FType")) meta.forceType = EnumForceType.values()[tag.getInt("FType")];
        return meta;
    }
}