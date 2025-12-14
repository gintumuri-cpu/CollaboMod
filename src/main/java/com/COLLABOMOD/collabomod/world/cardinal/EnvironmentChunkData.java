package com.COLLABOMOD.collabomod.world.cardinal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentChunkData {

    private final Map<Long, Float> temperatureMap = new HashMap<>();

    private final Map<Long, Float> psionDensityMap = new HashMap<>();

    private final Map<Long, Integer> magicHashMap = new HashMap<>();
    private final Map<Long, Float> entropyMap = new HashMap<>();

    private boolean isDirty = false;

    public EnvironmentChunkData() {
    }

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

    public int getMagicHash(BlockPos pos) {
        return magicHashMap.getOrDefault(getLocalKey(pos), 0);
    }
    public void setMagicHash(BlockPos pos, int hash) {
        long key = getLocalKey(pos);
        if (hash == 0) magicHashMap.remove(key);
        else magicHashMap.put(key, hash);
    }

    public float getEntropy(BlockPos pos) {
        return entropyMap.getOrDefault(getLocalKey(pos), 0.0F);
    }

    public void setEntropy(BlockPos pos, float entropy) {
        long key = getLocalKey(pos);
        if (entropy <= 0.0F) entropyMap.remove(key);
        else entropyMap.put(key, entropy);
    }

    public void directSetHash(long key, int hash) {
        if (hash == 0) magicHashMap.remove(key);
        else magicHashMap.put(key, hash);
    }

    public void directSetEntropy(long key, float entropy) {
        if (entropy <= 0.0F) entropyMap.remove(key);
        else entropyMap.put(key, entropy);
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

    public void directSetPsion(long key, float density) {
        if (Math.abs(density - 100.0F) < 1.0F) {
            psionDensityMap.remove(key);
        } else {
            psionDensityMap.put(key, density);
        }
    }

    // ローカル座標キーの生成 (0-15, y, 0-15)
    private long getLocalKey(BlockPos pos) {
        return BlockPos.asLong(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
    }

    // --- 同期管理 ---
    public boolean isDirty() {
        return isDirty;
    }

    public void clearDirty() {
        isDirty = false;
    }

    private void markDirty() {
        isDirty = true;
    }

    public Map<Long, Float> getAllTemperatures() {
        return temperatureMap;
    }

    public Map<Long, Float> getAllPsionDensities() {
        return psionDensityMap;
    }

    // --- NBT保存/読み込み ---
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 1. 温度 (Temperature)
        ListTag tempList = new ListTag();
        temperatureMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putFloat("V", val);
            tempList.add(entry);
        });
        tag.put("TempMap", tempList);

        // 2. サイオン濃度 (Psion Density)
        ListTag psionList = new ListTag();
        psionDensityMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putFloat("V", val);
            psionList.add(entry);
        });
        tag.put("PsionMap", psionList);

        // ■ 追加: 3. 魔法ハッシュ (Magic Hash)
        ListTag magicList = new ListTag();
        magicHashMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putInt("V", val); // Hashはint
            magicList.add(entry);
        });
        tag.put("MagicMap", magicList);

        // ■ 追加: エントロピー (Entropy)
        ListTag entropyList = new ListTag();
        entropyMap.forEach((key, val) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("P", key);
            entry.putFloat("V", val); // Entropyはfloat
            entropyList.add(entry);
        });
        tag.put("EntropyMap", entropyList);

        return tag;
    }

    // --- NBT読み込み ---
    public void deserializeNBT(CompoundTag tag) {
        // 1. 温度
        temperatureMap.clear();
        if (tag.contains("TempMap")) {
            ListTag list = tag.getList("TempMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                temperatureMap.put(entry.getLong("P"), entry.getFloat("V"));
            }
        }

        // 2. サイオン濃度
        psionDensityMap.clear();
        if (tag.contains("PsionMap")) {
            ListTag list = tag.getList("PsionMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                psionDensityMap.put(entry.getLong("P"), entry.getFloat("V"));
            }
        }

        // ■ 追加: 3. 魔法ハッシュ
        magicHashMap.clear();
        if (tag.contains("MagicMap")) {
            ListTag list = tag.getList("MagicMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                magicHashMap.put(entry.getLong("P"), entry.getInt("V"));
            }
        }

        // ■ 追加: 4. エントロピー
        entropyMap.clear();
        if (tag.contains("EntropyMap")) {
            ListTag list = tag.getList("EntropyMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                entropyMap.put(entry.getLong("P"), entry.getFloat("V"));
            }
        }

    }
}