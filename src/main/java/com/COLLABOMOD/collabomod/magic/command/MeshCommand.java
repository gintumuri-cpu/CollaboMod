package com.COLLABOMOD.collabomod.magic.command;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnimation;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;

public class MeshCommand implements ICommand {
    private final float startTime;
    public final EnumMagicShape shape;
    public final EnumMagicAnimation animation;
    public final Vector3f color;
    public final float scale;
    public final float duration; // アニメーションの長さ (0.0 - 1.0)

    public MeshCommand(float startTime, EnumMagicShape shape, EnumMagicAnimation animation, Vector3f color, float scale, float duration) {
        this.startTime = startTime;
        this.shape = shape;
        this.animation = animation;
        this.color = color;
        this.scale = scale;
        this.duration = duration;
    }

    public MeshCommand(CompoundTag nbt) {
        this.startTime = nbt.getFloat("start");
        this.shape = EnumMagicShape.valueOf(nbt.getString("shape"));
        this.animation = EnumMagicAnimation.valueOf(nbt.getString("anim"));
        this.color = new Vector3f(nbt.getFloat("r"), nbt.getFloat("g"), nbt.getFloat("b"));
        this.scale = nbt.getFloat("scale");
        this.duration = nbt.getFloat("duration");
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
        nbt.putString("shape", shape.name());
        nbt.putString("anim", animation.name());
        nbt.putFloat("r", color.x());
        nbt.putFloat("g", color.y());
        nbt.putFloat("b", color.z());
        nbt.putFloat("scale", scale);
        nbt.putFloat("duration", duration);
        return nbt;
    }

    @Override
    public CommandType<?> getType() {
        return CommandType.MESH;
    }
}