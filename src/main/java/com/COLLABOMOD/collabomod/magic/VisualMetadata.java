package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;

public class VisualMetadata {
    public EnumMagicShape shape = EnumMagicShape.RING;
    public EnumMagicAnchor anchorType = EnumMagicAnchor.WORLD_FIXED; // ■ 追加: アンカータイプ

    public int priority = 0;
    public Vector3f mainColor = new Vector3f(1.0F, 1.0F, 1.0F);
    public Vector3f subColor = new Vector3f(0.0F, 0.5F, 1.0F);

    public float scale = 1.0F;
    public float density = 1.0F;
    public float rotationSpeed = 1.0F;

    // ■ 追加: プロシージャル生成用パラメータ
    public boolean isWavy = false;       // 振動属性など (波打つ)
    public boolean isSpiky = false;      // 拡散・攻撃など (トゲトゲ)
    public int layerCount = 1;           // レイヤー数 (威力に応じて増加)

    public boolean hasLightning = false;
    public boolean isSolid = false;
    public String rendererID = "default";

    public VisualMetadata() {}

    public VisualMetadata(String rendererID, int priority, Vector3f color, float scale) {
        this.rendererID = rendererID;
        this.priority = priority;
        this.mainColor = color;
        this.scale = scale;
    }

    public void merge(VisualMetadata other) {
        if (other.priority > this.priority) {
            this.rendererID = other.rendererID;
            this.shape = other.shape;
            this.priority = other.priority;
            this.scale = other.scale;
            this.hasLightning = other.hasLightning;
            // 新規パラメータのマージ
            this.anchorType = other.anchorType;
            this.isWavy = other.isWavy || this.isWavy;
            this.isSpiky = other.isSpiky || this.isSpiky;
            this.layerCount = Math.max(this.layerCount, other.layerCount);
        }
        this.mainColor.add(other.mainColor);
        this.mainColor.mul(0.5F);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("RendererID", rendererID);
        tag.putInt("Shape", shape.ordinal());
        tag.putInt("Anchor", anchorType.ordinal()); // 保存
        tag.putFloat("ColorR", mainColor.x());
        tag.putFloat("ColorG", mainColor.y());
        tag.putFloat("ColorB", mainColor.z());
        tag.putFloat("Scale", scale);
        tag.putBoolean("IsWavy", isWavy);
        tag.putBoolean("IsSpiky", isSpiky);
        tag.putInt("Layers", layerCount);
        return tag;
    }

    public static VisualMetadata fromNBT(CompoundTag tag) {
        VisualMetadata meta = new VisualMetadata();
        if (tag.contains("RendererID")) meta.rendererID = tag.getString("RendererID");
        if (tag.contains("Shape")) meta.shape = EnumMagicShape.values()[tag.getInt("Shape")];
        if (tag.contains("Anchor")) meta.anchorType = EnumMagicAnchor.values()[tag.getInt("Anchor")];
        if (tag.contains("ColorR")) {
            meta.mainColor = new Vector3f(tag.getFloat("ColorR"), tag.getFloat("ColorG"), tag.getFloat("ColorB"));
        }
        if (tag.contains("Scale")) meta.scale = tag.getFloat("Scale");
        if (tag.contains("IsWavy")) meta.isWavy = tag.getBoolean("IsWavy");
        if (tag.contains("IsSpiky")) meta.isSpiky = tag.getBoolean("IsSpiky");
        if (tag.contains("Layers")) meta.layerCount = tag.getInt("Layers");
        return meta;
    }
}