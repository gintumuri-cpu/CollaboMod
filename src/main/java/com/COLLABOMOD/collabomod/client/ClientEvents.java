package com.COLLABOMOD.collabomod.client;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import com.COLLABOMOD.collabomod.item.ItemThirdEye;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketMaterialBurst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW; // マウス入力検知用

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");

    public static boolean isElementalSightActive = false;
    // ■ 追加: サード・アイによる起動モードかどうか
    public static boolean isThirdEyeMode = false;

    private static int sightTimer = 0;
    private static final int MAX_DURATION = 400;
    private static ArmorStand dummyCamera = null;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        // 通常のキー起動（Vキー）
        if (KeyInit.ELEMENTAL_SIGHT_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            toggleElementalSight(mc, false); // 通常モードで切替
        }
    }

    // ■ 追加: サード・アイから呼び出すメソッド
    public static void toggleThirdEyeMode() {
        Minecraft mc = Minecraft.getInstance();
        // 既に通常モードで起動中なら一旦切る
        if (isElementalSightActive && !isThirdEyeMode) {
            disableElementalSight(mc);
        }
        // サード・アイモードで切替
        toggleElementalSight(mc, true);
    }

    // 共通の切替ロジック
    private static void toggleElementalSight(Minecraft mc, boolean thirdEye) {
        if (mc.player == null || mc.level == null) return;

        if (!isElementalSightActive) {
            isThirdEyeMode = thirdEye; // モード設定
            startElementalSight(mc);
        } else {
            disableElementalSight(mc);
        }
    }

    private static void startElementalSight(Minecraft mc) {
        isElementalSightActive = true;
        sightTimer = MAX_DURATION;

        dummyCamera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);

        // 視点位置の調整
        double eyeHeight = mc.player.getEyeY() - dummyCamera.getEyeHeight();
        Vec3 look = mc.player.getLookAngle();
        double startX = mc.player.getX() + look.x * 0.5;
        double startY = eyeHeight + look.y * 0.5;
        double startZ = mc.player.getZ() + look.z * 0.5;

        dummyCamera.setPos(startX, startY, startZ);

        // 回転同期
        dummyCamera.setYRot(mc.player.getYRot());
        dummyCamera.setYHeadRot(mc.player.getYRot());
        dummyCamera.setXRot(mc.player.getXRot());
        dummyCamera.yRotO = mc.player.getYRot();
        dummyCamera.yHeadRotO = mc.player.getYRot();
        dummyCamera.xRotO = mc.player.getXRot();

        dummyCamera.setNoGravity(true);
        dummyCamera.setInvisible(true);

        try {
            Method method = net.minecraft.client.multiplayer.ClientLevel.class.getDeclaredMethod("addEntity", int.class, Entity.class);
            method.setAccessible(true);
            method.invoke(mc.level, dummyCamera.getId(), dummyCamera);
        } catch (Exception e) { e.printStackTrace(); }

        mc.setCameraEntity(dummyCamera);
        mc.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.5F, 1.5F);

        // モードに応じたメッセージ
        String msg = isThirdEyeMode
                ? "§c[サード・アイ] 照準シークエンス起動 - 発動点を視認して[攻撃]キー"
                : "§b[情報体次元] 視覚連結開始 - 自由視点モード";
        mc.player.displayClientMessage(new TextComponent(msg), true);
    }

    private static void disableElementalSight(Minecraft mc) {
        isElementalSightActive = false;
        isThirdEyeMode = false; // モードリセット
        sightTimer = 0;

        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.playSound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 0.5F);
            mc.player.displayClientMessage(new TextComponent("§7[情報体次元] 連結解除"), true);
        }

        if (dummyCamera != null) {
            dummyCamera.remove(Entity.RemovalReason.DISCARDED);
            dummyCamera = null;
        }
    }

    // ■ 追加: マウス入力（左クリック）検知
    // サード・アイモード中、左クリックで発動座標を決定する
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (isElementalSightActive && isThirdEyeMode && dummyCamera != null) {
            // 左クリックが押されたら (GLFW_MOUSE_BUTTON_LEFT = 0)
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {

                // 幽体カメラの視線の先のブロックを取得（最大距離300）
                HitResult result = dummyCamera.pick(300.0D, 0.0F, false);

                if (result.getType() != HitResult.Type.MISS) {
                    // 座標を取得
                    BlockPos targetPos = new BlockPos(result.getLocation());

                    // サーバーへパケット送信（発動要請）
                    NetworkHandler.INSTANCE.sendToServer(new PacketMaterialBurst(targetPos));

                    // エレメンタル・サイト終了
                    disableElementalSight(mc);

                    // クリックイベントを消費して、誤動作を防ぐ
                    event.setCanceled(true);
                }
            }
        }
    }

    // --- 以下、既存のイベント処理（変更なし） ---

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isElementalSightActive) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.isPaused()) return;

            handleHeartbeat(player);

            if (isElementalSightActive && dummyCamera != null) {
                sightTimer--;

                // 回転同期
                dummyCamera.setYRot(player.getYRot());
                dummyCamera.yRotO = player.yRotO;
                dummyCamera.setYHeadRot(player.getYHeadRot());
                dummyCamera.yHeadRotO = player.yHeadRotO;
                dummyCamera.setXRot(player.getXRot());
                dummyCamera.xRotO = player.xRotO;

                // カメラ移動 (WASD)
                handleCameraMovement(mc);

                // ノイズ・終了処理
                if (sightTimer < 60 && sightTimer % 10 == 0) {
                    player.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.5F, 0.5F + (60 - sightTimer) / 20.0F);
                }
                if (sightTimer <= 0) {
                    player.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.5F);
                    // タイムアウト時は通常解除メッセージ
                    disableElementalSight(mc);
                }
            }
        }
    }

    private static void handleCameraMovement(Minecraft mc) {
        if (dummyCamera == null) return;

        float speed = 0.5F;
        if (mc.options.keySprint.isDown()) speed = 1.0F;

        Vec3 lookVec = dummyCamera.getLookAngle();
        Vec3 rightVec = lookVec.cross(new Vec3(0, 1, 0)).normalize();

        double dx = 0; double dy = 0; double dz = 0;

        if (mc.options.keyUp.isDown()) { dx += lookVec.x * speed; dy += lookVec.y * speed; dz += lookVec.z * speed; }
        if (mc.options.keyDown.isDown()) { dx -= lookVec.x * speed; dy -= lookVec.y * speed; dz -= lookVec.z * speed; }
        if (mc.options.keyLeft.isDown()) { dx += rightVec.x * speed; dz += rightVec.z * speed; }
        if (mc.options.keyRight.isDown()) { dx -= rightVec.x * speed; dz -= rightVec.z * speed; }
        if (mc.options.keyJump.isDown()) dy += speed;
        if (mc.options.keyShift.isDown()) dy -= speed;

        dummyCamera.setPos(dummyCamera.getX() + dx, dummyCamera.getY() + dy, dummyCamera.getZ() + dz);
    }

    @SubscribeEvent
    public static void onInputUpdate(MovementInputUpdateEvent event) {
        if (isElementalSightActive) {
            net.minecraft.client.player.Input input = event.getInput();
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.jumping = false;
            input.shiftKeyDown = false;
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<LivingEntity, ?> event) {
        if (isElementalSightActive && dummyCamera != null) {
            Minecraft mc = Minecraft.getInstance();
            LivingEntity target = event.getEntity();
            if (target == mc.player || target == dummyCamera) return;
            if (dummyCamera.distanceTo(target) < 100) {
                target.setGlowingTag(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<LivingEntity, ?> event) {
        if (isElementalSightActive) {
            if (!event.getEntity().hasEffect(MobEffects.GLOWING)) {
                event.getEntity().setGlowingTag(false);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || player.isSpectator()) return;

            if (isElementalSightActive) {
                drawInformationWorldOverlay(event.getMatrixStack(), mc);
                // サード・アイモードならノイズは出さない（集中している演出）
                if (!isThirdEyeMode && sightTimer < 60) {
                    drawNoiseOverlay(event.getMatrixStack(), mc, (60 - sightTimer));
                }
            } else {
                drawStressOverlay(event.getMatrixStack(), mc, player);
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isHoldingCAD = (mainHand.getItem() instanceof ItemCAD || mainHand.getItem() instanceof ItemSilverHorn);
            // サード・アイを持っている時もHUDを表示
            if (isHoldingCAD || mainHand.getItem() instanceof ItemThirdEye || offHand.getItem() instanceof ItemThirdEye) {
                drawPsionOverlay(event.getMatrixStack(), mc, player);
            }
        }
    }

    private static void handleHeartbeat(Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int stress = stats.getMentalLoad();
            if (stress > 60) {
                int interval = 20 - ((stress - 60) / 3);
                if (interval < 5) interval = 5;
                if (player.tickCount % interval == 0) {
                    player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM, 1.0F, 0.5F);
                }
            }
        });
    }

    private static void drawInformationWorldOverlay(PoseStack poseStack, Minecraft mc) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        // サード・アイモードなら少し赤みがかった色にする（警告色）
        int color = isThirdEyeMode ? 0x40FF0000 : 0x400088FF;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.fill(poseStack, 0, 0, width, height, color);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static void drawNoiseOverlay(PoseStack poseStack, Minecraft mc, int intensity) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float alpha = (intensity / 60.0F) * 0.6F;
        if (mc.level.random.nextFloat() < alpha) {
            int color = 0x50FF0000;
            GuiComponent.fill(poseStack, 0, 0, width, height, color);
        }
    }

    private static void drawStressOverlay(PoseStack poseStack, Minecraft mc, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int stress = stats.getMentalLoad();
            if (stress > 50) {
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();
                float alpha = (float) (stress - 50) / 100.0F;
                float pulse = (float) Math.sin(player.tickCount * 0.2) * 0.1F;
                alpha += pulse;
                if (alpha > 0) {
                    int alphaHex = (int)(Math.min(alpha, 0.4F) * 255) << 24;
                    int color = alphaHex | 0x550000;
                    GuiComponent.fill(poseStack, 0, 0, width, height, color);
                }
            }
        });
    }

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
}