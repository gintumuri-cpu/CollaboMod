package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.util.GeometryHelper;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderMagicSequence extends EntityRenderer<EntityMagicSequence> {

    private static final ResourceLocation MAGIC_CIRCLE_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    public RenderMagicSequence(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSequence entity) {
        return MAGIC_CIRCLE_TEXTURE;
    }

    @Override
    public void render(EntityMagicSequence entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // 1. メタデータの取得
        VisualMetadata meta = entity.getVisualMetadata();
        if (meta == null) meta = new VisualMetadata();
        Vector3f color = meta.mainColor;

        // 2. 詠唱進行度の計算
        float maxTime = Math.max(1.0f, (float) entity.getCastTime());
        float currentTime = entity.tickCount + partialTicks;
        float progress = Math.min(1.0f, currentTime / maxTime);

        poseStack.pushPose();

        // 3. 演出: 詠唱の進行度に応じて回転とサイズが変化する魔法陣
        float rot = currentTime * (5.0f + progress * 20.0f);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(rot));
        poseStack.translate(0, 0.1, 0); // 少し浮かせる

        // 4. 描画
        // ■ 修正: RenderTypeをenergySwirlに変更し、より魔法らしい発光表現に
        VertexConsumer builder = buffer.getBuffer(RenderType.energySwirl(this.getTextureLocation(entity), progress, 1.0f - progress));

        // 魔法陣用の簡易メタデータ作成
        VisualMetadata circleMeta = new VisualMetadata();
        circleMeta.mainColor = color;
        circleMeta.subColor = meta.subColor;
        circleMeta.scale = 2.0f * progress;

        // 魔法陣としてリングを描画 (ShapeType=2)
        GeometryHelper.drawProceduralMesh(poseStack, builder, circleMeta, 2, currentTime, 0.8f);

        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-rot * 2.0f));
        poseStack.scale(0.6f, 1.0f, 0.6f);
        GeometryHelper.drawProceduralMesh(poseStack, builder, circleMeta, 2, currentTime, 0.5f);
        poseStack.popPose();

        poseStack.popPose();
    }
}