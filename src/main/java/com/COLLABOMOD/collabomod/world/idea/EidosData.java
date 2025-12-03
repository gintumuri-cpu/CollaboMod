package com.COLLABOMOD.collabomod.world.idea;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public class EidosData {
    // 座標情報
    private final BlockPos position;
    // 状態（ブロックならBlockState、エンティティならNBT）
    private final BlockState blockState;
    private final CompoundTag entityData;
    // 記録された時刻（ワールド時間）
    private final long timestamp;

    // ブロック用コンストラクタ
    public EidosData(BlockPos pos, BlockState state, long time) {
        this.position = pos;
        this.blockState = state;
        this.entityData = null;
        this.timestamp = time;
    }

    // エンティティ用コンストラクタ
    public EidosData(LivingEntity entity, long time) {
        this.position = entity.blockPosition();
        this.blockState = null;
        this.entityData = new CompoundTag();
        entity.saveWithoutId(this.entityData); // 現在のエンティティ情報をNBTとして保存
        this.timestamp = time;
    }

    public BlockPos getPosition() { return position; }
    public BlockState getBlockState() { return blockState; }
    public CompoundTag getEntityData() { return entityData; }
    public long getTimestamp() { return timestamp; }
}
