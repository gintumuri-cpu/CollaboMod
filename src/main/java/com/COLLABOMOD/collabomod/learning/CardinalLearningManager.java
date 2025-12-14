package com.COLLABOMOD.collabomod.learning;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken; // 追加
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type; // 追加
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator; // 追加
import java.util.List;
import java.util.stream.Collectors; // 追加

public class CardinalLearningManager {

    private static final CardinalLearningManager INSTANCE = new CardinalLearningManager();
    private final List<LearningData> dataset = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path savePath;

    private LearningData pendingData = null;

    private CardinalLearningManager() {
        this.savePath = FMLPaths.CONFIGDIR.get().resolve("collabomod_training_data.json");
        load();
    }

    public static CardinalLearningManager getInstance() {
        return INSTANCE;
    }

    public void registerInteraction(LearningData data) {
        this.pendingData = data;
    }

    public void rateLastInteraction(float score) {
        if (pendingData != null) {
            pendingData.developerScore = score;
            dataset.add(pendingData);
            save();
            System.out.println("[Cardinal AI] Data saved. Score: " + score + ", Total Data: " + dataset.size());
            pendingData = null;
        } else {
            System.out.println("[Cardinal AI] No pending interaction to rate.");
        }
    }

    // ■ 追加: k-近傍法検索 (似ている正解データを探す)
    public List<LearningData> findNearestNeighbors(float[] queryVector, int k) {
        // スコアが 0.0 より大きい（2点以上）のデータのみを「正解候補」とする
        // 1点 (-1.0) は「禁忌」として、ここでは検索対象に含めない（将来的に「避ける」学習に使う）
        List<LearningData> goodExamples = dataset.stream()
                .filter(d -> d.developerScore > 0.0f)
                .collect(Collectors.toList());

        if (goodExamples.isEmpty()) return new ArrayList<>();

        // 類似度ソート
        goodExamples.sort(Comparator.comparingDouble(d -> distance(d.attributeVector, queryVector)));

        return goodExamples.subList(0, Math.min(k, goodExamples.size()));
    }

    // ユークリッド距離の計算
    private double distance(float[] v1, float[] v2) {
        double sum = 0.0;
        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private void save() {
        try (FileWriter writer = new FileWriter(savePath.toFile())) {
            gson.toJson(dataset, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        File file = savePath.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                // ■ 修正: List<LearningData> 型として正しく読み込む
                Type listType = new TypeToken<ArrayList<LearningData>>(){}.getType();
                List<LearningData> loaded = gson.fromJson(reader, listType);
                if (loaded != null) {
                    dataset.addAll(loaded);
                    System.out.println("[Cardinal AI] Loaded " + dataset.size() + " training samples.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}