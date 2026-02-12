package com.COLLABOMOD.collabomod.magic.command;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class ParticleCommand implements ICommand {
    private final float startTime;
    public final ResourceLocation particleId;
    public final int count;
    // 他にも shape, radius, velocity などのパラメータを追加可能

    public ParticleCommand(float startTime, ResourceLocation particleId, int count) {
        this.startTime = startTime;
        this.particleId = particleId;
        this.count = count;
    }

    public ParticleCommand(CompoundTag nbt) {
        this.startTime = nbt.getFloat("start");
        this.particleId = new ResourceLocation(nbt.getString("particle"));
        this.count = nbt.getInt("count");
    }

    @Override
    public float getStartTime() {
        return startTime;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", CommandType.getId(getType()).toString());
        nbt.putFloat("start", startTime);
        nbt.putString("particle", particleId.toString());
        nbt.putInt("count", count);
        return nbt;
    }

    @Override
    public CommandType<?> getType() {
        return CommandType.PARTICLE;
    }
}