package com.COLLABOMOD.collabomod.client.gui;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.magic.SpellResolver;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketEditCAD;
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
        this.addRenderableWidget(new Button(editorX + 70, buttonY, 40, 18, new TextComponent("Write"), (btn) -> this.writeToCAD()));
        this.addRenderableWidget(new Button(editorX + 10, buttonY, 40, 18, new TextComponent("Load"), (btn) -> this.loadFromCAD()));
    }

    private void onLineEdited(int index, String text) {
        this.activeLineIndex = index;
        this.cachedPreviewMeta = null;
    }

    private void writeToCAD() {
        List<String> script = scriptLines.stream().map(EditBox::getValue).collect(Collectors.toList());
        NetworkHandler.INSTANCE.sendToServer(new PacketEditCAD(script));
    }

    private void loadFromCAD() {
        // メニューの0番スロット(CADスロット)を取得
        if (this.menu.slots.size() > 0) {
            ItemStack stack = this.menu.slots.get(0).getItem();

            // アイテムがあり、かつScriptタグを持っている場合
            if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("Script")) {
                ListTag list = stack.getTag().getList("Script", Tag.TAG_STRING);

                // 行ごとにEditBoxへ反映
                for (int i = 0; i < MAX_LINES; i++) {
                    if (i < list.size()) {
                        scriptLines.get(i).setValue(list.getString(i));
                    } else {
                        scriptLines.get(i).setValue(""); // 行が足りない分は空白で埋める
                    }
                }

                // 読み込み完了後、プレビューを強制更新するためにキャッシュをクリア
                this.cachedPreviewMeta = null;
            }
        }
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
        // 'e'キーが押された場合、かついずれかのEditBoxがフォーカスされている場合
        if (keyCode == GLFW.GLFW_KEY_E) {
            for (EditBox box : scriptLines) {
                if (box.isFocused()) {
                    // EditBoxにキー入力を処理させ、イベントの伝播を止める
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
            this.font.draw(poseStack, String.valueOf(i+1), 90, 20 + (i * LINE_HEIGHT) + 1, 0xFFAAAAAA);
        }

        // アクティブ行バー
        int editY = 20 + (activeLineIndex * LINE_HEIGHT);
        fill(poseStack, 103, editY, 104, editY + 10, 0xFFFFFF00);

        // CADラベル
        if (!this.menu.slots.isEmpty()) {
            Slot slot = this.menu.slots.get(0);
            // スロットの上に緑色で「CAD」と表示
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

        // CADスロットの枠のみ描画
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
        hLine(poseStack, monX - 1, monX + monSize, monY - 1, frame);
        vLine(poseStack, monX - 1, monY - 1, monY + monSize, frame);
        hLine(poseStack, monX - 1, monX + monSize, monY + monSize, frame);
        vLine(poseStack, monX + monSize, monY - 1, monY + monSize, frame);

        // PREVIEWの文字位置調整 (枠の上に表示)
        this.font.draw(poseStack, "PREVIEW", monX, monY - 10, frame);

        if (this.cachedPreviewMeta == null) {
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
            int color = 0xFF000000 | ((int)(meta.mainColor.x() * 255) << 16) | ((int)(meta.mainColor.y() * 255) << 8) | ((int)(meta.mainColor.z() * 255));
            fill(poseStack, monX + 30, monY + 30, monX + 50, monY + 50, color);
            poseStack.pushPose();
            poseStack.translate(monX + 4, monY + 60, 0);
            poseStack.scale(0.8f, 0.8f, 1.0f);
            this.font.draw(poseStack, meta.shape.name(), 0, 0, 0xFFFFFF);
            poseStack.popPose();
        } else {
            drawCenteredString(poseStack, this.font, "No Input", monX + 40, monY + 35, 0xFF555555);
        }
    }
}