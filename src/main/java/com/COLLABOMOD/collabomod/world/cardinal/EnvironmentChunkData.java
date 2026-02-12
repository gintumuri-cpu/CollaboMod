package com.COLLABOMOD.collabomod.world.cardinal;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EnvironmentChunkData {

    // ローカル座標(Long) -> 値
    private final Map<Long, Float> temperatureMap = new HashMap<>();
    private final Map<Long, Float> psionDensityMap = new HashMap<>();
    private final Map<Long, Integer> magicHashMap = new HashMap<>();
    private final Map<Long, Float> entropyMap = new HashMap<>();

    private boolean isDirty = false;

    public EnvironmentChunkData() {
    }

    // 時間経過処理
    public void tick() {
        boolean changed = false;

        // 1. 温度の減衰 (300Kに近づく)
        if (!temperatureMap.isEmpty()) {
            Iterator<Map.Entry<Long, Float>> it = temperatureMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Float> entry = it.next();
                float current = entry.getValue();
                float diff = current - 300.0F;

                if (Math.abs(diff) < 1.0F) {
                    it.remove();
                    changed = true;
                } else {
                    float next = current - diff * 0.01F; // 1%ずつ常温へ
                    entry.setValue(next);
                    changed = true;
                }
            }
        }

        // 2. サイオン濃度の減衰
        if (!psionDensityMap.isEmpty()) {
            Iterator<Map.Entry<Long, Float>> it = psionDensityMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Float> entry = it.next();
                float val = entry.getValue();
                if (val < 0.1F) {
                    it.remove();
                    changed = true;
                } else {
                    entry.setValue(val * 0.99F);
                    changed = true;
                }
            }
        }

        // 3. エントロピーの解消
        if (!entropyMap.isEmpty()) {
            Iterator<Map.Entry<Long, Float>> it = entropyMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Float> entry = it.next();
                float val = entry.getValue();
                if (val < 0.1F) {
                    it.remove();
                    changed = true;
                } else {
                    entry.setValue(val * 0.98F);
                    changed = true;
                }
            }
        }

        if (changed) {
            markDirty();
        }
    }

    public Map<Long, Float> getAllTemperatures() {
        return temperatureMap;
    }

    public Map<Long, Float> getAllPsionDensities() {
        return psionDensityMap;
    }

    // --- 温度操作 ---
    public float getTemperature(BlockPos pos) {
        long key = getLocalKey(pos);
        return temperatureMap.getOrDefault(key, 300.0F);
    }

    public void setTemperature(BlockPos pos, float temp) {
        long key = getLocalKey(pos);
        setTemperatureDirect(key, temp);
    }

    // ■ 追加: パケット受信時用 (Raw Key Setter)
    public void setTemperatureDirect(long key, float temp) {
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
        return psionDensityMap.getOrDefault(key, 0.0F);
    }

    public void setPsionDensity(BlockPos pos, float density) {
        long key = getLocalKey(pos);
        setPsionDensityDirect(key, density);
    }

    // ■ 追加: パケット受信時用
    public void setPsionDensityDirect(long key, float density) {
        if (density <= 0.001F) {
            if (psionDensityMap.remove(key) != null) markDirty();
        } else {
            psionDensityMap.put(key, density);
            markDirty();
        }
    }

    // --- 魔法ハッシュ操作 ---
    public int getMagicHash(BlockPos pos) {
        long key = getLocalKey(pos);
        return magicHashMap.getOrDefault(key, 0);
    }

    public void setMagicHash(BlockPos pos, int hash) {
        long key = getLocalKey(pos);
        if (hash == 0) {
            if (magicHashMap.remove(key) != null) markDirty();
        } else {
            magicHashMap.put(key, hash);
            markDirty();
        }
    }

    // --- エントロピー操作 ---
    public float getEntropy(BlockPos pos) {
        long key = getLocalKey(pos);
        return entropyMap.getOrDefault(key, 0.0F);
    }

    public void setEntropy(BlockPos pos, float entropy) {
        long key = getLocalKey(pos);
        setEntropyDirect(key, entropy);
    }

    // ■ 追加: パケット受信時用
    public void setEntropyDirect(long key, float entropy) {
        if (entropy <= 0.001F) {
            if (entropyMap.remove(key) != null) markDirty();
        } else {
            entropyMap.put(key, entropy);
            markDirty();
        }
    }

    private long getLocalKey(BlockPos pos) {
        // 下位4ビット(0-15)とY座標を使ってキー生成
        return BlockPos.asLong(pos.getX() & 0xF, pos.getY(), pos.getZ() & 0xF);
    }

    public void markDirty() {
        this.isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    // --- NBT Serialization ---
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        if (!temperatureMap.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Float> entry : temperatureMap.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("P", entry.getKey());
                entryTag.putFloat("V", entry.getValue());
                list.add(entryTag);
            }
            tag.put("TempMap", list);
        }

        if (!psionDensityMap.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Float> entry : psionDensityMap.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("P", entry.getKey());
                entryTag.putFloat("V", entry.getValue());
                list.add(entryTag);
            }
            tag.put("PsionMap", list);
        }

        if (!magicHashMap.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Integer> entry : magicHashMap.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("P", entry.getKey());
                entryTag.putInt("V", entry.getValue());
                list.add(entryTag);
            }
            tag.put("MagicMap", list);
        }

        if (!entropyMap.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Float> entry : entropyMap.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putLong("P", entry.getKey());
                entryTag.putFloat("V", entry.getValue());
                list.add(entryTag);
            }
            tag.put("EntropyMap", list);
        }

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

        magicHashMap.clear();
        if (tag.contains("MagicMap")) {
            ListTag list = tag.getList("MagicMap", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                magicHashMap.put(entry.getLong("P"), entry.getInt("V"));
            }
        }

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