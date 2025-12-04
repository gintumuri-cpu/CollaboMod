package com.COLLABOMOD.collabomod.client;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import com.COLLABOMOD.collabomod.item.ItemThirdEye;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketMaterialBurst;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
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
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

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
                ? "§c[サード・アイ] 照準シークエンス起動"
                : "§b[情報体次元] 視覚連結開始";
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

    // --- マウス入力 ---
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

    // --- 演出設定 ---
    @SubscribeEvent
    public static void onComputeFogColor(EntityViewRenderEvent.FogColors event) {
        if (isElementalSightActive) {
            event.setRed(0.0F);
            event.setGreen(0.05F);
            event.setBlue(0.15F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (isElementalSightActive) {
            RenderSystem.setShaderFogStart(-6.0F);
            RenderSystem.setShaderFogEnd(80.0F);
        }
    }

    // ■ 修正: RenderLevelStageEvent を使用してワイヤーフレームを描画
    // ステージは AFTER_TRANSLUCENT_BLOCKS (半透明ブロックの後) を指定
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (isElementalSightActive && event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            Minecraft mc = Minecraft.getInstance();
            PoseStack poseStack = event.getPoseStack();
            Vec3 camPos = event.getCamera().getPosition();

            renderIdeaGrid(poseStack, camPos);
        }
    }

    private static void renderIdeaGrid(PoseStack poseStack, Vec3 camPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // DepthTest無効化（壁を透視して線を表示）
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(1.7F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float rF = 0.0F; float gF = 0.8F; float bF = 1.0F; float aF = 0.4F;
        if (isThirdEyeMode) {
            rF = 1.0F; gF = 0.2F; bF = 0.2F;
        }
        int r = (int)(rF * 255.0F); int g = (int)(gF * 255.0F); int b = (int)(bF * 255.0F); int a = (int)(aF * 255.0F);

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        int range = 10;
        BlockPos center = new BlockPos(camPos);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    if (mc.level.isEmptyBlock(mutablePos)) continue;

                    VoxelShape shape = mc.level.getBlockState(mutablePos).getShape(mc.level, mutablePos);
                    if (shape.isEmpty()) continue;

                    shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                        buffer.vertex(poseStack.last().pose(), (float)(mutablePos.getX() + x1), (float)(mutablePos.getY() + y1), (float)(mutablePos.getZ() + z1))
                                .color(r, g, b, a).endVertex();
                        buffer.vertex(poseStack.last().pose(), (float)(mutablePos.getX() + x2), (float)(mutablePos.getY() + y2), (float)(mutablePos.getZ() + z2))
                                .color(r, g, b, a).endVertex();
                    });
                }
            }
        }

        tesselator.end();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
    }

    // --- カメラ揺れ ---
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
            if (energy > 10.0F && dist < 500.0D) {
                double distFactor = 1.0D - (dist / 500.0D);
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
            input.forwardImpulse = 0; input.leftImpulse = 0; input.jumping = false; input.shiftKeyDown = false;
        }
    }
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<LivingEntity, ?> event) {
        if (isElementalSightActive && dummyCamera != null) {
            if (event.getEntity() != Minecraft.getInstance().player && event.getEntity() != dummyCamera) {
                if (dummyCamera.distanceTo(event.getEntity()) < 100) event.getEntity().setGlowingTag(true);
            }
        }
    }
    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<LivingEntity, ?> event) {
        if (isElementalSightActive && !event.getEntity().hasEffect(MobEffects.GLOWING)) event.getEntity().setGlowingTag(false);
    }
    @SubscribeEvent
    public static void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null || player.isSpectator()) return;

            if (isElementalSightActive) {
                drawInformationWorldOverlay(event.getMatrixStack(), mc);
            } else {
                drawStressOverlay(event.getMatrixStack(), mc, player);
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isHoldingCAD = (mainHand.getItem() instanceof ItemCAD || mainHand.getItem() instanceof ItemSilverHorn) ||
                    (offHand.getItem() instanceof ItemCAD || offHand.getItem() instanceof ItemSilverHorn) ||
                    (mainHand.getItem() instanceof ItemThirdEye || offHand.getItem() instanceof ItemThirdEye);

            if (isHoldingCAD) {
                PoseStack poseStack = event.getMatrixStack();
                poseStack.pushPose();
                poseStack.translate(0, 0, 200);
                drawPsionOverlay(poseStack, mc, player);
                poseStack.popPose();
            }
        }
    }

    private static void handleHeartbeat(Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            if (stats.getMentalLoad() > 60 && player.tickCount % Math.max(5, 20 - ((stats.getMentalLoad() - 60) / 3)) == 0)
                player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM, 1.0F, 0.5F);
        });
    }
    private static void drawInformationWorldOverlay(PoseStack poseStack, Minecraft mc) {
        int width = mc.getWindow().getGuiScaledWidth(); int height = mc.getWindow().getGuiScaledHeight();
        int color = isThirdEyeMode ? 0x40FF0000 : 0x400088FF;
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false); RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader); RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.fill(poseStack, 0, 0, width, height, color);
        RenderSystem.depthMask(true); RenderSystem.enableDepthTest();
    }
    private static void drawStressOverlay(PoseStack poseStack, Minecraft mc, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int stress = stats.getMentalLoad();
            if (stress > 50) {
                int width = mc.getWindow().getGuiScaledWidth(); int height = mc.getWindow().getGuiScaledHeight();
                float alpha = (float) (stress - 50) / 100.0F; float pulse = (float) Math.sin(player.tickCount * 0.2) * 0.1F; alpha += pulse;
                if (alpha > 0) {
                    int alphaHex = (int)(Math.min(alpha, 0.4F) * 255) << 24; int color = alphaHex | 0x550000;
                    GuiComponent.fill(poseStack, 0, 0, width, height, color);
                }
            }
        });
    }
    private static void drawPsionOverlay(PoseStack poseStack, Minecraft mc, Player player) {
        player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
            int current = stats.getCurrentPsion(); int max = stats.getMaxPsion(); if (max <= 0) max = 1;
            int width = mc.getWindow().getGuiScaledWidth(); int height = mc.getWindow().getGuiScaledHeight();
            int x = width - 120; int y = height - 40;
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader); RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GUI_ICONS);
            String text = "Psion: " + current + " / " + max; int color = 0x40E0D0;
            GuiComponent.drawString(poseStack, mc.font, text, x, y - 10, color);
            int barWidth = 100; int filledWidth = (int) (((float) current / max) * barWidth);
            GuiComponent.fill(poseStack, x, y, x + barWidth, y + 5, 0xFF555555);
            GuiComponent.fill(poseStack, x, y, x + filledWidth, y + 5, 0xFF00FFFF);
            RenderSystem.enableDepthTest();
        });
    }
}