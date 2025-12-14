package com.COLLABOMOD.collabomod.client.util;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class GeometryHelper {

    public static void drawProceduralMesh(PoseStack poseStack, VertexConsumer builder, VisualMetadata meta, int shapeType, float time, float alphaMultiplier) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        int stacks = 24; // 解像度を戻して滑らかに
        int slices = 24;

        float rBase = meta.scale;
        Vector3f color = meta.mainColor;

        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks;
            float v1 = (float) (i + 1) / stacks;

            for (int j = 0; j < slices; j++) {
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;

                // 時間経過をゆっくりにする (* 0.05 -> * 0.02)
                float t = time * 0.02f;

                Vector3f p0 = calculateVertex(u0, v0, meta, shapeType, t, rBase);
                Vector3f p1 = calculateVertex(u1, v0, meta, shapeType, t, rBase);
                Vector3f p2 = calculateVertex(u1, v1, meta, shapeType, t, rBase);
                Vector3f p3 = calculateVertex(u0, v1, meta, shapeType, t, rBase);

                // 明滅（スクロールによる色のちらつき）を抑制
                float scroll = t * 0.1f;

                // アルファ値は固定（明滅させない）
                float a = alphaMultiplier;

                addVertex(builder, pose, normal, p0, u0 + scroll, v0, color, a);
                addVertex(builder, pose, normal, p1, u1 + scroll, v0, color, a);
                addVertex(builder, pose, normal, p2, u1 + scroll, v1, color, a);
                addVertex(builder, pose, normal, p3, u0 + scroll, v1, color, a);
            }
        }
    }

    private static Vector3f calculateVertex(float u, float v, VisualMetadata meta, int shapeType, float time, float rBase) {
        double theta = u * Math.PI * 2.0;
        double phi = v * Math.PI;

        double x = 0, y = 0, z = 0;

        if (shapeType == 0) { // SPHERE
            double sinPhi = Math.sin(phi);
            x = Math.cos(theta) * sinPhi;
            y = Math.cos(phi);
            z = Math.sin(theta) * sinPhi;
        }
        else if (shapeType == 1) { // CYLINDER
            x = Math.cos(theta);
            z = Math.sin(theta);
            y = (v - 0.5) * 3.0; // 高さは3倍程度に留める

            // ねじれもマイルドに
            if (meta.rotationSpeed != 0) {
                double twist = y * meta.rotationSpeed * 0.2;
                double tx = x * Math.cos(twist) - z * Math.sin(twist);
                double tz = x * Math.sin(twist) + z * Math.cos(twist);
                x = tx; z = tz;
            }
        }
        else if (shapeType == 2) { // RING
            double r = 1.0 + (v - 0.5) * 0.2;
            x = Math.cos(theta) * r;
            z = Math.sin(theta) * r;
            y = (v - 0.5) * 0.1;
        }

        // 変形 (Deformation) - 係数を下げて落ち着かせる
        double displacement = 0.0;

        if (meta.isWavy) {
            // ゆっくり波打つ
            displacement += Math.sin(theta * 4.0 + time) * 0.05;
            displacement += Math.cos(phi * 4.0 + time) * 0.05;
        }

        if (meta.isSpiky) {
            // トゲも控えめに
            double spikes = 4.0 + meta.layerCount;
            double spikeVal = Math.abs(Math.sin(theta * spikes + time * 0.5));
            if (spikeVal > 0.7) displacement += 0.15; // 鋭さを維持しつつ飛び出しすぎない
        }

        // 脈動もゆっくり小さく
        double pulse = Math.sin(time * 0.5) * 0.05;

        double rFinal = rBase * (1.0 + displacement + pulse);

        return new Vector3f((float)(x * rFinal), (float)(y * rFinal), (float)(z * rFinal));
    }

    private static void addVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, Vector3f p, float u, float v, Vector3f c, float a) {
        float nx = p.x(); float ny = p.y(); float nz = p.z();
        float len = (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len > 0) { nx/=len; ny/=len; nz/=len; }

        builder.vertex(pose, p.x(), p.y(), p.z())
                .color(c.x(), c.y(), c.z(), a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }
}