package com.COLLABOMOD.collabomod.client.gui;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.magic.SpellResolver;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketEditCAD;
import com.COLLABOMOD.collabomod.network.PacketRequestAIInference;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MagicConsoleScreen extends AbstractContainerScreen<MagicConsoleMenu> {

    private static final int MAX_LINES = 10;
    private static final int LINE_HEIGHT = 12;

    private final List<EditBox> scriptLines = new ArrayList<>();
    private int activeLineIndex = 0;
    private VisualMetadata cachedPreviewMeta = null;

    // ■ AI推論状態管理
    private boolean isAIInferring = false;
    private boolean aiInferenceSuccess = false;
    private VisualMetadata aiInferredMeta = null;
    private int inferenceAnimTick = 0;

    public MagicConsoleScreen(MagicConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int editorX = x + 105;
        int editorY = y + 20;

        this.scriptLines.clear();
        for (int i = 0; i < MAX_LINES; i++) {
            EditBox box = new EditBox(this.font, editorX, editorY + (i * LINE_HEIGHT), 135, 10, new TextComponent(""));
            box.setMaxLength(64);
            box.setBordered(false);
            box.setTextColor(0xFFFFFF);
            int finalI = i;
            box.setResponder((text) -> this.onLineEdited(finalI, text));
            this.addRenderableWidget(box);
            this.scriptLines.add(box);
        }

        int buttonY = editorY + (MAX_LINES * LINE_HEIGHT) + 8;
        this.addRenderableWidget(
                new Button(editorX + 70, buttonY, 40, 18, new TextComponent("Write"), (btn) -> this.writeToCAD()));
        this.addRenderableWidget(
                new Button(editorX + 10, buttonY, 40, 18, new TextComponent("Load"), (btn) -> this.loadFromCAD()));
        // ■ Apply ボタン追加
        this.addRenderableWidget(new Button(editorX + 10, buttonY + 22, 100, 18, new TextComponent("§aApply AI"),
                (btn) -> this.requestAIInference()));
    }

    private void onLineEdited(int index, String text) {
        this.activeLineIndex = index;
        this.cachedPreviewMeta = null;
        // スクリプト変更時はAI推論結果もリセット
        this.aiInferredMeta = null;
        this.aiInferenceSuccess = false;
    }

    private void writeToCAD() {
        List<String> script = scriptLines.stream().map(EditBox::getValue).collect(Collectors.toList());
        NetworkHandler.INSTANCE.sendToServer(new PacketEditCAD(script));
    }

    private void loadFromCAD() {
        if (this.menu.slots.size() > 0) {
            ItemStack stack = this.menu.slots.get(0).getItem();
            if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("Script")) {
                ListTag list = stack.getTag().getList("Script", Tag.TAG_STRING);
                for (int i = 0; i < MAX_LINES; i++) {
                    if (i < list.size()) {
                        scriptLines.get(i).setValue(list.getString(i));
                    } else {
                        scriptLines.get(i).setValue("");
                    }
                }
                this.cachedPreviewMeta = null;
            }
        }
    }

    /**
     * ■ Apply AI ボタン: 外部AI推論をリクエスト
     */
    private void requestAIInference() {
        System.out.println("[Apply AI] Button clicked. isAIInferring=" + this.isAIInferring);
        // ■ 推論中は重複リクエストを防止
        if (this.isAIInferring) {
            System.out.println("[Apply AI] BLOCKED: already inferring. animTick=" + this.inferenceAnimTick);
            return;
        }

        List<String> script = scriptLines.stream()
                .map(EditBox::getValue)
                .collect(Collectors.toList());

        boolean hasContent = script.stream().anyMatch(s -> !s.isEmpty());
        if (!hasContent) {
            System.out.println("[Apply AI] BLOCKED: no script content");
            return;
        }

        System.out.println("[Apply AI] Sending packet to server. Script: " + script);
        this.isAIInferring = true;
        this.aiInferenceSuccess = false;
        this.aiInferredMeta = null;
        this.inferenceAnimTick = 0;
        NetworkHandler.INSTANCE.sendToServer(new PacketRequestAIInference(script));
        System.out.println("[Apply AI] Packet sent successfully.");
    }

    /**
     * ■ サーバーからAI推論結果を受信した時に呼ばれる
     */
    public void onAIInferenceComplete(VisualMetadata result, boolean success) {
        this.isAIInferring = false;
        this.aiInferenceSuccess = success;
        this.aiInferredMeta = result;
        this.cachedPreviewMeta = result; // モニターの表示を更新
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderMonitor(poseStack);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_E) {
            for (EditBox box : scriptLines) {
                if (box.isFocused()) {
                    return box.keyPressed(keyCode, scanCode, modifiers);
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // 行番号
        for (int i = 0; i < MAX_LINES; i++) {
            this.font.draw(poseStack, String.valueOf(i + 1), 90, 20 + (i * LINE_HEIGHT) + 1, 0xFFAAAAAA);
        }

        // アクティブ行バー
        int editY = 20 + (activeLineIndex * LINE_HEIGHT);
        fill(poseStack, 103, editY, 104, editY + 10, 0xFFFFFF00);

        // CADラベル
        if (!this.menu.slots.isEmpty()) {
            Slot slot = this.menu.slots.get(0);
            this.font.draw(poseStack, "CAD", slot.x + 2, slot.y - 10, 0xFF00FF00);
        }
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 背景
        fill(poseStack, x, y, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

        // 枠線
        int border = 0xFF888888;
        hLine(poseStack, x, x + this.imageWidth - 1, y, border);
        hLine(poseStack, x, x + this.imageWidth - 1, y + this.imageHeight - 1, border);
        vLine(poseStack, x, y, y + this.imageHeight - 1, border);
        vLine(poseStack, x + this.imageWidth - 1, y, y + this.imageHeight - 1, border);

        // エディタ背景
        int editorX = x + 105;
        int editorY = y + 20;
        int editorH = (MAX_LINES * LINE_HEIGHT) + 2;
        fill(poseStack, editorX - 2, editorY - 2, editorX + 137, editorY + editorH, 0xFF000000);

        // CADスロット枠
        for (int i = 0; i < this.menu.slots.size(); i++) {
            if (i == 0) {
                Slot slot = this.menu.slots.get(i);
                int sx = x + slot.x;
                int sy = y + slot.y;

                fill(poseStack, sx, sy, sx + 16, sy + 16, 0xFF002200);
                int frame = 0xFF00AA00;
                hLine(poseStack, sx - 1, sx + 16, sy - 1, frame);
                vLine(poseStack, sx - 1, sy - 1, sy + 16, frame);
                hLine(poseStack, sx - 1, sx + 16, sy + 16, frame);
                vLine(poseStack, sx + 16, sy - 1, sy + 16, frame);
            }
        }
    }

    private void renderMonitor(PoseStack poseStack) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        int monX = x + 10;
        int monY = y + 20;
        int monSize = 80;

        fill(poseStack, monX, monY, monX + monSize, monY + monSize, 0xFF000000);
        int frame = 0xFFAADDFF;

        // ■ AI推論中は枠線を点滅させる
        if (isAIInferring) {
            inferenceAnimTick++;
            frame = (inferenceAnimTick / 5 % 2 == 0) ? 0xFFFF8800 : 0xFF884400;
            // ■ 120秒タイムアウト: API タイムアウトに合わせる
            if (inferenceAnimTick > 2400) {
                this.isAIInferring = false;
                this.aiInferenceSuccess = false;
            }
        } else if (aiInferenceSuccess && aiInferredMeta != null) {
            frame = 0xFF00FF00; // AI推論成功: 緑枠
        }

        hLine(poseStack, monX - 1, monX + monSize, monY - 1, frame);
        vLine(poseStack, monX - 1, monY - 1, monY + monSize, frame);
        hLine(poseStack, monX - 1, monX + monSize, monY + monSize, frame);
        vLine(poseStack, monX + monSize, monY - 1, monY + monSize, frame);

        // ラベル
        String label = "PREVIEW";
        int labelColor = 0xFFAADDFF;
        if (isAIInferring) {
            String[] anim = { "AI推論中.", "AI推論中..", "AI推論中..." };
            label = anim[(inferenceAnimTick / 10) % 3];
            labelColor = 0xFFFF8800;
        } else if (aiInferenceSuccess && aiInferredMeta != null) {
            label = "AI READY";
            labelColor = 0xFF00FF00;
        }
        this.font.draw(poseStack, label, monX, monY - 10, labelColor);

        // プレビュー表示
        if (this.cachedPreviewMeta == null && !isAIInferring) {
            List<String> script = scriptLines.stream()
                    .map(EditBox::getValue)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!script.isEmpty()) {
                this.cachedPreviewMeta = SpellResolver.resolveVisuals(script);
            }
        }

        if (this.cachedPreviewMeta != null) {
            VisualMetadata meta = this.cachedPreviewMeta;
            int color = 0xFF000000 | ((int) (meta.mainColor.x() * 255) << 16) | ((int) (meta.mainColor.y() * 255) << 8)
                    | ((int) (meta.mainColor.z() * 255));

            // 形状に応じたプレビュー描画
            int cx = monX + 40;
            int cy = monY + 35;
            renderShapePreview(poseStack, meta, cx, cy, color);

            // 情報テキスト
            poseStack.pushPose();
            poseStack.translate(monX + 4, monY + 60, 0);
            poseStack.scale(0.7f, 0.7f, 1.0f);
            this.font.draw(poseStack, meta.shape.name(), 0, 0, 0xFFFFFF);
            if (aiInferenceSuccess) {
                this.font.draw(poseStack, "§a[AI]", 60, 0, 0xFF00FF00);
            }
            poseStack.popPose();

            // スケール情報
            poseStack.pushPose();
            poseStack.translate(monX + 4, monY + 70, 0);
            poseStack.scale(0.6f, 0.6f, 1.0f);
            this.font.draw(poseStack, "Scale: " + String.format("%.1f", meta.scale), 0, 0, 0xFFAAAAAA);
            poseStack.popPose();
        } else if (!isAIInferring) {
            drawCenteredString(poseStack, this.font, "No Input", monX + 40, monY + 35, 0xFF555555);
        }
    }

    /**
     * ■ 形状に応じた簡易プレビュー
     */
    private void renderShapePreview(PoseStack poseStack, VisualMetadata meta, int cx, int cy, int color) {
        switch (meta.shape) {
            case SPHERE:
                // 円
                for (int deg = 0; deg < 360; deg += 10) {
                    int x1 = cx + (int) (12 * Math.cos(Math.toRadians(deg)));
                    int y1 = cy + (int) (12 * Math.sin(Math.toRadians(deg)));
                    fill(poseStack, x1, y1, x1 + 2, y1 + 2, color);
                }
                break;
            case BEAM:
            case CYLINDER:
                // 縦長の矩形
                fill(poseStack, cx - 3, cy - 15, cx + 3, cy + 15, color);
                break;
            case CONE:
                // 三角形風
                for (int i = 0; i < 20; i++) {
                    int w = i / 2;
                    fill(poseStack, cx - w, cy - 10 + i, cx + w, cy - 10 + i + 1, color);
                }
                break;
            case RING:
            case COMPLEX_CIRCLE:
                // リング
                for (int deg = 0; deg < 360; deg += 8) {
                    int x1 = cx + (int) (14 * Math.cos(Math.toRadians(deg)));
                    int y1 = cy + (int) (14 * Math.sin(Math.toRadians(deg)));
                    fill(poseStack, x1, y1, x1 + 2, y1 + 2, color);
                    int x2 = cx + (int) (10 * Math.cos(Math.toRadians(deg)));
                    int y2 = cy + (int) (10 * Math.sin(Math.toRadians(deg)));
                    fill(poseStack, x2, y2, x2 + 1, y2 + 1, color);
                }
                break;
            case VORTEX:
                // 螺旋
                for (int i = 0; i < 60; i++) {
                    float angle = i * 0.3f;
                    float dist = i * 0.3f;
                    int px = cx + (int) (dist * Math.cos(angle));
                    int py = cy + (int) (dist * Math.sin(angle));
                    fill(poseStack, px, py, px + 2, py + 2, color);
                }
                break;
            case LIGHTNING:
                // ジグザグ線
                int lx = cx - 5;
                int ly = cy - 12;
                for (int i = 0; i < 6; i++) {
                    int nlx = lx + (i % 2 == 0 ? 10 : -10);
                    int nly = ly + 5;
                    hLine(poseStack, Math.min(lx, nlx), Math.max(lx, nlx), ly, color);
                    vLine(poseStack, nlx, ly, nly, color);
                    lx = nlx;
                    ly = nly;
                }
                break;
            case CRYSTAL:
                // ダイヤモンド形
                for (int i = 0; i < 12; i++) {
                    int w2 = i < 6 ? i : 12 - i;
                    fill(poseStack, cx - w2, cy - 6 + i, cx + w2, cy - 6 + i + 1, color);
                }
                break;
            case CUBE:
                fill(poseStack, cx - 10, cy - 10, cx + 10, cy + 10, color);
                break;
            default:
                fill(poseStack, cx - 10, cy - 10, cx + 10, cy + 10, color);
                break;
        }
    }
}