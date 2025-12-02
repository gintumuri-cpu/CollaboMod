package com.COLLABOMOD.collabomod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MagicStatsProvider implements ICapabilitySerializable<CompoundTag> {
    // キャパビリティの登録トークン
    public static final Capability<MagicStats> PLAYER_MAGIC_STATS = CapabilityManager.get(new CapabilityToken<>(){});

    private MagicStats magicStats = null;
    private final LazyOptional<MagicStats> optional = LazyOptional.of(this::createMagicStats);

    private MagicStats createMagicStats() {
        if (this.magicStats == null) {
            this.magicStats = new MagicStats();
        }
        return this.magicStats;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_MAGIC_STATS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        createMagicStats().saveNBT(nbt);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createMagicStats().loadNBT(nbt);
    }
}
