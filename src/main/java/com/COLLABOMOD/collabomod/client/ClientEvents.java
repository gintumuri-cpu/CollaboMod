package com.COLLABOMOD.collabomod.client;
import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)

public class ClientEvents {
    private static final ResourceLocation GUI_ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    // ノイズ演出用のテクスチャ（今回は仮でポータルなどの既存テクスチャを流用するか、色塗りで対応）
    // 本来は専用のノイズ画像を用意するのがベスト

    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || player.isSpectator()) return;

            // ■ 変更点1: CADを持っているかチェック
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isHoldingCAD = (mainHand.getItem() instanceof ItemCAD || mainHand.getItem() instanceof ItemSilverHorn) ||
                    (offHand.getItem() instanceof ItemCAD || offHand.getItem() instanceof ItemSilverHorn);

            // CADを持っている時のみHUDを表示
            if (isHoldingCAD) {
                drawPsionOverlay(event.getMatrixStack(), mc, player);
            }

            // ■ 変更点2: 精神負荷による画面ノイズ演出
            // CADを持っていなくても、負荷が高ければ見えるようにする
            drawStressOverlay(event.getMatrixStack(), mc, player);
        }
    }

    // サイオンゲージ描画（引数を少し整理）
    private static void drawPsionOverlay(PoseStack poseStack, Minecraft mc, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int current = stats.getCurrentPsion();
            int max = stats.getMaxPsion();
            if (max <= 0) max = 1;

            int width = mc.getWindow().getGuiScaledWidth();
            int height = mc.getWindow().getGuiScaledHeight();
            int x = width - 120;
            int y = height - 40;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GUI_ICONS);

            String text = "Psion: " + current + " / " + max;
            int color = 0x40E0D0;
            GuiComponent.drawString(poseStack, mc.font, text, x, y - 10, color);

            int barWidth = 100;
            int filledWidth = (int) (((float) current / max) * barWidth);
            GuiComponent.fill(poseStack, x, y, x + barWidth, y + 5, 0xFF555555);
            GuiComponent.fill(poseStack, x, y, x + filledWidth, y + 5, 0xFF00FFFF);
        });
    }

    // ストレス時の画面オーバーレイ
    private static void drawStressOverlay(PoseStack poseStack, Minecraft mc, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int stress = stats.getMentalLoad();

            // ストレスが50を超えたら演出開始
            if (stress > 50) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();

                // 画面全体を赤く点滅させる（不透明度を計算）
                float alpha = (float) (stress - 50) / 100.0F; // 最大0.5くらい

                // 時間経過で明滅させる (Pulse)
                float pulse = (float) Math.sin(player.tickCount * 0.2) * 0.1F;
                alpha += pulse;

                if (alpha > 0) {
                    // 赤黒いオーバーレイを描画
                    // 0x(Alpha)(R)(G)(B)
                    int alphaHex = (int)(Math.min(alpha, 0.4F) * 255) << 24;
                    int color = alphaHex | 0x550000; // 赤黒

                    GuiComponent.fill(poseStack, 0, 0, width, height, color);
                }
            }
        });
    }

    // ■ 変更点3: 心拍音の再生（毎フレームではなくTickイベントで制御）
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            // プレイヤーが生きているかつ、ゲーム中のみ
            if (!mc.isPaused()) {
                player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                    int stress = stats.getMentalLoad();

                    // ストレスが高い時、定期的に心臓の音を鳴らす
                    if (stress > 60) {
                        // ストレスが高いほど間隔が短くなる
                        // 60以下: 鳴らない
                        // 60~80: ゆっくり
                        // 80~100: 早い
                        int interval = 20 - ((stress - 60) / 3);
                        if (interval < 5) interval = 5;

                        if (player.tickCount % interval == 0) {
                            // 低い音でドクン...と鳴らす（ベースドラムで代用）
                            player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM, 1.0F, 0.5F);
                        }
                    }
                });
            }
        }
    }
}
