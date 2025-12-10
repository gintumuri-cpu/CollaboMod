package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.util.GeometryHelper;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RenderUniversalMagic extends EntityRenderer<EntitySciencePhenomenon> {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    public RenderUniversalMagic(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySciencePhenomenon entity) {
        return BEAM_TEXTURE;
    }

    @Override
    public boolean shouldRender(EntitySciencePhenomenon entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // 常に描画
    }

    @Override
    public void render(EntitySciencePhenomenon entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        String rendererID = entity.getRendererID();
        Vector3f color = entity.getColor();

        VisualMetadata meta = new VisualMetadata();
        meta.mainColor = color;

        // 簡易判定
        if (rendererID.equals("material_burst")) {
            meta.shape = EnumMagicShape.SPHERE;
            meta.hasLightning = true;
        } else if (rendererID.equals("shield_dome") || rendererID.equals("explosion_sphere")) {
            meta.shape = EnumMagicShape.SPHERE;
        } else {
            meta.shape = EnumMagicShape.RING;
        }

        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F);

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        // 発光描画
        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        if (meta.shape == EnumMagicShape.SPHERE) {
            float baseScale = 3.0F * progress;

            if (rendererID.equals("shield_dome")) {
                // シールド: 薄い殻
                GeometryHelper.drawSphere(poseStack, builder, baseScale, color, 0.4F, time * 0.05F);
                // 逆回転する内側
                GeometryHelper.drawSphere(poseStack, builder, baseScale * 0.9F, color, 0.2F, -time * 0.05F);
            }
            else if (rendererID.equals("material_burst")) {
                // マテリアルバースト:
                // 1. 濃密なコア
                GeometryHelper.drawSphere(poseStack, builder, baseScale * 0.5F, new Vector3f(1.0F, 1.0F, 1.0F), 0.9F, time * 0.2F);
                // 2. メインのエネルギー球 (色はエンティティ由来)
                GeometryHelper.drawSphere(poseStack, builder, baseScale, color, 0.6F, -time * 0.1F);
                // 3. 外側のオーラ
                GeometryHelper.drawSphere(poseStack, builder, baseScale * 1.2F, color, 0.2F, time * 0.05F);
            }
            else {
                // 通常爆発
                GeometryHelper.drawSphere(poseStack, builder, baseScale, color, 0.5F, time * 0.1F);
            }
        }
        else {
            float scale = 1.5F * progress;
            GeometryHelper.drawRing(poseStack, builder, scale, 0.1F, color, 0.8F, time * 2.0F);
            GeometryHelper.drawRing(poseStack, builder, scale * 0.6F, 0.1F, color, 0.6F, -time * 5.0F);
        }

        poseStack.popPose();
    }
}



//    private void addVertex(VertexConsumer builder, Matrix4f pose, float x, float y, float z, float u, float v, Vector3f color) {
//        builder.vertex(pose, x, y, z)
//                .color(color.x(), color.y(), color.z(), 0.8F) // Alpha
//                .uv(u, v)
//                .overlayCoords(OverlayTexture.NO_OVERLAY)
//                .uv2(255, 255) // ★修正: 最大輝度 (15, 15) -> (255, 255)
//                .normal(0, 1, 0)
//                .endVertex();
//    }
//}
//// 雷 (Lightning)
//        if (meta.hasLightning) {
//VertexConsumer lightningBuilder = buffer.getBuffer(RenderType.lightning());
//// 簡易的に数本出す
//float r = meta.scale * 20.0F * progress * 1.2F;
//Vector3f center = new Vector3f(0,0,0);
//            for(int i=0; i<5; i++) {
//        // ランダムな方向へ
//        // (GeometryHelperのdrawLightningを呼ぶ)
//        }
//        }