package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;

public class VisualMetadata {
    public EnumMagicShape shape = EnumMagicShape.RING;
    public int priority = 0;
    public Vector3f mainColor = new Vector3f(1.0F, 1.0F, 1.0F);
    public Vector3f subColor = new Vector3f(0.0F, 0.5F, 1.0F);
    public float scale = 1.0F;
    public float density = 1.0F;
    public float rotationSpeed = 1.0F;
    public boolean hasLightning = false;
    public boolean isSolid = false;
    public String rendererID = "default";

    public VisualMetadata() {}

    public VisualMetadata(String rendererID, int priority, Vector3f color, float scale) {
        this.rendererID = rendererID;
        this.priority = priority;
        this.mainColor = color;
        this.scale = scale;
        // IDから形状を推測（簡易互換）
        if (rendererID.equals("material_burst") || rendererID.equals("explosion_sphere")) {
            this.shape = EnumMagicShape.SPHERE;
        } else {
            this.shape = EnumMagicShape.RING;
        }
    }

    public void merge(VisualMetadata other) {
        if (other.priority > this.priority) {
            this.rendererID = other.rendererID;
            this.shape = other.shape;
            this.priority = other.priority;
            this.scale = other.scale;
            this.hasLightning = other.hasLightning;
        }
        this.mainColor.add(other.mainColor);
        this.mainColor.mul(0.5F);
        this.subColor.add(other.subColor);
        this.subColor.mul(0.5F);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("RendererID", rendererID);
        tag.putInt("Shape", shape.ordinal());
        tag.putFloat("ColorR", mainColor.x());
        tag.putFloat("ColorG", mainColor.y());
        tag.putFloat("ColorB", mainColor.z());
        tag.putFloat("Scale", scale);
        tag.putBoolean("Lightning", hasLightning);
        return tag;
    }

    // ■■■ 追加: NBTからの読み込み ■■■
    public static VisualMetadata fromNBT(CompoundTag tag) {
        VisualMetadata meta = new VisualMetadata();
        if (tag.contains("RendererID")) meta.rendererID = tag.getString("RendererID");
        if (tag.contains("Shape")) meta.shape = EnumMagicShape.values()[tag.getInt("Shape")];
        if (tag.contains("ColorR")) {
            meta.mainColor = new Vector3f(
                    tag.getFloat("ColorR"),
                    tag.getFloat("ColorG"),
                    tag.getFloat("ColorB")
            );
        }
        if (tag.contains("Scale")) meta.scale = tag.getFloat("Scale");
        if (tag.contains("Lightning")) meta.hasLightning = tag.getBoolean("Lightning");
        return meta;
    }
}