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

        // ■ IPv4を優先 (IPv6接続問題を回避)
        System.setProperty("java.net.preferIPv4Stack", "true");

        // ■ HttpClient: executorを分離、connectTimeout短縮、HTTP/1.1強制
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        if (config.isReady()) {
            System.out.println("[External AI] Service initialized. API: " + config.apiUrl + " Model: " + config.model);
            // ■ 起動時に接続テスト
            testConnection(config);
        } else {
            System.out
                    .println("[External AI] Service initialized but DISABLED. Set enabled=true and apiKey in config.");
        }
    }

    /**
     * API接続テスト（起動時に非同期で実行）
     */
    private void testConnection(ExternalAIConfig config) {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create(config.apiUrl);
                System.out.println("[External AI] Connection test: host=" + uri.getHost() + " port=" + uri.getPort());

                // 簡単なPOSTテスト (空ボディ) - ステータス確認のみ
                HttpRequest testReq = HttpRequest.newBuilder()
                        .uri(uri)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
                HttpResponse<String> testResp = httpClient.send(testReq, HttpResponse.BodyHandlers.ofString());
                System.out.println("[External AI] Connection test result: HTTP " + testResp.statusCode());
            } catch (java.net.ConnectException e) {
                System.err.println("[External AI] Connection test FAILED: Connection refused - " + e.getMessage());
            } catch (java.net.http.HttpConnectTimeoutException e) {
                System.err.println("[External AI] Connection test FAILED: Connect timeout - " + e.getMessage());
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("[External AI] Connection test FAILED: Request timeout - " + e.getMessage());
            } catch (javax.net.ssl.SSLException e) {
                System.err.println("[External AI] Connection test FAILED: SSL error - " + e.getMessage());
            } catch (java.nio.channels.UnresolvedAddressException e) {
                System.err.println("[External AI] Connection test FAILED: DNS resolution failed");
            } catch (Exception e) {
                System.err.println("[External AI] Connection test FAILED: " + e.getClass().getSimpleName() + " - "
                        + e.getMessage());
            }
        }, executor);
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
        System.out.println("[External AI] inferAsync called. isReady=" + config.isReady() + " httpClient="
                + (httpClient != null ? "OK" : "NULL"));

        if (!config.isReady() && !config.debugMode) {
            System.err.println("[External AI] inferAsync ABORT: not ready");
            return CompletableFuture.completedFuture(null);
        }

        // ■ デバッグモード: APIを呼ばずにダミー結果を返す
        if (config.debugMode) {
            System.out.println("[External AI] DEBUG MODE ACTIVE. Returning mock data.");
            return CompletableFuture.completedFuture(createMockData(script));
        }

        // 同一スクリプトの重複リクエストを防止
        int scriptHash = script.hashCode();
        if (!inFlightHashes.add(scriptHash)) {
            System.out.println("[External AI] Request already in-flight for hash: " + scriptHash + ". Skipping.");
            return CompletableFuture.completedFuture(null);
        }

        System.out.println("[External AI] Starting async request for hash: " + scriptHash + " URL: " + config.apiUrl);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(physics, attributes, script);
                boolean isGemini = isGeminiApi(config.apiUrl);
                String requestBody = isGemini ? buildGeminiRequestBody(prompt) : buildOpenAIRequestBody(prompt, config);

                System.out.println("[External AI] Request body length: " + requestBody.length() + " chars");

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(config.apiUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(config.timeoutSeconds))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

                if (!isGemini) {
                    reqBuilder.header("Authorization", "Bearer " + config.apiKey);
                }

                HttpRequest request = reqBuilder.build();

                // ■ リトライロジック: 429/5xx エラー時は最大3回リトライ
                final int MAX_RETRIES = 3;
                HttpResponse<String> response = null;
                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                    System.out.println(
                            "[External AI] Sending inference request (" + (isGemini ? "Gemini" : "OpenAI")
                                    + ") attempt " + attempt + "/" + MAX_RETRIES + "...");

                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    System.out.println("[External AI] Response received! Status: " + response.statusCode());

                    if (response.statusCode() == 200) {
                        break; // 成功
                    } else if (response.statusCode() == 429) {
                        // レート制限: 待ってからリトライ
                        int waitSec = 25; // デフォルト25秒
                        String body = response.body();
                        // "retry in XX.XXs" パターンから待機秒数を抽出
                        int retryIdx = body.indexOf("retry in ");
                        if (retryIdx >= 0) {
                            try {
                                String numStr = body.substring(retryIdx + 9, body.indexOf("s", retryIdx + 9));
                                waitSec = (int) Math.ceil(Double.parseDouble(numStr));
                            } catch (Exception ignored) {
                            }
                        }
                        if (attempt < MAX_RETRIES) {
                            System.out.println(
                                    "[External AI] Rate limited (429). Waiting " + waitSec + "s before retry...");
                            Thread.sleep(waitSec * 1000L);
                        } else {
                            System.err.println("[External AI] Rate limited (429). Max retries exhausted.");
                        }
                    } else if (response.statusCode() >= 500) {
                        // サーバーエラー: 5秒待ってリトライ
                        if (attempt < MAX_RETRIES) {
                            System.out.println("[External AI] Server error (" + response.statusCode()
                                    + "). Waiting 5s before retry...");
                            Thread.sleep(5000);
                        }
                    } else {
                        // 4xx (429以外) は即失敗
                        break;
                    }
                }

                if (response != null && response.statusCode() == 200) {
                    VisualMetadata result = parseResponse(response.body());
                    if (result != null) {
                        System.out.println("[External AI] Inference SUCCESS. Shape: " + result.shape + " Color: (" +
                                result.mainColor.x() + ", " + result.mainColor.y() + ", " + result.mainColor.z() + ")");
                    } else {
                        System.err.println("[External AI] Parse returned null. Response body: "
                                + response.body().substring(0, Math.min(500, response.body().length())));
                    }
                    return result;
                } else {
                    System.err.println(
                            "[External AI] API returned status " + response.statusCode() + ": "
                                    + response.body().substring(0, Math.min(500, response.body().length())));
                    return null;
                }
            } catch (java.net.http.HttpConnectTimeoutException e) {
                System.err
                        .println("[External AI] FAILED: Connection timeout (cannot reach server) - " + e.getMessage());
                return null;
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("[External AI] FAILED: Request timeout (server too slow) - " + e.getMessage());
                return null;
            } catch (java.net.ConnectException e) {
                System.err.println("[External AI] FAILED: Connection refused - " + e.getMessage());
                return null;
            } catch (java.io.IOException e) {
                System.err.println(
                        "[External AI] FAILED: IO error (" + e.getClass().getSimpleName() + ") - " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("[External AI]   Cause: " + e.getCause().getClass().getSimpleName() + " - "
                            + e.getCause().getMessage());
                }
                return null;
            } catch (InterruptedException e) {
                System.err.println("[External AI] FAILED: Request interrupted");
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                System.err.println("[External AI] FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
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
        sb.append("以下のスクリプト（ユーザーが入力した魔法の呪文テキスト）を解析し、最適な描画設定をJSON形式で返してください。\n");
        sb.append("JSONのみを返してください。説明文は不要です。\n\n");

        sb.append("【重要】スクリプトの単語から魔法の性質を判断してください。\n");
        sb.append("例: 'lightning bolt' → Motion属性高め、LIGHTNING形状、青白い色\n");
        sb.append("例: 'fire explosion' → Heat属性高め、SPHERE形状、赤橙色\n");
        sb.append("例: 'ice wall' → Cold属性高め、CUBE形状、水色\n");
        sb.append("例: 'holy heal' → Divine属性高め、RING形状、黄金色\n");
        sb.append("例: 'chaos void' → Entropy属性高め、VORTEX形状、紫黒色\n\n");

        sb.append("【スクリプト（呪文テキスト）】\n");
        for (String line : script) {
            if (line != null && !line.isEmpty()) {
                sb.append("  ").append(line).append("\n");
            }
        }
        sb.append("\n");

        sb.append("【ローカル推定の物理パラメータ】（参考値）\n");
        sb.append("- 温度: ").append(String.format("%.1f", physics.temperature)).append("K\n");
        sb.append("- エネルギー: ").append(String.format("%.1f", physics.energy)).append("\n");
        sb.append("- 速度: ").append(String.format("%.2f", physics.velocity)).append("\n");
        sb.append("- 質量: ").append(String.format("%.2f", physics.mass)).append("\n");
        sb.append("- 力のタイプ: ").append(physics.forceType.name()).append("\n");
        sb.append("- 固体: ").append(physics.isSolid).append("\n");
        sb.append("- 範囲: ").append(String.format("%.1f", physics.areaOfEffect)).append("\n\n");

        sb.append("【ローカル推定の属性ベクトル】（参考値・AIで再評価してください）\n");
        String[] attrNames = { "Heat(火)", "Cold(氷)", "Motion(雷/風)", "Entropy(混沌)", "Divine(神聖)" };
        for (int i = 0; i < Math.min(attributes.length, attrNames.length); i++) {
            sb.append("- ").append(attrNames[i]).append(": ").append(String.format("%.3f", attributes[i])).append("\n");
        }
        sb.append("※上記はローカル解析の結果です。スクリプトの意味に基づいてAI側で正確に再評価してください。\n\n");

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
                "  \"attributes\": {\"heat\": 0.0-1.0, \"cold\": 0.0-1.0, \"motion\": 0.0-1.0, \"entropy\": 0.0-1.0, \"divine\": 0.0-1.0},\n");
        sb.append(
                "  \"shape\": \"RING | COMPLEX_CIRCLE | SPHERE | BEAM | CYLINDER | RIPPLE | PARTICLE_MIST | CUBE | VORTEX | CONE | LIGHTNING | CRYSTAL\",\n");
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
        sb.append("  \"density\": 1.0,\n");
        sb.append("  \"shockwave\": false,\n");
        sb.append("  \"trailLength\": 0,\n");
        sb.append("  \"blockEffect\": \"NONE | BURN | FREEZE | EXPLODE | BEAM | BARRIER\",\n");
        sb.append("  \"residualType\": \"NONE | HEAT | FROST | ELECTRIC | CHAOS | HOLY\",\n");
        sb.append("  \"residualDuration\": 0\n");
        sb.append("}\n");
        sb.append("※attributes: スクリプトの意味から判断した5属性の強度(0.0-1.0)。必ずスクリプトの単語に基づいて判断すること。\n");
        sb.append("※blockEffect: この魔法がワールドのブロックに与える影響。\n");
        sb.append("※residualType: 魔法消滅後に残留する効果場のタイプ。\n");
        sb.append("※shockwave: RADIAL爆発時に衝撃波リングを表示するか。\n");
        sb.append("※trailLength: DIRECTIONAL時の残像トレイルの数(0-5)。\n");

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

        // ■ AI返却の属性値をrawVectorに格納
        if (json.has("attributes")) {
            try {
                JsonObject attrs = json.getAsJsonObject("attributes");
                meta.rawVector = new float[5];
                meta.rawVector[0] = attrs.has("heat") ? Mth.clamp(attrs.get("heat").getAsFloat(), 0, 1) : 0;
                meta.rawVector[1] = attrs.has("cold") ? Mth.clamp(attrs.get("cold").getAsFloat(), 0, 1) : 0;
                meta.rawVector[2] = attrs.has("motion") ? Mth.clamp(attrs.get("motion").getAsFloat(), 0, 1) : 0;
                meta.rawVector[3] = attrs.has("entropy") ? Mth.clamp(attrs.get("entropy").getAsFloat(), 0, 1) : 0;
                meta.rawVector[4] = attrs.has("divine") ? Mth.clamp(attrs.get("divine").getAsFloat(), 0, 1) : 0;
                System.out.println("[External AI] AI attributes: heat=" + meta.rawVector[0]
                        + " cold=" + meta.rawVector[1] + " motion=" + meta.rawVector[2]
                        + " entropy=" + meta.rawVector[3] + " divine=" + meta.rawVector[4]);
            } catch (Exception e) {
                System.err.println("[External AI] Failed to parse attributes: " + e.getMessage());
            }
        }

        return meta;
    }

    /**
     * デバッグモード用: ダミーの推論結果を生成する
     */
    private VisualMetadata createMockData(List<String> script) {
        // キーワードに基づいて簡易的に分岐
        String joined = String.join(" ", script).toLowerCase();

        VisualMetadata meta = new VisualMetadata();
        meta.rawVector = new float[5];

        if (joined.contains("fire") || joined.contains("explosion") || joined.contains("bomb")
                || joined.contains("heat") || joined.contains("extream")) {
            meta.shape = EnumMagicShape.SPHERE;
            meta.mainColor = new Vector3f(1.0f, 0.2f, 0.0f); // Red
            meta.subColor = new Vector3f(1.0f, 0.8f, 0.0f); // Orange
            meta.layerCount = 3;
            meta.scale = 2.0f;
            meta.rawVector[0] = 0.9f; // Heat
            meta.hasLightning = true;
        } else if (joined.contains("ice") || joined.contains("cold") || joined.contains("freeze")
                || joined.contains("water") || joined.contains("blizzard")) {
            meta.shape = EnumMagicShape.CRYSTAL;
            meta.mainColor = new Vector3f(0.2f, 0.6f, 1.0f); // Blue
            meta.subColor = new Vector3f(0.8f, 0.9f, 1.0f); // White
            meta.layerCount = 2;
            meta.scale = 1.6f;
            meta.rawVector[1] = 0.9f; // Cold
            meta.isSpiky = true;
        } else if (joined.contains("lightning") || joined.contains("shock") || joined.contains("thunder")
                || joined.contains("motion") || joined.contains("charge")) {
            meta.shape = EnumMagicShape.LIGHTNING;
            meta.mainColor = new Vector3f(1.0f, 1.0f, 0.2f); // Yellow
            meta.subColor = new Vector3f(0.5f, 0.5f, 1.0f); // Blue-Violet
            meta.layerCount = 4;
            meta.scale = 1.8f;
            meta.rawVector[2] = 0.9f; // Motion
            meta.hasLightning = true;
        } else if (joined.contains("beam") || joined.contains("laser") || joined.contains("ray")
                || joined.contains("pierce")) {
            meta.shape = EnumMagicShape.BEAM;
            meta.mainColor = new Vector3f(1.0f, 0.0f, 0.8f); // Magenta
            meta.subColor = new Vector3f(1.0f, 1.0f, 1.0f); // White
            meta.layerCount = 2;
            meta.scale = 1.2f;
            meta.rawVector[2] = 0.8f; // Motion
        } else if (joined.contains("void") || joined.contains("chaos") || joined.contains("dark")
                || joined.contains("black") || joined.contains("gravity")) {
            // VORTEXがあれば使用、なければSPHERE
            try {
                meta.shape = EnumMagicShape.valueOf("VORTEX");
            } catch (IllegalArgumentException e) {
                meta.shape = EnumMagicShape.SPHERE;
            }
            meta.mainColor = new Vector3f(0.1f, 0.0f, 0.2f); // Dark Violet
            meta.subColor = new Vector3f(0.3f, 0.0f, 0.0f); // Dark Red
            meta.layerCount = 5;
            meta.scale = 2.5f;
            meta.rawVector[3] = 1.0f; // Entropy
            meta.isWavy = true;
        } else if (joined.contains("holy") || joined.contains("light") || joined.contains("divine")
                || joined.contains("heal") || joined.contains("god")) {
            try {
                meta.shape = EnumMagicShape.valueOf("RING");
            } catch (IllegalArgumentException e) {
                meta.shape = EnumMagicShape.SPHERE;
            }
            meta.mainColor = new Vector3f(1.0f, 0.9f, 0.2f); // Gold
            meta.subColor = new Vector3f(1.0f, 1.0f, 0.8f); // Light Yellow
            meta.layerCount = 2;
            meta.scale = 3.0f;
            meta.rawVector[4] = 1.0f; // Divine
        } else {
            // Default: Generic Magic Ball
            meta.shape = EnumMagicShape.SPHERE;
            meta.mainColor = new Vector3f(0.1f, 0.8f, 0.4f); // Emerald
            meta.subColor = new Vector3f(0.0f, 0.3f, 0.1f); // Dark Green
            meta.layerCount = 2;
            meta.scale = 1.0f;
            meta.rawVector[4] = 0.3f;
        }

        System.out.println("[External AI] Mock data created: Shape=" + meta.shape + " Color=" + meta.mainColor);
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
