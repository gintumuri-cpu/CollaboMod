package com.COLLABOMOD.collabomod.world.cardinal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentChunkData {

    // 座標(Long) -> 温度(Float/ケルビン)
    // デフォルト: 300.0F (常温)
    private final Map<Long, Float> temperatureMap = new HashMap<>();

    // 座標(Long) -> サイオン濃度(Float)
    // デフォルト: 100.0F (標準)
    private final Map<Long, Float> psionDensityMap = new HashMap<>();

    // 同期が必要かどうかのフラグ
    private boolean isDirty = false;

    public EnvironmentChunkData() {}

    // --- 温度操作 ---
    public float getTemperature(BlockPos pos) {
        long key = getLocalKey(pos);
        return temperatureMap.getOrDefault(key, 300.0F);
    }

    public void setTemperature(BlockPos pos, float temp) {
        long key = getLocalKey(pos);
        // 常温に戻ったら削除（メモリ節約）
        if (Math.abs(temp - 300.0F) < 1.0F) {
            if (temperatureMap.remove(key) != null) markDirty();
        } else {
            temperatureMap.put(key, temp);
            markDirty();
        }
    }

    // --- サイオン濃度操作 ---
    public float getPsionDensity(BlockPos pos) {
        long key = getLocalKey(pos);
        return psionDensityMap.getOrDefault(key, 100.0F);
    }

    public void setPsionDensity(BlockPos pos, float density) {
        long key = getLocalKey(pos);
        if (Math.abs(density - 100.0F) < 1.0F) {
            if (psionDensityMap.remove(key) != null) markDirty();
        } else {
            psionDensityMap.put(key, density);
            markDirty();
        }
    }

    // クライアント同期用（直接セット）
    public void directSet(long key, float temp) {
        if (Math.abs(temp - 300.0F) < 1.0F) {
            temperatureMap.remove(key);
        } else {
            temperatureMap.put(key, temp);
        }
    }

    // ローカル座標キーの生成 (0-15, y, 0-15)
    private long getLocalKey(BlockPos pos) {
        return BlockPos.asLong(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
    }

    // --- 同期管理 ---
    public boolean isDirty() { return isDirty; }
    public void clearDirty() { isDirty = false; }
    private void markDirty() { isDirty = true; }

    public Map<Long, Float> getAllTemperatures() { return temperatureMap; }
    public Map<Long, Float> getAllPsionDensities() { return psionDensityMap; }

    // --- NBT保存/読み込み ---
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 温度
        ListTag tempList = new ListTag();
        temperatureMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putFloat("V", val);
            tempList.add(entry);
        });
        tag.put("TempMap", tempList);

        // サイオン濃度
        ListTag psionList = new ListTag();
        psionDensityMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putFloat("V", val);
            psionList.add(entry);
        });
        tag.put("PsionMap", psionList);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        temperatureMap.clear();
        if (tag.contains("TempMap")) {
            ListTag list = tag.getList("TempMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                temperatureMap.put(entry.getLong("P"), entry.getFloat("V"));
            }
        }

        psionDensityMap.clear();
        if (tag.contains("PsionMap")) {
            ListTag list = tag.getList("PsionMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                psionDensityMap.put(entry.getLong("P"), entry.getFloat("V"));
            }
        }
    }
}
