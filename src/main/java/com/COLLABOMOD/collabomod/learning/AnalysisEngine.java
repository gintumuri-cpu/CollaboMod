package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.*;
import com.COLLABOMOD.collabomod.magic.command.CommandType;
import com.COLLABOMOD.collabomod.magic.command.ICommand;
import com.COLLABOMOD.collabomod.magic.command.MeshCommand;
import com.COLLABOMOD.collabomod.magic.command.ParticleCommand;
import com.COLLABOMOD.collabomod.magic.command.SoundCommand;
import com.COLLABOMOD.collabomod.magic.command.TextureCommand;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AnalysisEngine {

    private static final Random random = new Random();

    // 単語ベクトル辞書 (50次元)
    private static final Map<String, float[]> WORD_VECTORS = new HashMap<>();
    private static final int VECTOR_DIMENSION = 50;

    // 現象辞書 (JSONからロード)
    private static final List<PhenomenonRule> PHENOMENA_RULES = new ArrayList<>();

    // 属性キーワードの定義
    private static final String KEYWORD_HEAT = "fire";
    private static final String KEYWORD_COLD = "ice";
    private static final String KEYWORD_MOTION = "wind";
    private static final String KEYWORD_ENTROPY = "chaos";
    private static final String KEYWORD_DIVINE = "light";

    // 属性キーワードのベクトルキャッシュ
    private static float[] vecHeat;
    private static float[] vecCold;
    private static float[] vecMotion;
    private static float[] vecEntropy;
    private static float[] vecDivine;

    static {
        // 起動時に学習済みモデルと現象辞書をロードする
        loadWordVectors();
        loadPhenomenaRules();

        // 属性キーワードのベクトルをキャッシュ
        vecHeat = getVectorOrRandom(KEYWORD_HEAT);
        vecCold = getVectorOrRandom(KEYWORD_COLD);
        vecMotion = getVectorOrRandom(KEYWORD_MOTION);
        vecEntropy = getVectorOrRandom(KEYWORD_ENTROPY);
        vecDivine = getVectorOrRandom(KEYWORD_DIVINE);
    }

    /**
     * リソースから単語ベクトルファイル (GloVe形式) を読み込む
     */
    private static void loadWordVectors() {
        int lineCount = 0;
        int skippedCount = 0;
        try (InputStream is = AnalysisEngine.class
                .getResourceAsStream("/assets/collabo_mod/models/glove_50d_lite.txt")) {
            if (is == null) {
                System.err.println(
                        "[Cardinal AI ERROR] Word vector model not found! Path: /assets/collabo_mod/models/glove_50d_lite.txt");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length != VECTOR_DIMENSION + 1) {
                        if (skippedCount < 10) {
                            System.err.println("[Cardinal AI WARN] Skipping line " + lineCount
                                    + " due to unexpected format. Parts: " + parts.length);
                        }
                        skippedCount++;
                        continue;
                    }

                    String word = parts[0];
                    float[] vector = new float[VECTOR_DIMENSION];
                    try {
                        for (int i = 0; i < VECTOR_DIMENSION; i++) {
                            vector[i] = Float.parseFloat(parts[i + 1]);
                        }
                        WORD_VECTORS.put(word, vector);
                    } catch (NumberFormatException e) {
                        if (skippedCount < 10) {
                            System.err.println(
                                    "[Cardinal AI WARN] Skipping line " + lineCount + " due to NumberFormatException.");
                        }
                        skippedCount++;
                    }
                }
            }
            System.out.println("[Cardinal AI] Loaded " + WORD_VECTORS.size() + " word vectors from " + lineCount
                    + " lines. (" + skippedCount + " lines skipped)");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * リソースから現象辞書 (JSON) を読み込む
     */
    private static void loadPhenomenaRules() {
        try (InputStream is = AnalysisEngine.class
                .getResourceAsStream("/assets/collabo_mod/knowledge/phenomena.json")) {
            if (is == null) {
                System.err.println("Phenomena dictionary not found!");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                Gson gson = new Gson();
                JsonObject json = gson.fromJson(reader, JsonObject.class);
                JsonArray rules = json.getAsJsonArray("rules");

                for (JsonElement element : rules) {
                    JsonObject ruleObj = element.getAsJsonObject();
                    PhenomenonRule rule = new PhenomenonRule();

                    if (ruleObj.has("comment"))
                        rule.comment = ruleObj.get("comment").getAsString();

                    // 条件のパース
                    JsonObject conditions = ruleObj.getAsJsonObject("conditions");
                    if (conditions.has("min_temperature"))
                        rule.minTemperature = conditions.get("min_temperature").getAsFloat();
                    if (conditions.has("max_temperature"))
                        rule.maxTemperature = conditions.get("max_temperature").getAsFloat();
                    if (conditions.has("min_velocity"))
                        rule.minVelocity = conditions.get("min_velocity").getAsFloat();
                    if (conditions.has("min_energy"))
                        rule.minEnergy = conditions.get("min_energy").getAsFloat();
                    if (conditions.has("force_type"))
                        rule.forceType = conditions.get("force_type").getAsString();
                    if (conditions.has("min_attribute_heat"))
                        rule.minAttributeHeat = conditions.get("min_attribute_heat").getAsFloat();
                    if (conditions.has("min_attribute_cold"))
                        rule.minAttributeCold = conditions.get("min_attribute_cold").getAsFloat();
                    if (conditions.has("min_attribute_motion"))
                        rule.minAttributeMotion = conditions.get("min_attribute_motion").getAsFloat();
                    if (conditions.has("min_attribute_entropy"))
                        rule.minAttributeEntropy = conditions.get("min_attribute_entropy").getAsFloat();
                    if (conditions.has("min_attribute_divine"))
                        rule.minAttributeDivine = conditions.get("min_attribute_divine").getAsFloat();

                    // コマンドのパース
                    JsonArray commands = ruleObj.getAsJsonArray("commands");
                    for (JsonElement cmdElem : commands) {
                        JsonObject cmdObj = cmdElem.getAsJsonObject();
                        rule.commandTemplates.add(cmdObj);
                    }

                    PHENOMENA_RULES.add(rule);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * スクリプト全体の意味ベクトルを解析し、5つの属性値を算出する
     */
    public static float[] analyzeScript(List<String> script) {
        float[] scriptVector = new float[VECTOR_DIMENSION];
        int wordCount = 0;

        // ■ キーワード直接マッチング用カウンター
        float[] keywordBoost = new float[5]; // [heat, cold, motion, entropy, divine]

        for (String line : script) {
            String[] words = line.toLowerCase().split("[^a-z]+");
            for (String word : words) {
                if (word.isEmpty())
                    continue;
                float[] vec = getVectorOrRandom(word);
                for (int i = 0; i < VECTOR_DIMENSION; i++) {
                    scriptVector[i] += vec[i];
                }
                wordCount++;

                // ■ キーワード直接マッチング: GloVeベクトルに加えて属性を確実に反映
                float[] kwScore = getKeywordScore(word);
                for (int k = 0; k < 5; k++) {
                    keywordBoost[k] += kwScore[k];
                }
            }
        }

        if (wordCount > 0) {
            for (int i = 0; i < VECTOR_DIMENSION; i++) {
                scriptVector[i] /= wordCount;
            }
        }

        float[] attributes = new float[5];
        attributes[0] = cosineSimilarity(scriptVector, vecHeat);
        attributes[1] = cosineSimilarity(scriptVector, vecCold);
        attributes[2] = cosineSimilarity(scriptVector, vecMotion);
        attributes[3] = cosineSimilarity(scriptVector, vecEntropy);
        attributes[4] = cosineSimilarity(scriptVector, vecDivine);

        for (int i = 0; i < 5; i++) {
            attributes[i] = Math.max(0, attributes[i]);
        }

        // ■ キーワードブーストの加算 (GloVeが弱くても基本的な属性を保証)
        for (int i = 0; i < 5; i++) {
            attributes[i] = Math.min(1.0f, attributes[i] + keywordBoost[i]);
        }

        // デバッグログ: 属性値の確認
        System.out.println("[Cardinal AI] Script: " + script);
        System.out.println("[Cardinal AI] Attributes: " + Arrays.toString(attributes));
        System.out.println("[Cardinal AI] Keyword boosts: " + Arrays.toString(keywordBoost));

        return attributes;
    }

    // ■ キーワード→属性スコア: Map + あいまい一致（前方一致・部分一致対応）
    private static final Map<String, float[]> KEYWORD_SCORES = new HashMap<>();

    static {
        // float[5] = {heat, cold, motion, entropy, divine}
        // --- Heat (火/爆発) ---
        for (String w : new String[] { "fire", "flame", "burn", "blaze", "inferno", "heat", "magma", "lava",
                "scorch", "ignite", "combust", "incinerate", "fireball", "firestorm" })
            KEYWORD_SCORES.put(w, new float[] { 0.6f, 0, 0, 0, 0 });
        for (String w : new String[] { "hot", "warm", "ember", "solar", "sun", "pyro", "thermal", "sear",
                "smelt", "forge", "furnace", "volcano", "molten", "flare" })
            KEYWORD_SCORES.put(w, new float[] { 0.4f, 0, 0, 0, 0 });
        for (String w : new String[] { "bomb", "explosion", "blast", "burst", "detonate", "erupt", "explode",
                "extreme", "extream", "destroy", "demolish", "nuke", "napalm", "combust",
                "meteor", "comet", "impact", "smash", "crash", "shatter" })
            KEYWORD_SCORES.put(w, new float[] { 0.3f, 0, 0, 0, 0 });

        // --- Cold (氷/冷気) ---
        for (String w : new String[] { "ice", "frost", "freeze", "frozen", "blizzard", "glacier", "snow",
                "cold", "arctic", "icicle", "frostbite", "permafrost", "iceberg" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0.6f, 0, 0, 0 });
        for (String w : new String[] { "chill", "cool", "winter", "crystal", "cryo", "hail", "sleet",
                "tundra", "frigid", "frosty", "glacial", "polar", "subzero", "diamond" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0.4f, 0, 0, 0 });

        // --- Motion (雷/風/速度) ---
        for (String w : new String[] { "lightning", "thunder", "bolt", "spark", "electric", "shock", "voltage",
                "storm", "thunderbolt", "discharge", "arc", "plasma", "zap", "electro" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0.6f, 0, 0 });
        for (String w : new String[] { "wind", "gust", "tornado", "speed", "rush", "swift", "rapid", "flash",
                "sonic", "hurricane", "cyclone", "typhoon", "vortex", "whirlwind", "tempest",
                "force", "kinetic", "momentum", "velocity", "impulse", "thrust", "jet", "dash" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0.4f, 0, 0 });
        for (String w : new String[] { "air", "breeze", "current", "wave", "gale", "draft", "blow",
                "fly", "soar", "float", "hover", "levitate", "teleport", "warp" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0.3f, 0, 0 });

        // --- Entropy (混沌/闇) ---
        for (String w : new String[] { "chaos", "entropy", "void", "darkness", "shadow", "corrupt", "decay",
                "wither", "abyss", "oblivion", "annihilate", "disintegrate", "dissolve" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0.6f, 0 });
        for (String w : new String[] { "dark", "black", "curse", "hex", "blight", "doom", "death", "toxic",
                "poison", "plague", "venom", "necrotic", "undead", "zombie", "skeleton",
                "demon", "devil", "hell", "infernal", "nightmare", "horror", "dread" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0.4f, 0 });
        for (String w : new String[] { "random", "unstable", "wild", "distort", "twist", "warp", "mutate",
                "corrupt", "taint", "rot", "erode", "dissolve" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0.3f, 0 });

        // --- Divine (神聖/光) ---
        for (String w : new String[] { "light", "holy", "divine", "sacred", "angel", "heaven", "purify",
                "bless", "sanctify", "exorcise", "smite", "judgment", "consecrate" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0, 0.6f });
        for (String w : new String[] { "heal", "restore", "radiant", "luminous", "star", "celestial", "aura",
                "miracle", "prayer", "salvation", "redemption", "grace", "blessing",
                "aurora", "dawn", "guardian", "protect", "shield", "barrier", "ward" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0, 0.4f });
        for (String w : new String[] { "bright", "glow", "shine", "pure", "white", "golden", "silver",
                "moon", "lunar", "ethereal", "spirit", "soul", "mystic" })
            KEYWORD_SCORES.put(w, new float[] { 0, 0, 0, 0, 0.3f });

        // --- 汎用戦闘/魔法語 (複数属性に少しずつ加算) ---
        KEYWORD_SCORES.put("attack", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("strike", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("power", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("energy", new float[] { 0.1f, 0, 0.3f, 0, 0 });
        KEYWORD_SCORES.put("magic", new float[] { 0, 0, 0, 0.1f, 0.2f });
        KEYWORD_SCORES.put("spell", new float[] { 0, 0, 0, 0.1f, 0.2f });
        KEYWORD_SCORES.put("cast", new float[] { 0, 0, 0.1f, 0.1f, 0.1f });
        KEYWORD_SCORES.put("summon", new float[] { 0, 0, 0, 0.2f, 0.2f });
        KEYWORD_SCORES.put("elemental", new float[] { 0.15f, 0.15f, 0.15f, 0, 0 });
        KEYWORD_SCORES.put("dragon", new float[] { 0.3f, 0, 0, 0.1f, 0 });
        KEYWORD_SCORES.put("water", new float[] { 0, 0.3f, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("earth", new float[] { 0, 0, 0, 0, 0.2f });
        KEYWORD_SCORES.put("stone", new float[] { 0, 0, 0, 0, 0.1f });
        KEYWORD_SCORES.put("rock", new float[] { 0, 0, 0, 0, 0.1f });
        KEYWORD_SCORES.put("metal", new float[] { 0, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("iron", new float[] { 0, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("steel", new float[] { 0, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("gravity", new float[] { 0, 0, 0.3f, 0.2f, 0 });
        KEYWORD_SCORES.put("mega", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("ultra", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("super", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("hyper", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("max", new float[] { 0.2f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("beam", new float[] { 0.1f, 0, 0.3f, 0, 0.1f });
        KEYWORD_SCORES.put("ray", new float[] { 0.1f, 0, 0.2f, 0, 0.2f });
        KEYWORD_SCORES.put("laser", new float[] { 0.2f, 0, 0.3f, 0, 0 });
        KEYWORD_SCORES.put("cannon", new float[] { 0.3f, 0, 0.2f, 0, 0 });
        KEYWORD_SCORES.put("arrow", new float[] { 0, 0, 0.4f, 0, 0 });
        KEYWORD_SCORES.put("bullet", new float[] { 0, 0, 0.4f, 0, 0 });
        KEYWORD_SCORES.put("missile", new float[] { 0.2f, 0, 0.3f, 0, 0 });
        KEYWORD_SCORES.put("nova", new float[] { 0.3f, 0, 0, 0, 0.2f });
        KEYWORD_SCORES.put("big", new float[] { 0.1f, 0, 0.1f, 0, 0 });
        KEYWORD_SCORES.put("huge", new float[] { 0.1f, 0, 0.1f, 0, 0 });
        KEYWORD_SCORES.put("giant", new float[] { 0.1f, 0, 0.1f, 0, 0 });
        KEYWORD_SCORES.put("massive", new float[] { 0.1f, 0, 0.1f, 0, 0 });
    }

    /**
     * キーワードスコアを取得する（完全一致→前方一致→部分一致の3段階）
     */
    private static float[] getKeywordScore(String word) {
        if (word.length() < 2)
            return new float[5];

        // 1. 完全一致
        float[] exact = KEYWORD_SCORES.get(word);
        if (exact != null)
            return exact;

        // 2. 前方一致 (3文字以上の前方部分が一致する最長キーワードを探す)
        float[] bestPrefix = null;
        int bestLen = 0;
        for (Map.Entry<String, float[]> entry : KEYWORD_SCORES.entrySet()) {
            String key = entry.getKey();
            if (key.length() >= 3 && word.length() >= 3) {
                // wordがkeyで始まる、またはkeyがwordで始まる
                int minLen = Math.min(word.length(), key.length());
                int matchLen = 0;
                for (int i = 0; i < minLen; i++) {
                    if (word.charAt(i) == key.charAt(i))
                        matchLen++;
                    else
                        break;
                }
                // 3文字以上の前方一致で、かつ短い方の70%以上一致
                if (matchLen >= 3 && matchLen >= Math.min(word.length(), key.length()) * 0.7f
                        && matchLen > bestLen) {
                    bestLen = matchLen;
                    bestPrefix = entry.getValue();
                }
            }
        }
        if (bestPrefix != null) {
            // 前方一致は完全一致より少し弱く
            float[] scaled = new float[5];
            for (int i = 0; i < 5; i++)
                scaled[i] = bestPrefix[i] * 0.8f;
            return scaled;
        }

        return new float[5];
    }

    private static float[] getVectorOrRandom(String word) {
        if (WORD_VECTORS.containsKey(word)) {
            return WORD_VECTORS.get(word);
        }
        int hash = word.hashCode();
        Random rng = new Random(hash);
        float[] vec = new float[VECTOR_DIMENSION];
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            vec[i] = (rng.nextFloat() - 0.5f) * 2.0f;
        }
        return vec;
    }

    private static float cosineSimilarity(float[] v1, float[] v2) {
        float dot = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        for (int i = 0; i < VECTOR_DIMENSION; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        if (norm1 == 0 || norm2 == 0)
            return 0.0f;
        return dot / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    public static PhysicsMetadata derivePhysicsFromScript(List<String> script, float[] attributes) {
        PhysicsMetadata phy = new PhysicsMetadata();

        // 属性値の正規化 (合計が1になるように)
        float totalAttr = 0;
        for (float f : attributes)
            totalAttr += f;
        if (totalAttr > 0) {
            for (int i = 0; i < attributes.length; i++)
                attributes[i] /= totalAttr;
        }

        // 1. 属性ベクトルによる基礎ステータス設定
        phy.temperature = 300.0f; // 基準温度

        phy.temperature += attributes[0] * 3000.0F;
        phy.temperature -= attributes[1] * 2500.0F;

        phy.energy += attributes[0] * 150.0F;
        phy.energy += attributes[4] * 200.0F;

        if (attributes[1] > 0.3F) {
            phy.isSolid = true;
            phy.hardness += attributes[1] * 10.0F;
            phy.mass += attributes[1] * 5.0F;
        }

        phy.velocity += attributes[2] * 3.0F;
        phy.mass += attributes[2] * 0.1F;
        phy.velocity += attributes[4] * 1.0F;

        // 2. スクリプトのロジックコマンドによる挙動補正
        boolean hasMove = false;

        for (String line : script) {
            String l = line.toLowerCase().trim();
            if (l.startsWith("damage(")) {
                phy.energy += parseFloatArg(l, "damage", 0);
            }
            if (l.startsWith("knockback(")) {
                phy.velocity += parseFloatArg(l, "knockback", 0) * 0.5f;
                hasMove = true;
            }
            if (l.startsWith("area(")) {
                phy.areaOfEffect = Math.max(phy.areaOfEffect, parseFloatArg(l, "area", 0));
            }
            if (l.contains("explode") || l.contains("burst")) {
                phy.forceType = PhysicsMetadata.EnumForceType.RADIAL;
                phy.energy *= 1.5F;
            }
            if (l.contains("fly") || l.contains("shoot") || l.contains("accel")) {
                hasMove = true;
            }
            if (l.contains("gravity")) {
                phy.gravity = true;
            }
        }

        // 3. ForceTypeの最終決定
        if (phy.forceType == PhysicsMetadata.EnumForceType.NONE) {
            if (hasMove) {
                phy.forceType = PhysicsMetadata.EnumForceType.DIRECTIONAL;
            } else if (phy.areaOfEffect > 0) {
                phy.forceType = PhysicsMetadata.EnumForceType.RADIAL;
            } else if (phy.isSolid) {
                phy.forceType = PhysicsMetadata.EnumForceType.FIELD;
            } else {
                phy.forceType = PhysicsMetadata.EnumForceType.RADIAL;
            }
        }

        if (phy.temperature < 0)
            phy.temperature = 0;
        if (phy.mass < 0.1F)
            phy.mass = 0.1F;

        // デバッグログ: 物理パラメータの確認
        System.out.println("[Cardinal AI] Physics: Temp=" + phy.temperature + ", Energy=" + phy.energy + ", Type="
                + phy.forceType);

        return phy;
    }

    public static VisualMetadata deriveVisualsFromPhysics(PhysicsMetadata phy, float[] attributes, long seed) {
        VisualMetadata meta = new VisualMetadata();
        Random rand = new Random(seed);
        AttributePreference brain = CardinalLearningManager.getInstance().getBrain();

        // 脳の学習データから形状・アニメーションを推論
        // 学習データがない場合はforceTypeベースのデフォルトにフォールバック
        meta.shape = brain.preferShape(attributes, phy.forceType);
        meta.animationType = brain.preferAnimation(attributes, meta.shape);

        // 物理的制約によるバリデーション
        // 固体なのにビーム形状は不自然 → キューブに補正
        if (phy.isSolid && (meta.shape == EnumMagicShape.BEAM || meta.shape == EnumMagicShape.PARTICLE_MIST)) {
            meta.shape = EnumMagicShape.CUBE;
        }

        meta.mainColor = brain.inferColor(attributes);
        meta.scale = 1.0f + phy.areaOfEffect + (phy.energy / 50.0f);
        if (phy.energy > 80.0f)
            meta.isSpiky = true;
        if (phy.temperature > 1500.0f)
            meta.isWavy = true;

        float[] raw = new float[5];
        raw[0] = Mth.clamp((phy.temperature - 300f) / 2000f, 0, 1) * 0.5f + attributes[0] * 0.5f;
        raw[1] = Mth.clamp((273f - phy.temperature) / 273f, 0, 1) * 0.5f + attributes[1] * 0.5f;
        raw[2] = Mth.clamp(phy.velocity / 3.0f, 0, 1) * 0.5f + attributes[2] * 0.5f;
        raw[3] = attributes[3];
        raw[4] = attributes[4];
        meta.rawVector = raw;

        meta.timeline = generateTimeline(meta);

        return meta;
    }

    /**
     * 【ステップ4】物理現象と属性から実行コマンドリストを生成する
     * JSONルールベースの生成ロジック
     */
    public static ListTag generateCommandList(PhysicsMetadata phy, float[] attributes) {
        ListTag commandList = new ListTag();
        boolean matched = false;

        for (PhenomenonRule rule : PHENOMENA_RULES) {
            if (rule.matches(phy, attributes)) {
                matched = true;
                System.out.println("[Cardinal AI] Rule Matched: " + rule.comment); // マッチしたルールをログ出力
                for (JsonObject cmdTemplate : rule.commandTemplates) {
                    ICommand cmd = createCommandFromTemplate(cmdTemplate);
                    if (cmd != null) {
                        commandList.add(cmd.serializeNBT());
                    }
                }
            }
        }

        if (!matched) {
            System.out.println("[Cardinal AI] No rule matched. Using fallback.");
        }

        // 汎用エフェクト (常に発動)
        ParticleCommand sparkle = new ParticleCommand(0.0f, new ResourceLocation("minecraft:firework"), 3);
        commandList.add(sparkle.serializeNBT());

        return commandList;
    }

    private static ICommand createCommandFromTemplate(JsonObject template) {
        String type = template.get("type").getAsString();

        if ("particle".equals(type)) {
            ResourceLocation id = new ResourceLocation(template.get("id").getAsString());
            int count = template.has("count") ? template.get("count").getAsInt() : 1;
            return new ParticleCommand(0.0f, id, count);
        } else if ("sound".equals(type)) {
            ResourceLocation id = new ResourceLocation(template.get("id").getAsString());
            float volume = template.has("volume") ? template.get("volume").getAsFloat() : 1.0f;
            float pitch = template.has("pitch") ? template.get("pitch").getAsFloat() : 1.0f;
            return new SoundCommand(0.0f, id, volume, pitch);
        } else if ("mesh".equals(type)) {
            EnumMagicShape shape = EnumMagicShape.valueOf(template.get("shape").getAsString());
            EnumMagicAnimation anim = EnumMagicAnimation.valueOf(template.get("anim").getAsString());
            float scale = template.has("scale") ? template.get("scale").getAsFloat() : 1.0f;
            float duration = template.has("duration") ? template.get("duration").getAsFloat() : 1.0f;

            Vector3f color = new Vector3f(1, 1, 1);
            if (template.has("r")) {
                color = new Vector3f(
                        template.get("r").getAsFloat(),
                        template.get("g").getAsFloat(),
                        template.get("b").getAsFloat());
            }
            return new MeshCommand(0.0f, shape, anim, color, scale, duration);
        } else if ("texture".equals(type)) {
            String texType = template.get("tex_type").getAsString(); // NOISE, STRIPE, CELL
            long seed = template.has("seed") ? template.get("seed").getAsLong() : random.nextLong();
            int color = 0xFFFFFFFF; // Default White
            if (template.has("color_hex")) {
                // 16進数文字列 (例: "FF0000") からパース
                try {
                    color = (int) Long.parseLong(template.get("color_hex").getAsString(), 16);
                    // アルファ値がない場合はFFを付与
                    if (template.get("color_hex").getAsString().length() <= 6) {
                        color |= 0xFF000000;
                    }
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            return new TextureCommand(0.0f, texType, seed, color);
        }
        return null;
    }

    private static List<VisualKeyframe> generateTimeline(VisualMetadata finalMeta) {
        List<VisualKeyframe> timeline = new ArrayList<>();
        float[] startVec = new float[5];
        float[] endVec = finalMeta.rawVector;

        switch (finalMeta.animationType) {
            case EXPAND_FADE:
                timeline.add(new VisualKeyframe(0.0f, finalMeta.mainColor, finalMeta.subColor, 0.1f, 0.8f,
                        finalMeta.shape, false, false, false, startVec));
                timeline.add(new VisualKeyframe(0.3f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 1.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                timeline.add(new VisualKeyframe(1.0f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale * 1.5f,
                        0.0f, finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                break;
            case BEAM_EXTEND:
                timeline.add(new VisualKeyframe(0.0f, finalMeta.mainColor, finalMeta.subColor, 0.2f, 0.5f,
                        finalMeta.shape, false, false, false, startVec));
                timeline.add(new VisualKeyframe(0.1f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 1.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                timeline.add(new VisualKeyframe(0.9f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 1.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                timeline.add(new VisualKeyframe(1.0f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale * 0.8f,
                        0.0f, finalMeta.shape, false, false, false, endVec));
                break;
            case SUSTAIN_SPIN:
                timeline.add(new VisualKeyframe(0.0f, finalMeta.mainColor, finalMeta.subColor, 0.1f, 0.0f,
                        finalMeta.shape, false, false, false, startVec));
                timeline.add(new VisualKeyframe(0.2f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 1.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                timeline.add(new VisualKeyframe(0.8f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 1.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                timeline.add(new VisualKeyframe(1.0f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale * 0.5f,
                        0.0f, finalMeta.shape, false, false, false, endVec));
                break;
            default:
                timeline.add(new VisualKeyframe(0.0f, finalMeta.mainColor, finalMeta.subColor, 0.1f, 0.8f,
                        finalMeta.shape, false, false, false, startVec));
                timeline.add(new VisualKeyframe(1.0f, finalMeta.mainColor, finalMeta.subColor, finalMeta.scale, 0.0f,
                        finalMeta.shape, finalMeta.isSpiky, finalMeta.isWavy, finalMeta.hasLightning, endVec));
                break;
        }
        return timeline;
    }

    /**
     * 外部AIが返したVisualMetadataに不足するrawVectorとtimelineを補完する。
     * 外部AIはshape/mainColor/animationTypeのみ返すが、レンダラーは
     * rawVector（パラメトリックメッシュ変形）とtimeline（キーフレーム動画）を必要とする。
     */
    public static VisualMetadata completeVisualMetadata(VisualMetadata aiMeta, PhysicsMetadata phy,
            float[] attributes) {
        // rawVectorの計算（物理パラメータ + 属性値から）
        if (aiMeta.rawVector == null || aiMeta.rawVector.length == 0) {
            float[] raw = new float[5];
            raw[0] = Mth.clamp((phy.temperature - 300f) / 2000f, 0, 1) * 0.5f + attributes[0] * 0.5f;
            raw[1] = Mth.clamp((273f - phy.temperature) / 273f, 0, 1) * 0.5f + attributes[1] * 0.5f;
            raw[2] = Mth.clamp(phy.velocity / 3.0f, 0, 1) * 0.5f + attributes[2] * 0.5f;
            raw[3] = attributes[3];
            raw[4] = attributes[4];
            aiMeta.rawVector = raw;
        }

        // scaleの補完
        if (aiMeta.scale <= 0.1f) {
            aiMeta.scale = 1.0f + phy.areaOfEffect + (phy.energy / 50.0f);
        }

        // 物理パラメータからフラグ補完
        if (phy.energy > 80.0f)
            aiMeta.isSpiky = true;
        if (phy.temperature > 1500.0f)
            aiMeta.isWavy = true;

        // timelineの生成
        if (aiMeta.timeline == null || aiMeta.timeline.isEmpty()) {
            aiMeta.timeline = generateTimeline(aiMeta);
        }

        return aiMeta;
    }

    public static float calculateConsistencyScore(PhysicsMetadata phy, VisualMetadata vis) {
        float score = 0.0f;
        if (phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL
                && (vis.shape == EnumMagicShape.BEAM || vis.shape == EnumMagicShape.CYLINDER))
            score += 0.4f;
        else if (phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL)
            score -= 0.4f;
        if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL && vis.shape == EnumMagicShape.SPHERE)
            score += 0.4f;
        else if (phy.forceType == PhysicsMetadata.EnumForceType.RADIAL)
            score -= 0.4f;
        if (phy.temperature > 1000f && (vis.mainColor.x() > 0.7f && vis.mainColor.y() < 0.5f))
            score += 0.2f;
        if (phy.temperature < 273f && (vis.mainColor.z() > 0.7f && vis.mainColor.x() < 0.5f))
            score += 0.2f;
        if (phy.energy > 50f && (vis.isSpiky || vis.layerCount > 1))
            score += 0.2f;
        if (phy.energy < 10f && (vis.isSpiky || vis.layerCount > 1))
            score -= 0.2f;
        return Mth.clamp(score, -1.0f, 1.0f);
    }

    private static void normalize(float[] v) {
        float mag = magnitude(v);
        if (mag > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] /= mag;
            }
        }
    }

    private static float magnitude(float[] v) {
        float sum = 0;
        for (float f : v)
            sum += f * f;
        return (float) Math.sqrt(sum);
    }

    private static float parseFloatArg(String line, String keyword, float defaultValue) {
        if (line.startsWith(keyword + "(")) {
            try {
                int start = line.indexOf('(') + 1;
                int end = line.indexOf(')');
                return Float.parseFloat(line.substring(start, end).trim());
            } catch (Exception e) {
                // ignore
            }
        }
        return defaultValue;
    }

    // 内部クラス: 現象ルール
    private static class PhenomenonRule {
        float minTemperature = -Float.MAX_VALUE;
        float maxTemperature = Float.MAX_VALUE;
        float minVelocity = -Float.MAX_VALUE;
        float minEnergy = -Float.MAX_VALUE;
        float minAttributeHeat = 0.0f;
        float minAttributeCold = 0.0f;
        float minAttributeMotion = 0.0f;
        float minAttributeEntropy = 0.0f;
        float minAttributeDivine = 0.0f;
        String forceType = null;
        String comment = ""; // デバッグ用コメント

        List<JsonObject> commandTemplates = new ArrayList<>();

        boolean matches(PhysicsMetadata phy, float[] attributes) {
            if (phy.temperature < minTemperature)
                return false;
            if (phy.temperature > maxTemperature)
                return false;
            if (phy.velocity < minVelocity)
                return false;
            if (phy.energy < minEnergy)
                return false;
            if (forceType != null && !forceType.equals(phy.forceType.name()))
                return false;
            if (attributes[0] < minAttributeHeat)
                return false;
            if (attributes[1] < minAttributeCold)
                return false;
            if (attributes[2] < minAttributeMotion)
                return false;
            if (attributes[3] < minAttributeEntropy)
                return false;
            if (attributes[4] < minAttributeDivine)
                return false;
            return true;
        }
    }
}