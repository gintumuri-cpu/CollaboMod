package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.util.GeometryHelper;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
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

        VisualMetadata meta = entity.getVisualMetadata();
        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F);

        poseStack.pushPose();
        // 回転アニメーション (パラメータで速度制御)
        float rotSpeed = meta.rotationSpeed * time;
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot() + rotSpeed));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot() + rotSpeed * 0.5f));

        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        // ■ 変更: プロシージャル描画の呼び出し
        // Shape Enum を int 型IDに変換 (0:Sphere, 1:Cylinder, 2:Ring)
        int shapeType = 0;
        if (meta.shape == EnumMagicShape.CYLINDER || meta.shape == EnumMagicShape.BEAM) shapeType = 1;
        else if (meta.shape == EnumMagicShape.RING || meta.shape == EnumMagicShape.RIPPLE) shapeType = 2;

        // 1. メインレイヤー描画
        GeometryHelper.drawProceduralMesh(poseStack, builder, meta, shapeType, time, 0.6F * progress);

        // 2. 複層レイヤー描画 (AIが layerCount を増やしていれば実行)
        if (meta.layerCount > 1) {
            for (int i = 1; i < meta.layerCount; i++) {
                float scaleMult = 1.0f + (i * 0.3f); // 外側に広げる
                float speedMult = (i % 2 == 0) ? 1.0f : -1.0f; // 偶数奇数で逆回転

                // メタデータを一時的にコピーしていじるのはコストが高いので、
                // GeometryHelper側で対応するのが理想だが、今回は簡易的にscaleだけ操作して呼ぶ
                VisualMetadata layerMeta = new VisualMetadata(); // 簡易コピー
                layerMeta.shape = meta.shape;
                layerMeta.mainColor = meta.subColor; // 2層目はサブカラー
                layerMeta.scale = meta.scale * scaleMult;
                layerMeta.isSpiky = meta.isSpiky;
                layerMeta.isWavy = meta.isWavy;

                GeometryHelper.drawProceduralMesh(poseStack, builder, layerMeta, shapeType, time * speedMult, 0.3F * progress);
            }
        }

        poseStack.popPose();
    }
}