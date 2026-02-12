package com.COLLABOMOD.collabomod.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProceduralTextureManager {

    private static final ProceduralTextureManager INSTANCE = new ProceduralTextureManager();
    private final Map<String, ResourceLocation> textureCache = new HashMap<>();

    public static ProceduralTextureManager getInstance() {
        return INSTANCE;
    }

    /**
     * パラメータに基づいてテクスチャを生成（またはキャッシュから取得）し、そのResourceLocationを返す
     */
    public ResourceLocation getTexture(long seed, String type, int color) {
        String key = type + "_" + seed + "_" + color;
        
        if (textureCache.containsKey(key)) {
            return textureCache.get(key);
        }

        NativeImage image = generateImage(seed, type, color);
        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation location = Minecraft.getInstance().getTextureManager().register("collabomod_procedural/" + key, texture);
        
        textureCache.put(key, location);
        return location;
    }

    private NativeImage generateImage(long seed, String type, int color) {
        int width = 64;
        int height = 64;
        NativeImage image = new NativeImage(width, height, true);
        Random rand = new Random(seed);

        // 色成分の抽出 (ARGB)
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float noise = 0.0f;
                
                // 正規化座標
                float u = (float)x / width;
                float v = (float)y / height;

                if ("NOISE".equals(type)) {
                    noise = simpleNoise(u * 10 + rand.nextFloat() * 100, v * 10 + rand.nextFloat() * 100);
                } else if ("STRIPE".equals(type)) {
                    noise = Mth.sin((u + v) * 20.0f + rand.nextFloat() * 10.0f) * 0.5f + 0.5f;
                } else if ("CELL".equals(type)) {
                    // 簡易的なセルラーノイズっぽいもの
                    float dist = 1.0f;
                    for(int i=0; i<5; i++) {
                        float px = rand.nextFloat();
                        float py = rand.nextFloat();
                        float d = (float)Math.sqrt((u-px)*(u-px) + (v-py)*(v-py));
                        dist = Math.min(dist, d);
                    }
                    noise = 1.0f - dist * 3.0f;
                } else {
                    // Default: Random static
                    noise = rand.nextFloat();
                }

                noise = Mth.clamp(noise, 0.0f, 1.0f);

                // ノイズをアルファ値や明るさに適用
                int pixelA = (int)(a * noise);
                int pixelR = (int)(r * (0.5f + noise * 0.5f));
                int pixelG = (int)(g * (0.5f + noise * 0.5f));
                int pixelB = (int)(b * (0.5f + noise * 0.5f));

                // NativeImageは ABGR 形式でセットする場合が多いが、setPixelRGBAはR,G,B,Aの順でintを期待する実装もある
                // MinecraftのNativeImage.combineは (a << 24 | b << 16 | g << 8 | r) の形式 (ABGR)
                // しかし setPixelRGBA メソッドではなく、ループでセットするなら combine を使うのが一般的
                
                // ここでは NativeImage.combine(alpha, blue, green, red) を使用
                int col = NativeImage.combine(pixelA, pixelB, pixelG, pixelR);
                image.setPixelRGBA(x, y, col);
            }
        }
        return image;
    }

    // 簡易的な擬似ノイズ
    private float simpleNoise(float x, float y) {
        return (Mth.sin(x) * Mth.cos(y)) * 0.5f + 0.5f;
    }
}