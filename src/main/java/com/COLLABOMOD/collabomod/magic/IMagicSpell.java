package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.capability.MagicStats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IMagicSpell {
    // 魔法の基本コスト
    int getCost();

    // 魔法式の展開時間 (tick)
    int getCastTime();

    // ■ 1. 起動処理 (CADを使用した瞬間)
    // 戻り値: 成功したかどうか
    boolean initiate(Level level, Player player, MagicStats stats);

    // ■ 2. 発動処理 (魔法式がタイムラグを経て実行する処理)
    // origin: 魔法陣の位置, caster: 術者
    void execute(Level level, LivingEntity caster, Vec3 origin, float rotX, float rotY);
}
