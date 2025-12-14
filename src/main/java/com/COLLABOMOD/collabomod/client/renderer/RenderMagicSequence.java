package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
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

public class RenderMagicSequence extends EntityRenderer<EntityMagicSequence> {

    private static final ResourceLocation BLANK_TEXTURE = new ResourceLocation("textures/particle/glitter_0.png");

    public RenderMagicSequence(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSequence entity) {
        return BLANK_TEXTURE;
    }

    @Override
    public void render(EntityMagicSequence entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        VisualMetadata meta = entity.getVisualMetadata();
        Vector3f color = entity.getColor();
        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F);

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        // 1. ベースリング
        drawProceduralRing(poseStack, builder, 1.5F * progress, 0.05F, color, meta, time);

        // ■ 追加: 魔法陣の「中身（幾何学模様）」を描く
        // レイヤー数に応じて頂点数を変える（3角形, 4角形, 5芒星...）
        int vertices = 2 + meta.layerCount;
        drawProceduralPattern(poseStack, builder, 1.2F * progress, color, vertices, time * 2.0F);

        // 2. 多重レイヤー
        for (int i = 1; i <= meta.layerCount; i++) {
            float scale = (1.5F * progress) * (float)Math.pow(0.7, i);
            float rotSpeed = (i % 2 == 0 ? 1.0F : -1.0F) * (1.0F + i * 2.0F);

            Vector3f layerColor = new Vector3f(
                    Math.min(1.0F, color.x() + i * 0.1F),
                    Math.min(1.0F, color.y() + i * 0.1F),
                    Math.min(1.0F, color.z() + i * 0.1F)
            );

            drawProceduralRing(poseStack, builder, scale, 0.03F, layerColor, meta, time * rotSpeed);
        }

        poseStack.popPose();
    }

    // リング描画
    private void drawProceduralRing(PoseStack poseStack, VertexConsumer builder, float radius, float width, Vector3f color, VisualMetadata meta, float rotation) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int segments = 60;
        float angleStep = (float) (2 * Math.PI / segments);

        for (int i = 0; i < segments; i++) {
            float theta = i * angleStep;
            float nextTheta = (i + 1) * angleStep;
            float r = radius;

            if (meta.isWavy) {
                float freq = 6.0F; float amp = 0.1F;
                r += (float)Math.sin(theta * freq + rotation * 0.2F) * amp;
            }

            float rotRad = (float)Math.toRadians(rotation * 2.0F);
            float x1 = (float)Math.cos(theta + rotRad) * r;
            float y1 = (float)Math.sin(theta + rotRad) * r;
            float x2 = (float)Math.cos(nextTheta + rotRad) * r;
            float y2 = (float)Math.sin(nextTheta + rotRad) * r;

            // 表
            addVertex(builder, pose, normal, x1, y1, 0, 0, 0, color, 0.8F);
            addVertex(builder, pose, normal, x2, y2, 0, 0, 0, color, 0.8F);
            addVertex(builder, pose, normal, x2 * (1-width), y2 * (1-width), 0, 0, 1, color, 0.8F);
            addVertex(builder, pose, normal, x1 * (1-width), y1 * (1-width), 0, 0, 1, color, 0.8F);

            // ■ 追加: 裏面描画 (カリング対策)
            addVertex(builder, pose, normal, x1 * (1-width), y1 * (1-width), 0, 0, 1, color, 0.8F);
            addVertex(builder, pose, normal, x2 * (1-width), y2 * (1-width), 0, 0, 1, color, 0.8F);
            addVertex(builder, pose, normal, x2, y2, 0, 0, 0, color, 0.8F);
            addVertex(builder, pose, normal, x1, y1, 0, 0, 0, color, 0.8F);
        }
    }

    // ■ 新規追加: 幾何学模様描画 (ポリゴン/星)
    private void drawProceduralPattern(PoseStack poseStack, VertexConsumer builder, float radius, Vector3f color, int vertices, float rotation) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float angleStep = (float) (2 * Math.PI / vertices);
        float rotRad = (float)Math.toRadians(rotation);

        for (int i = 0; i < vertices; i++) {
            float theta = i * angleStep + rotRad;
            float nextTheta = (i + 1) * angleStep + rotRad;

            // 頂点の計算
            float x1 = (float)Math.cos(theta) * radius;
            float y1 = (float)Math.sin(theta) * radius;
            float x2 = (float)Math.cos(nextTheta) * radius;
            float y2 = (float)Math.sin(nextTheta) * radius;

            // 線を描く（中心から外へ、ではなく外周を結ぶ）
            // 星形にするならここで頂点を飛ばすロジックを入れるが、まずはシンプルに多角形
            // ラインの太さを出すために細い四角形を描く

            // 中心と結ぶライン (スポーク)
            float w = 0.05F; // 太さ

            // 表
            addVertex(builder, pose, normal, 0, 0, 0, 0, 0, color, 0.6F);
            addVertex(builder, pose, normal, x1, y1, 0, 1, 1, color, 0.0F); // 先端は透明
            addVertex(builder, pose, normal, x2, y2, 0, 1, 1, color, 0.0F);
            addVertex(builder, pose, normal, 0, 0, 0, 0, 0, color, 0.6F);

            // 裏
            addVertex(builder, pose, normal, 0, 0, 0, 0, 0, color, 0.6F);
            addVertex(builder, pose, normal, x2, y2, 0, 1, 1, color, 0.0F);
            addVertex(builder, pose, normal, x1, y1, 0, 1, 1, color, 0.0F);
            addVertex(builder, pose, normal, 0, 0, 0, 0, 0, color, 0.6F);
        }
    }

    private void addVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, Vector3f c, float a) {
        builder.vertex(pose, (float)x, (float)y, (float)z)
                .color(c.x(), c.y(), c.z(), a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240)
                .normal(normal, 0, 0, 1)
                .endVertex();
    }
}