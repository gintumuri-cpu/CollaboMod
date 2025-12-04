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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class RenderMaterialBurst extends EntityRenderer<EntityMaterialBurst> {

    private final Random random = new Random();
    private static final ResourceLocation BEACON_BEAM_LOCATION = new ResourceLocation("textures/entity/beacon_beam.png");

    // 稲妻管理リスト
    private final List<PlasmaBolt> bolts = new ArrayList<>();

    public RenderMaterialBurst(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMaterialBurst entity) {
        return BEACON_BEAM_LOCATION;
    }

    // ■ 重要: カリング無効化（球体が消えるのを防ぐ）
    @Override
    public boolean shouldRender(EntityMaterialBurst entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(EntityMaterialBurst entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();

        float radius = entity.getRadius() + 0.1F * partialTicks;
        float renderRadius = radius * 1.1F;
        float time = entity.tickCount + partialTicks;

        // エンティティから透明度を取得
        float globalAlpha = entity.getAlpha();

        // 1. エネルギーコア
        VertexConsumer beamBuilder = buffer.getBuffer(RenderType.entityTranslucent(BEACON_BEAM_LOCATION));
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 2.0F));
        drawTexturedSphere(poseStack, beamBuilder, renderRadius, 0.2F, 0.9F, 1.0F, 0.6F * globalAlpha, time * 0.05F);
        poseStack.popPose();

        // 2. エネルギーシェル
        VertexConsumer shellBuilder = buffer.getBuffer(RenderType.entityTranslucent(BEACON_BEAM_LOCATION));
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-time * 5.0F));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(time * 3.0F));
        drawTexturedSphere(poseStack, shellBuilder, renderRadius * 1.05F, 0.8F, 0.95F, 1.0F, 0.3F * globalAlpha, -time * 0.1F);
        poseStack.popPose();

        // 3. 持続型プラズマ稲妻
        if (radius > 2.0F) {
            updateBolts(radius);
            poseStack.pushPose();
            VertexConsumer lightningBuilder = buffer.getBuffer(RenderType.lightning());

            for (PlasmaBolt bolt : bolts) {
                // ここで globalAlpha を渡しています
                drawPersistentBolt(poseStack, lightningBuilder, bolt, radius, globalAlpha);
            }

            poseStack.popPose();
        } else {
            bolts.clear();
        }

        poseStack.popPose();
    }

    // --- 稲妻管理 ---
    private void updateBolts(float radius) {
        Iterator<PlasmaBolt> it = bolts.iterator();
        while (it.hasNext()) {
            PlasmaBolt bolt = it.next();
            bolt.age++;
            if (bolt.age > bolt.maxAge) {
                it.remove();
            } else {
                double drift = 0.05;
                bolt.direction = bolt.direction.add(
                        (random.nextDouble() - 0.5) * drift,
                        (random.nextDouble() - 0.5) * drift,
                        (random.nextDouble() - 0.5) * drift
                ).normalize();
            }
        }
        while (bolts.size() < 15) {
            bolts.add(new PlasmaBolt(random));
        }
    }

    // --- 稲妻描画 ---
    // ■ 修正箇所: 引数に float globalAlpha を追加しました
    private void drawPersistentBolt(PoseStack poseStack, VertexConsumer builder, PlasmaBolt bolt, float radius, float globalAlpha) {
        Matrix4f pose = poseStack.last().pose();

        float lifeRatio = 1.0F - ((float)bolt.age / bolt.maxAge);
        float r = 0.6F; float g = 0.95F; float b = 1.0F;

        // ■ ここで globalAlpha を使用してフェードアウト
        float a = 0.8F * lifeRatio * globalAlpha;

        Vec3 dir = bolt.direction;
        Vec3 endPos = dir.scale(radius);

        double distMult = 3.0D + random.nextDouble() * 2.0D;
        Vec3 startPos = dir.scale(radius * distMult);

        drawJaggedLine(builder, pose, startPos, endPos, r, g, b, a);
    }

    private void drawJaggedLine(VertexConsumer builder, Matrix4f pose, Vec3 start, Vec3 end, float r, float g, float b, float a) {
        int segments = 8;
        Vec3 diff = end.subtract(start);
        double totalDistance = diff.length();
        Vec3 step = diff.scale(1.0 / segments);
        Vec3 currentPos = start;

        for (int j = 0; j < segments; j++) {
            Vec3 nextPos;
            if (j == segments - 1) {
                nextPos = end;
            } else {
                double progress = (double)j / segments;
                double jitterScale = totalDistance * 0.1 * (1.0 - progress);
                double jx = (random.nextDouble() - 0.5) * jitterScale;
                double jy = (random.nextDouble() - 0.5) * jitterScale;
                double jz = (random.nextDouble() - 0.5) * jitterScale;
                nextPos = start.add(step.scale(j + 1)).add(jx, jy, jz);
            }

            builder.vertex(pose, (float)currentPos.x, (float)currentPos.y, (float)currentPos.z)
                    .color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();
            builder.vertex(pose, (float)nextPos.x, (float)nextPos.y, (float)nextPos.z)
                    .color(r, g, b, a).uv(0, 0).uv2(240, 240).normal(0, 1, 0).endVertex();

            currentPos = nextPos;
        }
    }

    // --- 球体描画 ---
    private void drawTexturedSphere(PoseStack poseStack, VertexConsumer builder, float r, float red, float green, float blue, float alpha, float uvOffset) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int stacks = 24; int slices = 24;

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
                float u0 = (float) (j - 1) / slices;
                float u1 = (float) j / slices;

                addTexturedVertex(builder, pose, normal, x * zr0, y * zr0, z0, u0, v0, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, x * zr1, y * zr1, z1, u0, v1, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, xNext * zr1, yNext * zr1, z1, u1, v1, red, green, blue, alpha);
                addTexturedVertex(builder, pose, normal, xNext * zr0, yNext * zr0, z0, u1, v0, red, green, blue, alpha);
            }
        }
    }

    private void addTexturedVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal, double x, double y, double z, float u, float v, float r, float g, float b, float a) {
        builder.vertex(pose, (float)x, (float)y, (float)z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240, 240).normal(normal, (float)x, (float)y, (float)z).endVertex();
    }

    // --- 内部クラス ---
    private static class PlasmaBolt {
        Vec3 direction;
        int age;
        int maxAge;

        public PlasmaBolt(Random rand) {
            double x = rand.nextGaussian();
            double y = rand.nextGaussian();
            double z = rand.nextGaussian();
            double norm = Math.sqrt(x*x + y*y + z*z);
            this.direction = new Vec3(x / norm, y / norm, z / norm);
            this.age = 0;
            this.maxAge = 10 + rand.nextInt(20);
        }
    }
}