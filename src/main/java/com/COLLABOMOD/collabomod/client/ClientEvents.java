package com.COLLABOMOD.collabomod.client;
import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)

public class ClientEvents {
    private static final ResourceLocation GUI_ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        //System.out.println("DEBUG: Event Fired");
        // 経験値バーなどが描画されるレイヤーと同じタイミングで描画
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            //System.out.println("DEBUG: ElementType ALL passed");
            drawPsionOverlay(event.getMatrixStack());
        }
    }

    private static void drawPsionOverlay(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // プレイヤーがいない、またはスペクテイターモードなら表示しない
        if (player == null || player.isSpectator()) return;

        // クライアント側のCapabilityデータを取得
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            //System.out.println("DEBUG: Rendering! Psion: " + stats.getCurrentPsion());
            // 1. データの取得
            int current = stats.getCurrentPsion();
            int max = stats.getMaxPsion();
            // ゼロ除算回避
            if (max <= 0) max = 1;

            // 2. 描画位置の計算（画面の右下あたり）
            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();

            int x = width - 120; // 右端から120px左
            int y = height - 40; // 下端から40px上

            // 3. 描画設定
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GUI_ICONS); // ここを自作画像に変えるとオリジナルの枠になります

            // 4. バーの背景（黒い枠など）を描画
            // 引数: (poseStack, x, y, u, v, width, height)
            // ここでは簡易的に文字とバーを描画します

            // テキスト表示 "Psion: 100 / 100"
            String text = "Psion: " + current + " / " + max;
            int color = 0x40E0D0; // ターコイズブルー (魔法科っぽい色)

            // 文字の描画 (影付き)
            GuiComponent.drawString(poseStack, mc.font, text, x, y - 10, color);

            // ゲージの描画
            int barWidth = 100; // バーの最大幅
            int filledWidth = (int) (((float) current / max) * barWidth);

            // 背景バー（グレー）
            GuiComponent.fill(poseStack, x, y, x + barWidth, y + 5, 0xFF555555);
            // 中身バー（水色）
            GuiComponent.fill(poseStack, x, y, x + filledWidth, y + 5, 0xFF00FFFF);
        });
    }
}
