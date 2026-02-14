package com.COLLABOMOD.collabomod.network;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー→クライアント: 外部AI推論の結果を送信する。
 * クライアントGUIのモニターにプレビューを反映し、チャットで通知する。
 */
public class PacketAIInferenceResult {
    private final CompoundTag resultTag;

    public PacketAIInferenceResult(CompoundTag resultTag) {
        this.resultTag = resultTag;
    }

    public PacketAIInferenceResult(FriendlyByteBuf buf) {
        this.resultTag = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(resultTag);
    }

    public static void handle(PacketAIInferenceResult msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                // クライアントサイドで処理
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null)
                    return;

                VisualMetadata result = VisualMetadata.fromNBT(msg.resultTag);
                boolean aiSuccess = msg.resultTag.getBoolean("AISuccess");

                // GUIに結果を通知
                if (mc.screen instanceof com.COLLABOMOD.collabomod.client.gui.MagicConsoleScreen screen) {
                    screen.onAIInferenceComplete(result, aiSuccess);
                }

                // チャット通知
                if (aiSuccess) {
                    mc.player.displayClientMessage(
                            new TextComponent("§a[AI] §f推論完了! Shape: §e" + result.shape.name()
                                    + " §fColor: §e(" + String.format("%.2f", result.mainColor.x())
                                    + ", " + String.format("%.2f", result.mainColor.y())
                                    + ", " + String.format("%.2f", result.mainColor.z()) + ")"),
                            false);
                } else {
                    mc.player.displayClientMessage(
                            new TextComponent("§e[AI] §fローカルAIのプレビューを表示中"),
                            false);
                }
            } catch (Exception e) {
                System.err.println("[AI Result] Error handling inference result: " + e.getMessage());
                e.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
