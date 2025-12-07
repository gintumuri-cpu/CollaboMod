package com.COLLABOMOD.collabomod.client.gui;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketInstallSpell;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MagicConsoleScreen extends AbstractContainerScreen<MagicConsoleMenu> {

    // 背景テクスチャ（とりあえずディスペンサーのGUIを流用）
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/dispenser.png");

    public MagicConsoleScreen(MagicConsoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        // ボタンの追加 (x, y, width, height, text, action)
        // 座標はGUIの左上基準 + オフセット
        this.addRenderableWidget(new Button(this.leftPos + 135, this.topPos + 33, 30, 20, new TextComponent("Set"), button -> {
            // パケット送信
            NetworkHandler.INSTANCE.sendToServer(new PacketInstallSpell());
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.blit(poseStack, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.blit(poseStack, x + 25, y + 34, 79, 16, 18, 18);
    }
}
