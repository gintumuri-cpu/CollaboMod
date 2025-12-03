package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

public class RenderMaterialBurst extends EntityRenderer<EntityMaterialBurst> {

    private final Random random = new Random();

    // バニラのビーコンビームのテクスチャを使用（きれいなエネルギー表現に最適）
    private static final ResourceLocation BEACON_BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");

    public RenderMaterialBurst(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMaterialBurst entity) {
        return BEACON_BEAM_LOCATION;
    }

    @Override
    public void render(EntityMaterialBurst entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();

        // 半径の計算
        float radius = entity.getRadius() + 0.2F * partialTicks; // expansionSpeedに合わせる(0.2F)
        float renderRadius = radius * 1.1F;

        // 時間経過（アニメーション用）
        float time = entity.tickCount + partialTicks;

        // ■■■ レイヤー1: 内部のエネルギーコア（シアン色・ゆっくり流れる） ■■■
        // entityTranslucent: 半透明でテクスチャ反映
        VertexConsumer beamBuilder = buffer.getBuffer(RenderType.entityTranslucent(BEACON_BEAM_LOCATION));

        poseStack.pushPose();
        // 少し回転させる
        poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 2.0F));
        // UVスクロール速度: time * 0.05
        drawTexturedSphere(poseStack, beamBuilder, renderRadius, 0.2F, 0.9F, 1.0F, 0.6F, time * 0.05F);
        poseStack.popPose();


        // ■■■ レイヤー2: 外部のエネルギーシェル（白っぽく・逆回転・高速） ■■■
        // 少し大きくする
        VertexConsumer shellBuilder = buffer.getBuffer(RenderType.entityTranslucent(BEACON_BEAM_LOCATION));

        poseStack.pushPose();
        // 逆回転
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-time * 5.0F));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(time * 3.0F));
        // 色は白く(R0.8, G0.9, B1.0)、透明度は薄く(0.3)、UVスクロールは速く
        drawTexturedSphere(poseStack, shellBuilder, renderRadius * 1.05F, 0.8F, 0.95F, 1.0F, 0.3F, -time * 0.1F);
        poseStack.popPose();


        // ■■■ レイヤー3: 稲妻エフェクト（バチバチ） ■■■
        if (radius > 2.0F) {
            VertexConsumer lightningBuilder = buffer.getBuffer(RenderType.lightning());

            // A. コアから表面へ突き刺さる雷 (太くて激しい)
            drawCoreBolts(poseStack, lightningBuilder, renderRadius);

            // B. 表面を這い回る雷 (細かくて数が多い)
            drawSurfaceArcs(poseStack, lightningBuilder, renderRadius);
        }

        poseStack.popPose();
    }

    // A. コアから表面へ突き刺さる雷
    private void drawCoreBolts(PoseStack poseStack, VertexConsumer builder, float radius) {
        Matrix4f pose = poseStack.last().pose();

        // 色: 純白に近いシアン
        float r = 0.5F; float g = 1.0F; float b = 1.0F; float a = 0.8F;

        // 本数: 10～20本（ランダム）
        int boltCount = 40 + random.nextInt(10);

        for (int i = 0; i < boltCount; i++) {
            poseStack.pushPose();

            // ランダムな方向へ回転
            float rotX = random.nextFloat() * 360.0F;
            float rotY = random.nextFloat() * 360.0F;
            poseStack.mulPose(Vector3f.XP.rotationDegrees(rotX));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(rotY));

            // 中心(0)から表面(radius)まで線を引く
            float segmentLength = radius / 6.0F; // 6分割
            float currentDist = 0.0F;

            float lastX = 0; float lastY = 0; float lastZ = 0;

            for (int j = 0; j < 6; j++) {
                currentDist += segmentLength;

                // ブレ幅（外側に行くほど激しくブレる）
                float jitter = (radius * 0.15F) * ((float)j / 6.0F);

                float nextX = (random.nextFloat() - 0.5F) * jitter;
                float nextY = currentDist; // Y軸方向に伸ばす（回転済みなので全方位になる）
                float nextZ = (random.nextFloat() - 0.5F) * jitter;

                // 線の描画
                builder.vertex(pose, lastX, lastY, lastZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
                builder.vertex(pose, nextX, nextY, nextZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();

                lastX = nextX; lastY = nextY; lastZ = nextZ;
            }
            poseStack.popPose();
        }
    }

    // B. 表面を這い回る雷（球体に沿ってバチバチする）
    private void drawSurfaceArcs(PoseStack poseStack, VertexConsumer builder, float radius) {
        Matrix4f pose = poseStack.last().pose();

        // 色: 少し青みを強く
        float r = 0.2F; float g = 0.8F; float b = 1.0F; float a = 0.6F;

        // 本数: 20本以上
        int arcCount = 40 + random.nextInt(10);

        for (int i = 0; i < arcCount; i++) {
            poseStack.pushPose();

            // ランダムな位置へ回転
            float rotX = random.nextFloat() * 360.0F;
            float rotY = random.nextFloat() * 360.0F;
            poseStack.mulPose(Vector3f.XP.rotationDegrees(rotX));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(rotY));

            // 「表面」付近で短いイナズマを描く
            // 半径の位置からスタート
            float startY = radius;

            float lastX = 0;
            float lastY = startY;
            float lastZ = 0;

            // 表面を這うように数ステップ進む
            for (int j = 0; j < 4; j++) {
                // 表面に沿うように移動（Yはあまり変えず、XZを動かす）
                float step = radius * 0.1F;
                float nextX = lastX + (random.nextFloat() - 0.5F) * step * 2.0F;
                float nextY = startY + (random.nextFloat() - 0.5F) * step * 0.5F; // 半径からあまり離れない
                float nextZ = lastZ + (random.nextFloat() - 0.5F) * step * 2.0F;

                builder.vertex(pose, lastX, lastY, lastZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
                builder.vertex(pose, nextX, nextY, nextZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();

                lastX = nextX; lastY = nextY; lastZ = nextZ;
            }
            poseStack.popPose();
        }
    }

    /**
     * テクスチャ付きの球体を描画するメソッド
     * UV座標を動かすことで「エネルギーが流れる」表現を行う
     */
    private void drawTexturedSphere(PoseStack poseStack, VertexConsumer builder, float r, float red, float green, float blue, float alpha, float uvOffset) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int stacks = 24; // 分割数を上げて滑らかに
        int slices = 24;

        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) (i - 1) / stacks);
            double z0 = Math.sin(lat0) * r;
            double zr0 = Math.cos(lat0) * r;
            double lat1 = Math.PI * (-0.5 + (double) i / stacks);
            double z1 = Math.sin(lat1) * r;
            double zr1 = Math.cos(lat1) * r;

            // テクスチャのV座標（縦方向）
            float v0 = (float) (i - 1) / stacks + uvOffset;
            float v1 = (float) i / stacks + uvOffset;

            for (int j = 0; j < slices; j++) {
                double lng = 2 * Math.PI * (double) (j - 1) / slices;
                double x = Math.cos(lng);
                double y = Math.sin(lng);

                double lngNext = 2 * Math.PI * (double) j / slices;
                double xNext = Math.cos(lngNext);
                double yNext = Math.sin(lngNext);

                // テクスチャのU座標（横方向）
                float u0 = (float) (j - 1) / slices;
                float u1 = (float) j / slices;

                // 頂点データ (Pos, Color, UV, Overlay, Light, Normal)
                addTexturedVertex(builder, pose, normal, x * zr0, y * zr0, z0, u0, v0, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, x * zr1, y * zr1, z1, u0, v1, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, xNext * zr1, yNext * zr1, z1, u1, v1, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, xNext * zr0, yNext * zr0, z0, u1, v0, red, green, blue, alpha);
            }
        }
    }

    private void addTexturedVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, float r, float g, float b, float a) {
        builder.vertex(pose, (float)x, (float)y, (float)z)
                .color(r, g, b, a)
                .uv(u, v) // UV座標を設定
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240) // 常に明るく(MAX Light)
                .normal(normal, (float)x, (float)y, (float)z)
                .endVertex();
    }

    // 稲妻描画（前回から変更なし）
    private void drawLightningArcs(PoseStack poseStack, VertexConsumer builder, float radius) {
        Matrix4f pose = poseStack.last().pose();
        float r = 0.8F; float g = 1.0F; float b = 1.0F; float a = 0.9F; // より白く輝くように調整

        int boltCount = 6 + random.nextInt(4); // 本数増量

        for (int i = 0; i < boltCount; i++) {
            poseStack.pushPose();

            float rotX = random.nextFloat() * 360.0F;
            float rotY = random.nextFloat() * 360.0F;
            poseStack.mulPose(Vector3f.XP.rotationDegrees(rotX));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(rotY));

            float currentDist = 0.0F;
            float step = radius / 8.0F;

            float lastX = 0;
            float lastY = 0;
            float lastZ = 0;

            for (int j = 0; j < 8; j++) {
                currentDist += step;

                float offset = (radius * 0.15F) * (1.0F - (float)j/8.0F);
                float nextX = (random.nextFloat() - 0.5F) * offset;
                float nextY = currentDist;
                float nextZ = (random.nextFloat() - 0.5F) * offset;

                // 雷も RenderType.lightning なので UV や Overlay は 0 でOK
                builder.vertex(pose, lastX, lastY, lastZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
                builder.vertex(pose, nextX, nextY, nextZ).color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();

                lastX = nextX;
                lastY = nextY;
                lastZ = nextZ;
            }
            poseStack.popPose();
        }
    }
}