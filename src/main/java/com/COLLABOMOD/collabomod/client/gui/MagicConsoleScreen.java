package com.COLLABOMOD.collabomod.client.gui;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.magic.MagicComponentType;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketEditCAD;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class MagicConsoleScreen extends AbstractContainerScreen<MagicConsoleMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    // 現在選択されているコンポーネントのリスト（画面表示用）
    // 初期値はすべて "NONE"
    private final MagicComponentType[] selectedComponents = new MagicComponentType[5];

    public MagicConsoleScreen(MagicConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        // 初期化
        for(int i=0; i<5; i++) selectedComponents[i] = null; // null = NONE

        // ■ プログラム行ボタンの配置 (5行)
        // x: GUI左端 + 60, y: 上から 20, 40, 60...
        for (int i = 0; i < 5; i++) {
            final int index = i;
            this.addRenderableWidget(new Button(this.leftPos + 60, this.topPos + 18 + (i * 22), 100, 20, new TextComponent("---"), button -> {
                // ボタンを押すとコンポーネントを切り替える
                cycleComponent(index);
                updateButtonText(button, index);
            }));
        }

        // ■ 書き込みボタン
        this.addRenderableWidget(new Button(this.leftPos + 70, this.topPos + 130, 80, 20, new TextComponent("INSTALL"), button -> {
            // 現在の構成をリスト化してサーバーへ送信
            List<String> list = new ArrayList<>();
            for (MagicComponentType comp : selectedComponents) {
                if (comp != null) {
                    list.add(comp.name());
                }
            }
            NetworkHandler.INSTANCE.sendToServer(new PacketEditCAD(list));
        }));
    }

    // コンポーネントを順番に切り替えるロジック
    private void cycleComponent(int index) {
        MagicComponentType current = selectedComponents[index];
        MagicComponentType[] allTypes = MagicComponentType.values();

        if (current == null) {
            // 最初はリストの先頭へ
            selectedComponents[index] = allTypes[0];
        } else {
            // 次の要素へ
            int nextOrdinal = current.ordinal() + 1;
            if (nextOrdinal >= allTypes.length) {
                selectedComponents[index] = null; // 一周したら無しに戻す
            } else {
                selectedComponents[index] = allTypes[nextOrdinal];
            }
        }
    }

    // ボタンの文字を更新
    private void updateButtonText(Button button, int index) {
        MagicComponentType comp = selectedComponents[index];
        if (comp == null) {
            button.setMessage(new TextComponent("---"));
        } else {
            // 分かりやすい名前に変換して表示
            String name = comp.name();
            // 例: PROJECTILE_AIR -> Air Projectile
            if (name.startsWith("ACT_")) button.setMessage(new TextComponent("§c[Act] " + name.substring(4)));
            else if (name.startsWith("ATTRIB_")) button.setMessage(new TextComponent("§b[Attr] " + name.substring(7)));
            else if (name.startsWith("MOD_")) button.setMessage(new TextComponent("§e[Mod] " + name.substring(4)));
            else button.setMessage(new TextComponent(name));
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);

        // ラベル描画
        this.font.draw(poseStack, "Magic Sequence:", this.leftPos + 60, this.topPos + 8, 0x404040);
        this.font.draw(poseStack, "CAD", this.leftPos + 26, this.topPos + 20, 0x404040);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // CADスロット枠（左側）
        this.blit(poseStack, x + 25, y + 34, 79, 16, 18, 18);
    }
}
