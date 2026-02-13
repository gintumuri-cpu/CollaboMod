package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * スクリプトハッシュをキーに VisualMetadata をキャッシュする。
 * NBT形式でファイルに永続化し、再起動後もキャッシュを保持する。
 */
public class AIInferenceCache {

    private static final AIInferenceCache INSTANCE = new AIInferenceCache();
    private static final String CACHE_FILENAME = "collabomod_ai_cache.nbt";

    private final ConcurrentHashMap<Integer, CompoundTag> cache = new ConcurrentHashMap<>();

    private AIInferenceCache() {
    }

    public static AIInferenceCache getInstance() {
        return INSTANCE;
    }

    public VisualMetadata get(int scriptHash) {
        CompoundTag tag = cache.get(scriptHash);
        if (tag != null) {
            System.out.println("[External AI] Cache HIT for hash: " + scriptHash);
            return VisualMetadata.fromNBT(tag);
        }
        return null;
    }

    public void put(int scriptHash, VisualMetadata meta) {
        if (meta == null)
            return;

        int maxSize = ExternalAIConfig.getInstance().maxCacheSize;
        while (cache.size() >= maxSize && !cache.isEmpty()) {
            Integer oldestKey = cache.keys().nextElement();
            cache.remove(oldestKey);
        }

        cache.put(scriptHash, meta.toNBT());
        System.out.println("[External AI] Cached result for hash: " + scriptHash + " (total: " + cache.size() + ")");
    }

    /**
     * キャッシュをNBTファイルに永続化する。
     */
    public void save() {
        Path cachePath = FMLPaths.CONFIGDIR.get().resolve(CACHE_FILENAME);
        try {
            CompoundTag root = new CompoundTag();
            cache.forEach((hash, tag) -> root.put(String.valueOf(hash), tag));
            root.putInt("_size", cache.size());
            NbtIo.writeCompressed(root, cachePath.toFile());
            System.out.println("[External AI] Cache saved to disk. Entries: " + cache.size());
        } catch (Exception e) {
            System.err.println("[External AI] Failed to save cache: " + e.getMessage());
        }
    }

    /**
     * NBTファイルからキャッシュを復元する。
     */
    public void load() {
        Path cachePath = FMLPaths.CONFIGDIR.get().resolve(CACHE_FILENAME);
        File file = cachePath.toFile();
        if (!file.exists()) {
            System.out.println("[External AI] No cache file found. Starting with empty cache.");
            return;
        }

        try {
            CompoundTag root = NbtIo.readCompressed(file);
            cache.clear();
            int loaded = 0;
            for (String key : root.getAllKeys()) {
                if (key.startsWith("_"))
                    continue;
                try {
                    int hash = Integer.parseInt(key);
                    cache.put(hash, root.getCompound(key));
                    loaded++;
                } catch (NumberFormatException ignored) {
                }
            }
            System.out.println("[External AI] Cache loaded from disk. Entries: " + loaded);
        } catch (Exception e) {
            System.err.println("[External AI] Failed to load cache: " + e.getMessage());
        }
    }

    public void clear() {
        cache.clear();
        System.out.println("[External AI] Cache cleared.");
    }

    public int size() {
        return cache.size();
    }
}
