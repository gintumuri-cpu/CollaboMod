package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;

public class VisualMetadata {
    public EnumMagicShape shape = EnumMagicShape.RING;
    public EnumMagicAnchor anchorType = EnumMagicAnchor.WORLD_FIXED;
    public int priority = 0;
    public Vector3f mainColor = new Vector3f(1.0F, 1.0F, 1.0F);
    public Vector3f subColor = new Vector3f(0.0F, 0.5F, 1.0F);

    public float scale = 1.0F;
    public float density = 1.0F;
    public float rotationSpeed = 1.0F;
    public float[] rawVector = new float[5];

    // ■ 追加: プロシージャル生成用パラメータ
    public boolean isWavy = false;       // 振動属性など (波打つ)
    public boolean isSpiky = false;      // 拡散・攻撃など (トゲトゲ)
    public int layerCount = 1;           // レイヤー数 (威力に応じて増加)

    public boolean hasLightning = false;
    public boolean isSolid = false;
    public String rendererID = "default";

    public int scriptHash = 0;
    public float normalizedIntensity = 0.0F;
    public float entropy = 0.0F;

    public EnumMagicAnimation animationType = EnumMagicAnimation.EXPAND_FADE;
    public float timeSpeed = 1.0F;
    public List<VisualKeyframe> timeline = new ArrayList<>();

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

        tag.putInt("ScriptHash", scriptHash);
        tag.putFloat("NormIntensity", normalizedIntensity);
        tag.putFloat("Entropy", entropy);
        tag.putInt("AnimType", animationType.ordinal());
        tag.putFloat("TimeSpeed", timeSpeed);

        // ■ 修正: AI推論ベクトル(rawVector)をNBTに保存
        if (rawVector != null && rawVector.length > 0) {
            // 1.18.2にはputFloatArrayがないため、ListTag<FloatTag>として保存する
            ListTag listTag = new ListTag();
            for (float f : rawVector) {
                listTag.add(FloatTag.valueOf(f));
            }
            tag.put("RawVec", listTag);
        }

        // タイムライン保存
        ListTag tList = new ListTag();
        for (VisualKeyframe k : timeline) {
            CompoundTag kTag = new CompoundTag();
            kTag.putFloat("T", k.timeStamp);
            kTag.putFloat("R", k.mainColor.x());
            kTag.putFloat("G", k.mainColor.y());
            kTag.putFloat("B", k.mainColor.z());
            kTag.putFloat("S", k.scale);
            kTag.putFloat("A", k.alpha);
            kTag.putInt("Shp", k.shape.ordinal());
            kTag.putBoolean("Spk", k.isSpiky);
            kTag.putBoolean("Wav", k.isWavy);
            kTag.putBoolean("Lit", k.hasLightning);
            // ■ 追加: キーフレームごとのrawVectorを保存
            if (k.rawVector != null && k.rawVector.length > 0) {
                ListTag rawVecList = new ListTag();
                for (float f : k.rawVector) {
                    rawVecList.add(FloatTag.valueOf(f));
                }
                kTag.put("RawVec", rawVecList);
            }
            tList.add(kTag);
        }
        tag.put("Timeline", tList);
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

        if (tag.contains("ScriptHash")) meta.scriptHash = tag.getInt("ScriptHash");
        if (tag.contains("NormIntensity")) meta.normalizedIntensity = tag.getFloat("NormIntensity");
        if (tag.contains("Entropy")) meta.entropy = tag.getFloat("Entropy");
        if (tag.contains("AnimType")) meta.animationType = EnumMagicAnimation.values()[tag.getInt("AnimType")];
        if (tag.contains("TimeSpeed")) meta.timeSpeed = tag.getFloat("TimeSpeed");

        // ■ 修正: NBTからAI推論ベクトル(rawVector)を読み込む
        // 1.18.2にはgetFloatArrayがないため、ListTag<FloatTag>から復元する
        if (tag.contains("RawVec", 9)) { // 9はListTagのID
            ListTag listTag = tag.getList("RawVec", 5); // 5はFloatTagのID
            meta.rawVector = new float[listTag.size()];
            for (int i = 0; i < listTag.size(); i++) {
                meta.rawVector[i] = listTag.getFloat(i);
            }
        }

        if (tag.contains("Timeline")) {
            ListTag tList = tag.getList("Timeline", 10);
            for (int i = 0; i < tList.size(); i++) {
                CompoundTag kTag = tList.getCompound(i);
                // ■ 追加: キーフレームごとのrawVectorを読み込み
                float[] rawVector = null;
                if (kTag.contains("RawVec", 9)) { // 9はListTagのID
                    ListTag rawVecList = kTag.getList("RawVec", 5); // 5はFloatTagのID
                    rawVector = new float[rawVecList.size()];
                    for (int j = 0; j < rawVecList.size(); j++) {
                        rawVector[j] = rawVecList.getFloat(j);
                    }
                }

                meta.timeline.add(new VisualKeyframe(
                        kTag.getFloat("T"),
                        new Vector3f(kTag.getFloat("R"), kTag.getFloat("G"), kTag.getFloat("B")),
                        new Vector3f(0,0,0), // subColorは簡易
                        kTag.getFloat("S"),
                        kTag.getFloat("A"),
                        EnumMagicShape.values()[kTag.getInt("Shp")],
                        kTag.getBoolean("Spk"),
                        kTag.getBoolean("Wav"),
                        kTag.getBoolean("Lit"),
                        rawVector // ■ 追加: 読み込んだrawVectorをコンストラクタに渡す
                ));
            }
        }
        return meta;
    }
}