package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.util.GeometryHelper;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
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

public class RenderUniversalMagic extends EntityRenderer<EntityMagicSequence> {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    public RenderUniversalMagic(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSequence entity) {
        return BEAM_TEXTURE;
    }

    @Override
    public boolean shouldRender(EntityMagicSequence entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true; // 常に描画
    }

    @Override
    public void render(EntityMagicSequence entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // ■ エンティティからパラメータを取得（※EntityMagicSequenceの更新が必要）
        // 現時点では古いメソッドしかないため、後でEntity側を更新して getVisualMetadata() を作ります
        // 仮の実装イメージです
        /*
        VisualMetadata meta = entity.getVisualMetadata();
        */
        // ↓ 互換用：今の実装に合わせて手動構築
        VisualMetadata meta = new VisualMetadata();
        meta.mainColor = entity.getColor();
        // IDから形状を逆算（過渡期用）
        String id = entity.getRendererID();
        if (id.equals("material_burst")) {
            meta.shape = EnumMagicShape.SPHERE;
            meta.scale = 2.0F;
            meta.hasLightning = true;
        } else {
            meta.shape = EnumMagicShape.RING;
        }


        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F); // 本来はcastTimeを使う

        poseStack.pushPose();

        // 共通設定: 回転とテクスチャ
        VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        // 向き合わせ
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        // ■■■ 形状による分岐（if文はここだけ！） ■■■

        // ケース1: 魔法陣リング
        if (meta.shape == EnumMagicShape.RING) {
            float scale = meta.scale * 1.5F * progress;
            // 外側
            GeometryHelper.drawRing(poseStack, builder, scale, 0.1F, meta.mainColor, 0.8F, time * 2.0F * meta.rotationSpeed);
            // 内側
            GeometryHelper.drawRing(poseStack, builder, scale * 0.6F, 0.1F, meta.subColor, 0.6F, -time * 5.0F * meta.rotationSpeed);
        }

        // ケース2: エネルギー球体
        else if (meta.shape == EnumMagicShape.SPHERE) {
            float scale = meta.scale * 20.0F * progress; // 巨大化

            // コア
            poseStack.pushPose();
            poseStack.mulPose(Vector3f.YP.rotationDegrees(time * 2.0F));
            GeometryHelper.drawSphere(poseStack, builder, scale, meta.mainColor, 0.6F, time * 0.05F);
            poseStack.popPose();

            // シェル
            poseStack.pushPose();
            poseStack.mulPose(Vector3f.XP.rotationDegrees(time * 3.0F));
            GeometryHelper.drawSphere(poseStack, builder, scale * 1.1F, meta.subColor, 0.3F, -time * 0.1F);
            poseStack.popPose();
        }

        // ■■■ 付随エフェクト ■■■

        // 雷 (Lightning)
        if (meta.hasLightning) {
            VertexConsumer lightningBuilder = buffer.getBuffer(RenderType.lightning());
            // 簡易的に数本出す
            float r = meta.scale * 20.0F * progress * 1.2F;
            Vector3f center = new Vector3f(0,0,0);
            for(int i=0; i<5; i++) {
                // ランダムな方向へ
                // (GeometryHelperのdrawLightningを呼ぶ)
            }
        }

        poseStack.popPose();
    }
}