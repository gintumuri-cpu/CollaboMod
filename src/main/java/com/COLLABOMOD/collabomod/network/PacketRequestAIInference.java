package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.learning.AIInferenceCache;
import com.COLLABOMOD.collabomod.learning.AnalysisEngine;
import com.COLLABOMOD.collabomod.learning.ExternalAIConfig;
import com.COLLABOMOD.collabomod.learning.ExternalAIService;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * クライアント→サーバー: GUI Apply ボタンで外部AI推論をリクエストする。
 * サーバー側で推論を実行し、完了後 PacketAIInferenceResult をクライアントに返す。
 */
public class PacketRequestAIInference {
    private final List<String> scriptLines;

    public PacketRequestAIInference(List<String> scriptLines) {
        this.scriptLines = scriptLines;
    }

    public PacketRequestAIInference(FriendlyByteBuf buf) {
        this.scriptLines = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            this.scriptLines.add(buf.readUtf());
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(scriptLines.size());
        for (String s : scriptLines) {
            buf.writeUtf(s);
        }
    }

    public static void handle(PacketRequestAIInference msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            System.out.println("[Apply AI Server] Packet received! Script lines: " + msg.scriptLines.size());
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                System.err.println("[Apply AI Server] ABORT: player is null");
                return;
            }

            ExternalAIConfig config = ExternalAIConfig.getInstance();
            System.out.println("[Apply AI Server] Config: enabled=" + config.enabled
                    + " apiKey=" + (config.apiKey != null && !config.apiKey.isEmpty() ? "SET" : "EMPTY")
                    + " isReady=" + config.isReady());
            if (!config.isReady()) {
                // 外部AI無効 → ローカルAIのみでフォールバック結果を返す
                System.out.println("[Apply AI Server] External AI not ready. Using local fallback.");
                sendLocalFallback(player, msg.scriptLines);
                return;
            }

            // 属性解析と物理パラメータ計算
            float[] attributes = AnalysisEngine.analyzeScript(msg.scriptLines);
            PhysicsMetadata physics = AnalysisEngine.derivePhysicsFromScript(msg.scriptLines, attributes);

            // 非同期で外部AI推論を実行
            ExternalAIService.getInstance().inferAsync(physics, attributes, msg.scriptLines)
                    .whenCompleteAsync((result, throwable) -> {
                        try {
                            // サーバーが有効でプレイヤーがまだ接続中か確認
                            if (player.getServer() == null || player.hasDisconnected()) {
                                System.err.println("[External AI] Player disconnected before result delivery.");
                                return;
                            }

                            if (throwable != null) {
                                System.err
                                        .println("[External AI] Async inference exception: " + throwable.getMessage());
                                sendLocalFallback(player, msg.scriptLines);
                                return;
                            }

                            if (result != null) {
                                // 推論成功: キャッシュに保存 + timeline補完
                                int scriptHash = msg.scriptLines.hashCode();
                                VisualMetadata completed = AnalysisEngine.completeVisualMetadata(result, physics,
                                        attributes);
                                AIInferenceCache.getInstance().put(scriptHash, completed);
                                System.out.println("[External AI] GUI Apply: inference success for hash " + scriptHash);

                                // クライアントに結果を送信
                                CompoundTag resultTag = completed.toNBT();
                                resultTag.putBoolean("AISuccess", true);
                                NetworkHandler.INSTANCE.send(
                                        PacketDistributor.PLAYER.with(() -> player),
                                        new PacketAIInferenceResult(resultTag));
                            } else {
                                // 推論失敗 (タイムアウト含む): ローカルフォールバック
                                sendLocalFallback(player, msg.scriptLines);
                            }
                        } catch (Exception e) {
                            System.err.println("[External AI] Error in result callback: " + e.getMessage());
                            e.printStackTrace();
                            try {
                                sendLocalFallback(player, msg.scriptLines);
                            } catch (Exception e2) {
                                System.err.println("[External AI] Failed to send fallback: " + e2.getMessage());
                            }
                        }
                    }, player.getServer());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void sendLocalFallback(ServerPlayer player, List<String> script) {
        float[] attributes = AnalysisEngine.analyzeScript(script);
        PhysicsMetadata physics = AnalysisEngine.derivePhysicsFromScript(script, attributes);
        StringBuilder sb = new StringBuilder();
        for (String s : script)
            sb.append(s).append("\n");
        VisualMetadata visual = AnalysisEngine.deriveVisualsFromPhysics(physics, attributes, sb.toString().hashCode());

        CompoundTag resultTag = visual.toNBT();
        resultTag.putBoolean("AISuccess", false);
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketAIInferenceResult(resultTag));
    }
}
