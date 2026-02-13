package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.*;
import com.COLLABOMOD.collabomod.learning.AttributePreference.AttributeNode;
import com.google.gson.*;
import com.mojang.math.Vector3f;
import net.minecraft.util.Mth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 外部AIサービスへのHTTP通信を担うクラス。
 * 非同期でLLM APIを呼び出し、VisualMetadataを推論する。
 */
public class ExternalAIService {

    private static final ExternalAIService INSTANCE = new ExternalAIService();
    private static final Gson GSON = new Gson();

    private HttpClient httpClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    // 同一スクリプトの重複リクエストを防止するin-flightガード
    private final java.util.Set<Integer> inFlightHashes = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private ExternalAIService() {
    }

    public static ExternalAIService getInstance() {
        return INSTANCE;
    }

    /**
     * HTTP クライアントを初期化する。
     */
    public void initialize() {
        ExternalAIConfig config = ExternalAIConfig.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
                .executor(executor)
                .build();

        if (config.isReady()) {
            System.out.println("[External AI] Service initialized. API: " + config.apiUrl + " Model: " + config.model);
        } else {
            System.out
                    .println("[External AI] Service initialized but DISABLED. Set enabled=true and apiKey in config.");
        }
    }

    /**
     * 非同期で外部AIに描画推論を依頼する。
     *
     * @param physics    物理メタデータ
     * @param attributes 属性ベクトル (5次元)
     * @param script     魔法スクリプト
     * @return CompletableFuture<VisualMetadata> 推論結果（失敗時はnull）
     */
    /**
     * APIのURLからGemini APIかどうかを判定する。
     */
    private boolean isGeminiApi(String apiUrl) {
        return apiUrl != null && apiUrl.contains("generativelanguage.googleapis.com");
    }

    public CompletableFuture<VisualMetadata> inferAsync(PhysicsMetadata physics, float[] attributes,
            List<String> script) {
        ExternalAIConfig config = ExternalAIConfig.getInstance();

        if (!config.isReady() || httpClient == null) {
            return CompletableFuture.completedFuture(null);
        }

        // 同一スクリプトの重複リクエストを防止
        int scriptHash = script.hashCode();
        if (!inFlightHashes.add(scriptHash)) {
            System.out.println("[External AI] Request already in-flight for hash: " + scriptHash + ". Skipping.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(physics, attributes, script);
                boolean isGemini = isGeminiApi(config.apiUrl);
                String requestBody = isGemini ? buildGeminiRequestBody(prompt) : buildOpenAIRequestBody(prompt, config);

                // Gemini API: ?key= がURLに含まれているのでAuthorizationヘッダー不要
                // OpenAI API: Bearer トークンで認証
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(config.apiUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(config.timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

                if (!isGemini) {
                    reqBuilder.header("Authorization", "Bearer " + config.apiKey);
                }

                HttpRequest request = reqBuilder.build();

                System.out.println(
                        "[External AI] Sending inference request (" + (isGemini ? "Gemini" : "OpenAI") + ")...");

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    VisualMetadata result = parseResponse(response.body());
                    if (result != null) {
                        System.out.println("[External AI] Inference SUCCESS. Shape: " + result.shape + " Color: (" +
                                result.mainColor.x() + ", " + result.mainColor.y() + ", " + result.mainColor.z() + ")");
                    }
                    return result;
                } else {
                    System.err.println(
                            "[External AI] API returned status " + response.statusCode() + ": " + response.body());
                    return null;
                }
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("[External AI] Inference FAILED: request timed out");
                return null;
            } catch (Exception e) {
                System.err.println("[External AI] Inference FAILED: " + e.getMessage());
                return null;
            } finally {
                inFlightHashes.remove(scriptHash);
            }
        }, executor);
    }

