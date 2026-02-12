package com.COLLABOMOD.collabomod.magic.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 描画命令の基底インターフェース
 */
public interface ICommand {
    /**
     * この命令が実行されるべき時間（タイムラインの開始を0.0とする）
     */
    float getStartTime();

    /**
     * 命令をNBTにシリアライズする
     */
    CompoundTag serializeNBT();

    /**
     * この命令のタイプを取得する
     */
    CommandType<?> getType();
}