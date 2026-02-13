package com.COLLABOMOD.collabomod.learning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 外部AI連携の設定管理クラス。
 * config/collabomod_ai.json から設定を読み書きする。
 */
public class ExternalAIConfig {

    private static ExternalAIConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- 設定フィールド ---
    public boolean enabled = false;
    public String apiUrl = "https://api.openai.com/v1/chat/completions";
    public String apiKey = "";
    public String model = "gpt-4o-mini";
    public int timeoutSeconds = 30;
    public int maxCacheSize = 256;

    private ExternalAIConfig() {
    }

    public static ExternalAIConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExternalAIConfig();
        }
        return INSTANCE;
    }

    /**
     * 設定ファイルを読み込む。ファイルが存在しない場合はデフォルト値で新規作成する。
     */
    public static void load() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("collabomod_ai.json");
        File file = configPath.toFile();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                INSTANCE = GSON.fromJson(reader, ExternalAIConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ExternalAIConfig();
                }
                // Gemini API使用時はタイムアウトが短すぎると使い物にならない
                if (INSTANCE.apiUrl != null && INSTANCE.apiUrl.contains("generativelanguage.googleapis.com")
                        && INSTANCE.timeoutSeconds < 60) {
                    System.out.println("[External AI] Gemini API detected. Adjusting timeout: "
                            + INSTANCE.timeoutSeconds + "s -> 60s");
                    INSTANCE.timeoutSeconds = 60;
                }
                System.out.println("[External AI] Config loaded. Enabled: " + INSTANCE.enabled);
            } catch (IOException e) {
                System.err.println("[External AI] Failed to load config: " + e.getMessage());
                INSTANCE = new ExternalAIConfig();
            }
        } else {
            // デフォルト設定ファイルを生成
            INSTANCE = new ExternalAIConfig();
            save();
            System.out.println("[External AI] Default config generated at: " + configPath);
        }
    }

    /**
     * 現在の設定をファイルに保存する。
     */
    public static void save() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve("collabomod_ai.json");
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(getInstance(), writer);
        } catch (IOException e) {
            System.err.println("[External AI] Failed to save config: " + e.getMessage());
        }
    }

    /**
     * 外部AI連携が有効かつAPIキーが設定されているかを返す。
     */
    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }
}
