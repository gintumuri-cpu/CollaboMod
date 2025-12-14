package com.COLLABOMOD.collabomod.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.Random;

public class GeometryHelper {

    private static final Random random = new Random();

    // ■ 球体（UVスクロール付き）の描画
    public static void drawSphere(PoseStack poseStack, VertexConsumer builder, float r, Vector3f color, float alpha, float uvOffset) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int stacks = 16;
        int slices = 16;

        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) (i - 1) / stacks);
            double z0 = Math.sin(lat0) * r;
            double zr0 = Math.cos(lat0) * r;
            double lat1 = Math.PI * (-0.5 + (double) i / stacks);
            double z1 = Math.sin(lat1) * r;
            double zr1 = Math.cos(lat1) * r;

            float v0 = (float) (i - 1) / stacks + uvOffset;
            float v1 = (float) i / stacks + uvOffset;

            for (int j = 0; j < slices; j++) {
                double lng = 2 * Math.PI * (double) (j - 1) / slices;
                double x = Math.cos(lng);
                double y = Math.sin(lng);
                double lngNext = 2 * Math.PI * (double) j / slices;
                double xNext = Math.cos(lngNext);
                double yNext = Math.sin(lngNext);

                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;

                addVertex(builder, pose, normal, x * zr0, y * zr0, z0, u0, v0, color, alpha);
                addVertex(builder, pose, normal, x * zr1, y * zr1, z1, u0, v1, color, alpha);
                addVertex(builder, pose, normal, xNext * zr1, yNext * zr1, z1, u1, v1, color, alpha);
                addVertex(builder, pose, normal, xNext * zr0, yNext * zr0, z0, u1, v0, color, alpha);
            }
        }
    }

    // ■ リングの描画
    public static void drawRing(PoseStack poseStack, VertexConsumer builder, float radius, float width, Vector3f color, float alpha, float rotation) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int segments = 24;

        // 回転適用は呼び出し元で行うか、ここで計算に含める
        // 今回は単純な円形配置
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments + Math.toRadians(rotation);
            double angle2 = 2 * Math.PI * (i + 1) / segments + Math.toRadians(rotation);

            float x1 = (float) Math.cos(angle1) * radius;
            float y1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float y2 = (float) Math.sin(angle2) * radius;

            float u1 = (float)i / segments;
            float u2 = (float)(i + 1) / segments;

            // 内径・外径
            float rIn = 1.0F - width;

            addVertex(builder, pose, normal, x1, y1, 0, u1, 0, color, alpha);
            addVertex(builder, pose, normal, x2, y2, 0, u2, 0, color, alpha);
            addVertex(builder, pose, normal, x2 * rIn, y2 * rIn, 0, u2, 1, color, alpha);
            addVertex(builder, pose, normal, x1 * rIn, y1 * rIn, 0, u1, 1, color, alpha);
        }
    }

    // ■ 稲妻の描画 (3次元)
    public static void drawLightning(PoseStack poseStack, VertexConsumer builder, Vector3f start, Vector3f end, Vector3f color, float alpha, int segments, float width) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        Vector3f diff = new Vector3f(end.x() - start.x(), end.y() - start.y(), end.z() - start.z());
        float length = (float)Math.sqrt(diff.dot(diff));

        // ステップごとのベクトル
        Vector3f step = new Vector3f(diff.x()/segments, diff.y()/segments, diff.z()/segments);
        Vector3f currentPos = new Vector3f(start.x(), start.y(), start.z());

        for (int j = 0; j < segments; j++) {
            Vector3f nextPos;
            if (j == segments - 1) {
                nextPos = end;
            } else {
                float progress = (float)j / segments;
                float jitter = length * width * (1.0F - progress);

                float jx = (random.nextFloat() - 0.5F) * jitter;
                float jy = (random.nextFloat() - 0.5F) * jitter;
                float jz = (random.nextFloat() - 0.5F) * jitter;

                nextPos = new Vector3f(
                        start.x() + step.x() * (j + 1) + jx,
                        start.y() + step.y() * (j + 1) + jy,
                        start.z() + step.z() * (j + 1) + jz
                );
            }
            // 線を描画
            addVertex(builder, pose, normal, currentPos.x(), currentPos.y(), currentPos.z(), 0, 0, color, alpha);
            addVertex(builder, pose, normal, nextPos.x(), nextPos.y(), nextPos.z(), 0, 0, color, alpha);

            currentPos = nextPos;
        }
    }

    public static void drawCylinder(PoseStack poseStack, VertexConsumer builder, float radius, float height, Vector3f color, float alpha, float time) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int segments = 16;

        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            // 側面 (Bottom to Top)
            // UVスクロールでエネルギーが昇る演出
            float v0 = time * 0.1F;
            float v1 = v0 + 1.0F;

            addVertex(builder, pose, normal, x1, 0, z1, 0, v1, color, alpha);
            addVertex(builder, pose, normal, x2, 0, z2, 1, v1, color, alpha);
            addVertex(builder, pose, normal, x2, height, z2, 1, v0, color, 0.0F); // 上端は透明にフェード
            addVertex(builder, pose, normal, x1, height, z1, 0, v0, color, 0.0F);
        }
    }

    private static void addVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, Vector3f c, float a) {
        builder.vertex(pose, (float)x, (float)y, (float)z)
                .color(c.x(), c.y(), c.z(), a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }
}