    /**
     * LLMに送信するプロンプトを構築する。
     */
    private String buildPrompt(PhysicsMetadata physics, float[] attributes, List<String> script) {
        StringBuilder sb = new StringBuilder();
        sb.append("あなたはMinecraftの魔法エフェクトデザイナーです。\n");
        sb.append("以下の物理パラメータに基づいて、最適な描画設定をJSON形式で返してください。\n");
        sb.append("JSONのみを返してください。説明文は不要です。\n\n");

        sb.append("【物理パラメータ】\n");
        sb.append("- 温度: ").append(String.format("%.1f", physics.temperature)).append("K\n");
        sb.append("- エネルギー: ").append(String.format("%.1f", physics.energy)).append("\n");
        sb.append("- 速度: ").append(String.format("%.2f", physics.velocity)).append("\n");
        sb.append("- 質量: ").append(String.format("%.2f", physics.mass)).append("\n");
        sb.append("- 力のタイプ: ").append(physics.forceType.name()).append("\n");
        sb.append("- 固体: ").append(physics.isSolid).append("\n");
        sb.append("- 範囲: ").append(String.format("%.1f", physics.areaOfEffect)).append("\n\n");

        sb.append("【属性ベクトル】\n");
        String[] attrNames = { "Heat", "Cold", "Motion", "Entropy", "Divine" };
        for (int i = 0; i < Math.min(attributes.length, attrNames.length); i++) {
            sb.append("- ").append(attrNames[i]).append(": ").append(String.format("%.3f", attributes[i])).append("\n");
        }
        sb.append("\n");

        sb.append("【スクリプト】\n");
        for (String line : script) {
            sb.append("  ").append(line).append("\n");
        }
        sb.append("\n");

        // 学習フィードバック: プレイヤーの好みを外部AIに伝える
        try {
            AttributePreference brain = CardinalLearningManager.getInstance().getBrain();
            if (brain != null) {
                sb.append("【プレイヤーの好み（学習済み）】\n");
                String[] prefNames = { "Fire", "Ice", "Air", "Entropy", "Divine" };
                for (int i = 0; i < Math.min(brain.nodes.length, prefNames.length); i++) {
                    AttributeNode node = brain.nodes[i];
                    sb.append("- ").append(prefNames[i]).append(": 理想色=RGB(")
                            .append(String.format("%.2f", node.idealMainColor.x())).append(", ")
                            .append(String.format("%.2f", node.idealMainColor.y())).append(", ")
                            .append(String.format("%.2f", node.idealMainColor.z())).append(")");

                    // 形状の好み（スコア上位を表示）
                    if (!node.shapeScores.isEmpty()) {
                        node.shapeScores.entrySet().stream()
                                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                                .limit(2)
                                .forEach(e -> sb.append(" 好む形状=").append(e.getKey().name())
                                        .append("(").append(String.format("%.1f", e.getValue())).append(")"));
                    }

                    // アニメーションの好み
                    if (!node.animScores.isEmpty()) {
                        node.animScores.entrySet().stream()
                                .sorted((a, b) -> Float.compare(b.getValue(), a.getValue()))
                                .limit(2)
                                .forEach(e -> sb.append(" 好むAnim=").append(e.getKey().name())
                                        .append("(").append(String.format("%.1f", e.getValue())).append(")"));
                    }
                    sb.append("\n");
                }
                sb.append("※上記の好みを参考にしつつ、物理パラメータに適した描画を回答してください。\n\n");
            }
        } catch (Exception e) {
            // 学習データ取得失敗時はスキップ
        }

        sb.append("【出力JSON形式】\n");
        sb.append("{\n");
        sb.append(
                "  \"shape\": \"RING | COMPLEX_CIRCLE | SPHERE | BEAM | CYLINDER | RIPPLE | PARTICLE_MIST | CUBE | VORTEX\",\n");
        sb.append(
                "  \"animationType\": \"EXPAND_FADE | CONVERGE | SUSTAIN_SPIN | PULSE | RISE | SHOOT_AHEAD | IMPLODE | BEAM_EXTEND | FIXED\",\n");
        sb.append("  \"mainColor\": {\"r\": 0.0-1.0, \"g\": 0.0-1.0, \"b\": 0.0-1.0},\n");
        sb.append("  \"subColor\": {\"r\": 0.0-1.0, \"g\": 0.0-1.0, \"b\": 0.0-1.0},\n");
        sb.append("  \"scale\": 1.0,\n");
        sb.append("  \"isSpiky\": false,\n");
        sb.append("  \"isWavy\": false,\n");
        sb.append("  \"hasLightning\": false,\n");
        sb.append("  \"isSolid\": false,\n");
        sb.append("  \"layerCount\": 1,\n");
        sb.append("  \"rotationSpeed\": 1.0,\n");
        sb.append("  \"density\": 1.0\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Gemini API 用のリクエストボディを構築する。
     * 形式: { contents: [{ parts: [{ text: "..." }] }], generationConfig: { ... } }
     */
    private String buildGeminiRequestBody(String prompt) {
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", prompt);

        JsonArray parts = new JsonArray();
        parts.add(textPart);

        JsonObject content = new JsonObject();
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 8192);
        generationConfig.addProperty("responseMimeType", "application/json");

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        return GSON.toJson(body);
    }

    /**
     * OpenAI互換のAPIリクエストボディを構築する。
     */
    private String buildOpenAIRequestBody(String prompt, ExternalAIConfig config) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", config.model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 500);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        return GSON.toJson(body);
    }

