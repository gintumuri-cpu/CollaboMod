package com.COLLABOMOD.collabomod.client.gui;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.magic.MagicComponentType;
import com.COLLABOMOD.collabomod.magic.MagicScriptEngine;
import com.COLLABOMOD.collabomod.magic.SpellContext;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketEditCAD;
import com.COLLABOMOD.collabomod.magic.SpellResolver;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MagicConsoleScreen extends AbstractContainerScreen<MagicConsoleMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    // パレットのコマンドリスト
    private static final String[] PALETTE_COMMANDS = {
            "IF (...) {",
            "REPEAT (3) {",
            "}",
            "Cast()",
            "Action.", "Attr.", "Mod." // 入力支援用プレフィックス
    };

    private final List<EditBox> codeInputs = new ArrayList<>();
    private int activeLineIndex = 0;

    public MagicConsoleScreen(MagicConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 320;
        this.imageHeight = 220;
        this.inventoryLabelY = 1000;
        this.titleLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ■ 1. 左側：構文パレットのみ配置
        // カテゴリボタン等は削除しました
        int palX = x + 10;
        int palY = y + 20;
        for (String cmd : PALETTE_COMMANDS) {
            this.addRenderableWidget(new Button(palX, palY, 80, 16, new TextComponent(cmd), button -> {
                insertText(cmd);
            }));
            palY += 18;
        }

        // ■ 2. 中央：エディタエリア (EditBox)
        if (codeInputs.isEmpty()) {
            for (int i = 0; i < 10; i++) {
                EditBox box = new EditBox(this.font, 0, 0, 140, 12, new TextComponent(""));
                box.setMaxLength(200);
                box.setBordered(false);
                box.setTextColor(0xFFFFFF);
                int finalI = i;
                box.setResponder((text) -> this.activeLineIndex = finalI);
                codeInputs.add(box);
            }
        }
        for (int i = 0; i < codeInputs.size(); i++) {
            EditBox box = codeInputs.get(i);
            box.x = x + 100;
            box.y = y + 20 + (i * 14);
            this.addRenderableWidget(box);
        }

        // ■ 3. 右下：インストールボタン
        // x=215, y=113 (CADスロットの右隣)
        this.addRenderableWidget(new Button(x + 215, y + 113, 60, 20, new TextComponent("INSTALL"), button -> {
            List<String> code = codeInputs.stream().map(EditBox::getValue).collect(Collectors.toList());
            NetworkHandler.INSTANCE.sendToServer(new PacketEditCAD(code));
        }));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1. 各エディタボックスのキー処理を優先
        for (EditBox box : codeInputs) {
            if (box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        // 2. インベントリキー（Eなど）が押された場合
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            // エディタのどれかがフォーカスされていたら、画面を閉じずにイベントを消費する（文字入力とみなす）
            for (EditBox box : codeInputs) {
                if (box.isFocused()) {
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void insertText(String text) {
        if (activeLineIndex < codeInputs.size()) {
            EditBox box = codeInputs.get(activeLineIndex);
            String current = box.getValue();
            if (current.isEmpty()) box.setValue(text);
            else box.setValue(current + " " + text);

            box.setFocus(true);
            // 他のフォーカスを外す
            for (int i = 0; i < codeInputs.size(); i++) {
                if (i != activeLineIndex) codeInputs.get(i).setFocus(false);
            }
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        fill(poseStack, x, y, x + this.imageWidth, y + this.imageHeight, 0xFF202020);

        vLine(poseStack, x + 95, y + 10, y + 210, 0xFF555555);
        vLine(poseStack, x + 245, y + 10, y + 210, 0xFF555555);

        super.render(poseStack, mouseX, mouseY, partialTick);

        renderMonitor(poseStack, x + 250, y + 20);

        this.font.draw(poseStack, "Palette", x + 10, y + 8, 0xFFAAAAAA);
        this.font.draw(poseStack, "Magic Code Editor", x + 100, y + 8, 0xFFFFFFFF);
        this.font.draw(poseStack, "Monitor", x + 250, y + 8, 0xFF55FFFF);

        // 行番号
        for(int i=0; i<10; i++) {
            this.font.draw(poseStack, String.valueOf(i+1), x + 100 - 12, y + 20 + (i * 14) + 2, 0xFF888888);
        }

        // アクティブ行ハイライト
        int editY = y + 20 + (activeLineIndex * 14);
        fill(poseStack, x + 98, editY, x + 100, editY + 12, 0xFFFFFF00);

        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    // 背景描画（スロット枠）
    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // ■ 修正: CADスロット枠の描画位置変更 (189, 112)
        // Menu側の (190, 113) に合わせて調整
        this.blit(poseStack, x + 189, y + 112, 79, 16, 18, 18);

        this.font.draw(poseStack, "CAD", x + 190, y + 102, 0xFFAAAAAA);
    }

    private void renderMonitor(PoseStack poseStack, int x, int y) {
        // ... (モニター表示は変更なし)
        // 必要ならここに「Syntax Error」などの簡易表示を追加できます
        List<String> code = codeInputs.stream().map(EditBox::getValue).collect(Collectors.toList());
        SpellContext result = MagicScriptEngine.simulate(code);
        this.font.draw(poseStack, "Cost: " + result.cost, x, y, 0xFF55FFFF);
        this.font.draw(poseStack, "Pwr: " + result.science.energy, x, y + 12, 0xFFFFAA00);
    }
}
