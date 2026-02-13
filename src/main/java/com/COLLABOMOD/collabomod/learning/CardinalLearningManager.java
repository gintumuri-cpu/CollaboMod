package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
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
    private final Path preferencePath;
    private AttributePreference globalPreference;
    private LearningData pendingData = null;

    private CardinalLearningManager() {
        // 保存先：config/collabomod_brain.json
        this.preferencePath = FMLPaths.CONFIGDIR.get().resolve("collabomod_brain.json");
        loadBrain();
    }

    public static CardinalLearningManager getInstance() {
        return INSTANCE;
    }

    public void registerInteraction(LearningData data) {
        this.pendingData = data;
    }

    // 脳へアクセスするメソッド
    public AttributePreference getBrain() {
        return globalPreference;
    }

    public void rateLastInteraction(float score) {
        if (pendingData != null) {
            pendingData.developerScore = score;
            dataset.add(pendingData);
            globalPreference.train(pendingData.attributeVector, pendingData.visualSettings, score);
            saveBrain();
            System.out.println("[Cardinal AI] Data saved. Score: " + score + ", Total Data: " + dataset.size());
            pendingData = null;
        } else {
            System.out.println("[Cardinal AI] No pending interaction to rate.");
        }
    }

    /**
     * 魔法の実行結果を記録し、自己評価を行う
     * 
     * @param attributeVector 属性ベクトル
     * @param phy             物理メタデータ
     * @param vis             視覚メタデータ
     * @param score           一貫性スコア
     */
    public void recordExperience(float[] attributeVector, PhysicsMetadata phy, VisualMetadata vis, float score) {
        if (attributeVector == null || phy == null || vis == null)
            return;

        // 学習データを生成
        LearningData data = new LearningData();
        data.attributeVector = attributeVector;
        // VisualSettingsに変換する必要があるが、ここでは簡易的にVisualMetadataをそのまま使う
        // 本来はVisualMetadataから色や形状などの設定を抽出してVisualSettingsに詰める
        data.visualSettings = new VisualSettings(vis.mainColor, vis.shape, vis.animationType);
        data.consistencyScore = score;

        // 自己評価を実行
        selfEvaluate(data, score);
    }

    /**
     * AIによる自己評価を実行する。
     * 物理現象と描画の一貫性スコアに基づいて、弱い学習信号を脳に送る。
     * 
     * @param data             学習対象のデータ
     * @param consistencyScore -1.0 ~ 1.0 の一貫性スコア
     */
    public void selfEvaluate(LearningData data, float consistencyScore) {
        if (data == null)
            return;

        // 自己評価の学習率はプレイヤー評価より低く設定 (例: 0.2倍)
        // これにより、プレイヤーの評価を優先しつつ、AIが自律的に微調整を行う
        float learningSignal = consistencyScore * 0.2f;
        globalPreference.train(data.attributeVector, data.visualSettings, learningSignal);
        saveBrain(); // 自己学習でも脳を保存
    }

    /**
     * 外部AIの推論結果をローカルAI脳に逆学習させる。
     * 外部AIの高品質な推論をローカルに取り込み、フォールバック精度を向上させる。
     * 学習率はプレイヤー評価の半分（0.5倍）で控えめに設定。
     */
    public void learnFromExternalAI(float[] attributeVector, VisualMetadata vis) {
        if (attributeVector == null || vis == null || globalPreference == null)
            return;

        VisualSettings settings = new VisualSettings(vis.mainColor, vis.shape, vis.animationType);
        // 外部AIの推論は「正解」として扱うが、プレイヤー評価より弱い学習信号
        float externalAISignal = 0.5f;
        globalPreference.train(attributeVector, settings, externalAISignal);
        saveBrain();
        System.out.println("[Cardinal AI] Learned from external AI result. Shape: " + vis.shape);
    }

    private void saveBrain() {
        try (FileWriter writer = new FileWriter(preferencePath.toFile())) {
            gson.toJson(globalPreference, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBrain() {
        File file = preferencePath.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                globalPreference = gson.fromJson(reader, AttributePreference.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ファイルがない、または読み込み失敗時は新規作成
        if (globalPreference == null) {
            globalPreference = new AttributePreference();
        }
    }
}