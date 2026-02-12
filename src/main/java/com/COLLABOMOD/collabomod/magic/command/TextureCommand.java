package com.COLLABOMOD.collabomod.magic.command;

import net.minecraft.nbt.CompoundTag;

public class TextureCommand implements ICommand {
    private final float startTime;
    public final String type; // "NOISE", "STRIPE", "CELL"
    public final long seed;
    public final int color; // ARGB

    public TextureCommand(float startTime, String type, long seed, int color) {
        this.startTime = startTime;
        this.type = type;
        this.seed = seed;
        this.color = color;
    }

    public TextureCommand(CompoundTag nbt) {
        this.startTime = nbt.getFloat("start");
        this.type = nbt.getString("type");
        this.seed = nbt.getLong("seed");
        this.color = nbt.getInt("color");
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
        nbt.putString("type", type);
        nbt.putLong("seed", seed);
        nbt.putInt("color", color);
        return nbt;
    }

    @Override
    public CommandType<?> getType() {
        return CommandType.TEXTURE;
    }
}