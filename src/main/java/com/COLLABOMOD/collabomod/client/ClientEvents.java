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
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW; // マウス入力検知用
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.lang.reflect.Method;
import java.util.Random;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");

    public static boolean isElementalSightActive = false;
    public static boolean isThirdEyeMode = false;

    private static int sightTimer = 0;
    private static final int MAX_DURATION = 400;
    private static ArmorStand dummyCamera = null;

    // --- キー入力 ---
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        if (KeyInit.ELEMENTAL_SIGHT_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            toggleElementalSight(mc, false);
        }
    }

    public static void toggleThirdEyeMode() {
        Minecraft mc = Minecraft.getInstance();
        if (isElementalSightActive && !isThirdEyeMode) {
            disableElementalSight(mc);
        }
        toggleElementalSight(mc, true);
    }

    private static void toggleElementalSight(Minecraft mc, boolean thirdEye) {
        if (mc.player == null || mc.level == null) return;

        if (!isElementalSightActive) {
            isThirdEyeMode = thirdEye;
            startElementalSight(mc);
        } else {
            disableElementalSight(mc);
        }
    }

    private static void startElementalSight(Minecraft mc) {
        isElementalSightActive = true;
        sightTimer = MAX_DURATION;

        dummyCamera = new ArmorStand(EntityType.ARMOR_STAND, mc.level);

        double eyeHeight = mc.player.getEyeY() - dummyCamera.getEyeHeight();
        Vec3 look = mc.player.getLookAngle();
        double startX = mc.player.getX() + look.x * 0.5;
        double startY = eyeHeight + look.y * 0.5;
        double startZ = mc.player.getZ() + look.z * 0.5;

        dummyCamera.setPos(startX, startY, startZ);
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

        String msg = isThirdEyeMode
                ? "§c[サード・アイ] 照準シークエンス起動 - 発動点を視認して[攻撃]キー"
                : "§b[情報体次元] 視覚連結開始 - 自由視点モード";
        mc.player.displayClientMessage(new TextComponent(msg), true);
    }

    private static void disableElementalSight(Minecraft mc) {
        isElementalSightActive = false;
        isThirdEyeMode = false;
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

    // --- マウス入力（左クリック） ---
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (isElementalSightActive && isThirdEyeMode && dummyCamera != null) {
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {

                HitResult result = dummyCamera.pick(300.0D, 0.0F, false);

                if (result.getType() != HitResult.Type.MISS) {
                    BlockPos targetPos = new BlockPos(result.getLocation());
                    NetworkHandler.INSTANCE.sendToServer(new PacketMaterialBurst(targetPos));
                    disableElementalSight(mc);
                    mc.options.keyAttack.setDown(false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isElementalSightActive) {
            event.setCanceled(true);
        }
    }

    // ■■■ 修正: 距離に応じた画面揺れ（Camera Shake） ■■■
    /*
    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        EntityMaterialBurst burst = null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof EntityMaterialBurst b) {
                burst = b;
                break;
            }
        }

        if (burst != null) {
            float energy = burst.getEnergy();
            double dist = burst.distanceTo(mc.player);

            // 揺れの影響範囲: 500ブロック
            double maxShakeDist = 500.0D;

            if (energy > 10.0F && dist < maxShakeDist) {
                // 距離減衰: 近いほど激しく、遠いほど緩やかに
                double distFactor = 1.0D - (dist / maxShakeDist);
                // 2乗することで「近くで急激に強くなる」演出にする
                distFactor = distFactor * distFactor;

                float intensity = (float) (energy * 0.05F * distFactor);

                Random rand = new Random();
                float shakeX = (rand.nextFloat() - 0.5F) * intensity;
                float shakeY = (rand.nextFloat() - 0.5F) * intensity;

                event.setYaw(event.getYaw() + shakeX);
                event.setPitch(event.getPitch() + shakeY);
            }
        }
    }

     */


    // --- Tick処理 ---
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || mc.isPaused()) return;

            handleHeartbeat(player);

            if (isElementalSightActive && dummyCamera != null) {
                sightTimer--;

                dummyCamera.setYRot(player.getYRot());
                dummyCamera.yRotO = player.yRotO;
                dummyCamera.setYHeadRot(player.getYHeadRot());
                dummyCamera.yHeadRotO = player.yHeadRotO;
                dummyCamera.setXRot(player.getXRot());
                dummyCamera.xRotO = player.xRotO;

                handleCameraMovement(mc);

                if (sightTimer < 60 && sightTimer % 10 == 0) {
                    player.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 0.5F, 0.5F + (60 - sightTimer) / 20.0F);
                }
                if (sightTimer <= 0) {
                    player.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.5F);
                    disableElementalSight(mc);
                }
            }
        }
    }

    private static void handleCameraMovement(Minecraft mc) {
        if (dummyCamera == null) return;
        float speed = 1.5F;
        if (mc.options.keySprint.isDown()) speed = 4.0F;

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
                /*
                if (!isThirdEyeMode && sightTimer < 60) {
                    drawNoiseOverlay(event.getMatrixStack(), mc, (60 - sightTimer));
                }
                 */
            } else {
                drawStressOverlay(event.getMatrixStack(), mc, player);
            }

            // ■■■ 修正: 距離に応じた砂嵐ノイズ ■■■
            /*
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof EntityMaterialBurst burst) {
                    float energy = burst.getEnergy();
                    // ノイズの影響範囲: 300ブロック
                    double maxNoiseDist = 300.0D;
                    double dist = burst.distanceTo(mc.player);

                    if (energy > 50.0F && dist < maxNoiseDist) {
                        // 距離減衰 (Linear)
                        double distFactor = 1.0D - (dist / maxNoiseDist);
                        // 0未満にならないように制限
                        if (distFactor < 0) distFactor = 0;

                        // エネルギーと距離を掛け合わせて不透明度を決定
                        int noiseAlpha = (int) (Math.min(200, energy * 0.5F) * distFactor);

                        if (noiseAlpha > 5) { // 薄すぎる場合は描画しない
                            drawStaticNoise(event.getMatrixStack(), mc, noiseAlpha);
                        }
                        break;
                    }
                }
            }
             */

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isHoldingCAD = (mainHand.getItem() instanceof ItemCAD || mainHand.getItem() instanceof ItemSilverHorn || mainHand.getItem() instanceof ItemThirdEye) ||
                    (offHand.getItem() instanceof ItemCAD || offHand.getItem() instanceof ItemSilverHorn || offHand.getItem() instanceof ItemThirdEye);

            if (isHoldingCAD) {
                PoseStack poseStack = event.getMatrixStack();
                poseStack.pushPose();
                poseStack.translate(0, 0, 200); // Zを200手前にずらす（確実に最前面へ）
                drawPsionOverlay(poseStack, mc, player);
                poseStack.popPose();
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
        /*
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        float alpha = (intensity / 60.0F) * 0.6F;
        if (mc.level.random.nextFloat() < alpha) {
            int color = 0x50FF0000;
            GuiComponent.fill(poseStack, 0, 0, width, height, color);
        }
         */
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
            RenderSystem.disableDepthTest();
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

    private static void drawStaticNoise(PoseStack poseStack, Minecraft mc, int alpha) {
        /*
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        Random rand = new Random();
        int color = (alpha << 24) | 0x808080;
        GuiComponent.fill(poseStack, 0, 0, width, height, color);
        for (int i = 0; i < 20; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int w = rand.nextInt(50) + 10;
            int h = rand.nextInt(5) + 1;
            int noiseColor = (rand.nextInt(100) + 100) << 24 | 0xFFFFFF;
            GuiComponent.fill(poseStack, x, y, x + w, y + h, noiseColor);
        }
         */
    }
}