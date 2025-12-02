package com.COLLABOMOD.collabomod.capability;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class MagicStats {
    private int currentPsion;
    private int maxPsion;
    private int calculationArea;
    private int mentalLoad;
    private boolean isInitialized;

    public MagicStats() {
        this.currentPsion = 100;
        this.maxPsion = 100;
        this.calculationArea = 100;
        this.mentalLoad = 0;
        this.isInitialized = false;
    }
    public void generateTalent(UUID uuid) {
        if (!isInitialized) {
            // UUIDのハッシュ値を利用して擬似ランダム生成
            long seed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
            java.util.Random rand = new java.util.Random(seed);

            // 例: 演算規模は 100 ～ 500 の間
            this.calculationArea = 100 + rand.nextInt(401);
            // 例: サイオン保有量は 500 ～ 2000
            this.maxPsion = 500 + rand.nextInt(1501);
            this.currentPsion = this.maxPsion;

            this.isInitialized = true;
        }
    }

    public int getCurrentPsion() { return currentPsion; }
    public void setCurrentPsion(int val) { this.currentPsion = Math.max(0, Math.min(val, maxPsion)); }

    public int getMaxPsion() { return maxPsion; }
    public void setMaxPsion(int val) { this.maxPsion = val; }

    public int getCalculationArea() { return calculationArea; }

    public int getMentalLoad() { return mentalLoad; }
    public void setMentalLoad(int val) { this.mentalLoad = Math.max(0, val); }

    public void addMentalLoad(int val) {
        this.mentalLoad += val;
    }

    // データの保存（セーブデータへの書き込み）
    public void saveNBT(CompoundTag nbt) {
        nbt.putInt("CurrentPsion", currentPsion);
        nbt.putInt("MaxPsion", maxPsion);
        nbt.putInt("CalculationArea", calculationArea);
        nbt.putInt("MentalLoad", mentalLoad);
        nbt.putBoolean("IsInitialized", isInitialized);
    }

    // データの読み込み
    public void loadNBT(CompoundTag nbt) {
        currentPsion = nbt.getInt("CurrentPsion");
        maxPsion = nbt.getInt("MaxPsion");
        calculationArea = nbt.getInt("CalculationArea");
        mentalLoad = nbt.getInt("MentalLoad");
        isInitialized = nbt.getBoolean("IsInitialized");
    }

    // クライアント同期用などにデータをコピーするメソッド
    public void copyFrom(MagicStats source) {
        this.currentPsion = source.currentPsion;
        this.maxPsion = source.maxPsion;
        this.calculationArea = source.calculationArea;
        this.mentalLoad = source.mentalLoad;
        this.isInitialized = source.isInitialized;
    }

    public void syncClient(int current, int max, int area, int load) {
        this.currentPsion = current;
        this.maxPsion = max;
        this.calculationArea = area;
        this.mentalLoad = load;
    }
}
