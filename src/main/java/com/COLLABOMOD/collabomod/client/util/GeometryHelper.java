package com.COLLABOMOD.collabomod.client.util;

import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.Random;

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


    public static void drawLightning(PoseStack poseStack, VertexConsumer builder, VisualMetadata meta, float time, float radius) {
        Matrix4f pose = poseStack.last().pose();

        // シードを使って、毎フレーム同じ形状の稲妻が出ないようにしつつ、
        // ある程度持続性を持たせる（timeを離散的に使う）
        Random lightningRand = new Random((long) (time * 0.5f) + meta.hashCode());

        int count = 4 + meta.layerCount * 2; // レイヤー数が多いほど稲妻も増やす

        for (int i = 0; i < count; i++) {
            // 始点と終点の決定
            Vector3f start = new Vector3f(0, 0, 0); // 中心

            // ランダムな方向へ飛ばす
            Vector3f end = new Vector3f(
                    lightningRand.nextFloat() - 0.5f,
                    lightningRand.nextFloat() - 0.5f,
                    lightningRand.nextFloat() - 0.5f
            );
            end.normalize();
            end.mul(radius * (1.2f + lightningRand.nextFloat())); // 本体より少し大きく

            drawJaggedLine(builder, pose, start, end, meta.subColor, 0.8f, lightningRand);
        }
    }
    private static void drawJaggedLine(VertexConsumer builder, Matrix4f pose, Vector3f start, Vector3f end, Vector3f color, float alpha, Random rand) {
        int segments = 6;
        Vector3f currentPos = new Vector3f(start.x(), start.y(), start.z());

        // ベクトル計算
        float dx = end.x() - start.x();
        float dy = end.y() - start.y();
        float dz = end.z() - start.z();

        float stepX = dx / segments;
        float stepY = dy / segments;
        float stepZ = dz / segments;

        for (int j = 0; j < segments; j++) {
            Vector3f nextPos;
            if (j == segments - 1) {
                nextPos = end;
            } else {
                // ジッター（稲妻のギザギザ）
                float jitter = 0.3f;
                float jx = (rand.nextFloat() - 0.5f) * jitter;
                float jy = (rand.nextFloat() - 0.5f) * jitter;
                float jz = (rand.nextFloat() - 0.5f) * jitter;

                nextPos = new Vector3f(
                        start.x() + stepX * (j + 1) + jx,
                        start.y() + stepY * (j + 1) + jy,
                        start.z() + stepZ * (j + 1) + jz
                );
            }

            // 線を描画（太さを出すために少しずらして複数回描く等の工夫も可だが、まずはシンプルに）
            addVertex(builder, pose, null, currentPos, 0, 0, color, alpha);
            addVertex(builder, pose, null, nextPos, 1, 1, color, alpha);

            currentPos = nextPos;
        }
    }

    private static void addVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, Vector3f p, float u, float v, Vector3f c, float a) {
        // 法線がない場合は適当に上向きなどを設定
        float nx = 0, ny = 1, nz = 0;
        if (normal != null) {
            // 既存の計算
            float len = (float)Math.sqrt(p.x()*p.x() + p.y()*p.y() + p.z()*p.z());
            if(len > 0) { nx=p.x()/len; ny=p.y()/len; nz=p.z()/len; }
        }

        builder.vertex(pose, p.x(), p.y(), p.z())
                .color(c.x(), c.y(), c.z(), a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(240, 240)
                .normal(nx, ny, nz)
                .endVertex();
    }
}