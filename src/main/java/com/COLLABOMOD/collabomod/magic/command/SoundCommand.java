package com.COLLABOMOD.collabomod.magic.command;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

public class SoundCommand implements ICommand {
    private final float startTime;
    public final ResourceLocation soundId;
    public final float volume;
    public final float pitch;

    public SoundCommand(float startTime, ResourceLocation soundId, float volume, float pitch) {
        this.startTime = startTime;
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    public SoundCommand(CompoundTag nbt) {
        this.startTime = nbt.getFloat("start");
        this.soundId = new ResourceLocation(nbt.getString("sound"));
        this.volume = nbt.getFloat("volume");
        this.pitch = nbt.getFloat("pitch");
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
        nbt.putString("sound", soundId.toString());
        nbt.putFloat("volume", volume);
        nbt.putFloat("pitch", pitch);
        return nbt;
    }

    @Override
    public CommandType<?> getType() {
        return CommandType.SOUND;
    }
}