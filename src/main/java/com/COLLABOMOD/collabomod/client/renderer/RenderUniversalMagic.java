package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.util.GeometryHelper;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.science.PhenomenonType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
    public void render(EntitySciencePhenomenon entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        VisualMetadata meta = entity.getVisualMetadata(); // 自動計算されたメタデータ
        Vector3f color = entity.getColor();
        float radius = entity.getRadius();
        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F);

        poseStack.pushPose();

        // 向き合わせ
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        if (meta.shape == EnumMagicShape.SPHERE) {
            float baseScale = radius * 1.1F * progress;
            if (meta.rendererID.equals("material_burst")) {
                GeometryHelper.drawSphere(poseStack, builder, baseScale * 0.5F, new Vector3f(1,1,1), 0.9F, time * 0.2F);
                GeometryHelper.drawSphere(poseStack, builder, baseScale, color, 0.6F, -time * 0.1F);
            } else {
                GeometryHelper.drawSphere(poseStack, builder, baseScale, color, 0.5F, time * 0.1F);
            }
        }
        // ■ 追加: 円柱 (火柱)
        else if (meta.shape == EnumMagicShape.CYLINDER) {
            // 半径は少し小さめ、高さは半径の4倍程度
            float r = radius * 0.8F * progress;
            float h = radius * 4.0F * progress;

            // コア（白）
            GeometryHelper.drawCylinder(poseStack, builder, r * 0.5F, h, new Vector3f(1,1,1), 0.8F, time * 0.5F);
            // 外側（炎の色）
            GeometryHelper.drawCylinder(poseStack, builder, r, h * 0.9F, color, 0.5F, time * 0.2F);
        }
        else {
            float scale = 1.5F * progress;
            GeometryHelper.drawRing(poseStack, builder, scale, 0.1F, color, 0.8F, time * 2.0F);
        }

        poseStack.popPose();
    }
}