    /**
     * APIレスポンスからVisualMetadataをパースする。
     * Gemini / OpenAI / Ollama の各形式に自動対応。
     */
    VisualMetadata parseResponse(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();

            String content;
            if (root.has("candidates")) {
                // Gemini形式: candidates[0].content.parts[0].text
                content = root.getAsJsonArray("candidates")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("content")
                        .getAsJsonArray("parts")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
            } else if (root.has("choices")) {
                // OpenAI形式: choices[0].message.content
                content = root.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            } else {
                // ダイレクトJSON（Ollama等）
                content = responseBody;
            }

            // コードブロックマーカーを除去
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            return jsonToVisualMetadata(json);

        } catch (Exception e) {
            System.err.println("[External AI] Failed to parse response: " + e.getMessage());
            return null;
        }
    }

    /**
     * JSONオブジェクトからVisualMetadataを構築する。
     * 不正な値はデフォルトにフォールバックする。
     */
    private VisualMetadata jsonToVisualMetadata(JsonObject json) {
        VisualMetadata meta = new VisualMetadata();

        // Shape
        if (json.has("shape")) {
            try {
                meta.shape = EnumMagicShape.valueOf(json.get("shape").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                // フォールバック
            }
        }

        // Animation
        if (json.has("animationType")) {
            try {
                meta.animationType = EnumMagicAnimation.valueOf(json.get("animationType").getAsString().toUpperCase());
            } catch (IllegalArgumentException e) {
                // フォールバック
            }
        }

        // Main Color
        if (json.has("mainColor")) {
            JsonObject c = json.getAsJsonObject("mainColor");
            meta.mainColor = new Vector3f(
                    clampColor(c, "r"),
                    clampColor(c, "g"),
                    clampColor(c, "b"));
        }

        // Sub Color
        if (json.has("subColor")) {
            JsonObject c = json.getAsJsonObject("subColor");
            meta.subColor = new Vector3f(
                    clampColor(c, "r"),
                    clampColor(c, "g"),
                    clampColor(c, "b"));
        }

        // Numeric fields
        if (json.has("scale"))
            meta.scale = Mth.clamp(json.get("scale").getAsFloat(), 0.1f, 100f);
        if (json.has("rotationSpeed"))
            meta.rotationSpeed = json.get("rotationSpeed").getAsFloat();
        if (json.has("density"))
            meta.density = Mth.clamp(json.get("density").getAsFloat(), 0.1f, 10f);
        if (json.has("layerCount"))
            meta.layerCount = Mth.clamp(json.get("layerCount").getAsInt(), 1, 10);

        // Boolean fields
        if (json.has("isSpiky"))
            meta.isSpiky = json.get("isSpiky").getAsBoolean();
        if (json.has("isWavy"))
            meta.isWavy = json.get("isWavy").getAsBoolean();
        if (json.has("hasLightning"))
            meta.hasLightning = json.get("hasLightning").getAsBoolean();
        if (json.has("isSolid"))
            meta.isSolid = json.get("isSolid").getAsBoolean();

        return meta;
    }

    private float clampColor(JsonObject colorObj, String key) {
        if (colorObj.has(key)) {
            return Mth.clamp(colorObj.get(key).getAsFloat(), 0.0f, 1.0f);
        }
        return 0.5f;
    }

    /**
     * サービスをシャットダウンする。
     */
    public void shutdown() {
        executor.shutdown();
        System.out.println("[External AI] Service shut down.");
    }
}
