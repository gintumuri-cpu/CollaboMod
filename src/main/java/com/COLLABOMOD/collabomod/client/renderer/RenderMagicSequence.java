package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.magic.MagicComponentType; // 新Enum
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class RenderMagicSequence extends EntityRenderer<EntityMagicSequence> {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    public RenderMagicSequence(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMagicSequence entity) {
        return BEAM_TEXTURE;
    }

    @Override
    public void render(EntityMagicSequence entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // ■ 修正: コンポーネントから情報を取得
        String rendererID = entity.getRendererID();
        Vector3f color = entity.getColor(); // Enumに定義された色を使う

        float time = entity.tickCount + partialTicks;
        float progress = Math.min(1.0F, time / 20.0F); // castTime(20)で割る

        if (rendererID.equals("magic_circle")) {
            // 通常の魔法陣
            renderMagicCircle(poseStack, buffer, color, time, progress, entity);
        }
        else if (rendererID.equals("material_burst")) {
            // 戦略級エフェクト（必要ならここで特殊な描画を行うが、今回は魔法陣フェーズなので円でOK）
            // マテリアル・バーストの「発射前」も魔法陣を出したいならここ
            renderMagicCircle(poseStack, buffer, color, time, progress, entity);
        }
    }

    private void renderMagicCircle(PoseStack poseStack, MultiBufferSource buffer, Vector3f color, float time, float progress, EntityMagicSequence entity) {
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Vector3f.XP.rotationDegrees(entity.getXRot()));

        VertexConsumer builder = buffer.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        float scale = 1.5F * progress;
        drawRing(poseStack, builder, scale, color, time * 2.0F);
        drawRing(poseStack, builder, scale * 0.6F, color, -time * 5.0F);

        poseStack.popPose();
    }

    // drawRing, addVertex は変更なし（そのまま維持）
    private void drawRing(PoseStack poseStack, VertexConsumer builder, float radius, Vector3f color, float rotation) {
        Matrix4f pose = poseStack.last().pose();
        int segments = 24;
        float width = 0.1F;

        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments + Math.toRadians(rotation);
            double angle2 = 2 * Math.PI * (i + 1) / segments + Math.toRadians(rotation);
            float x1 = (float) Math.cos(angle1) * radius;
            float y1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float y2 = (float) Math.sin(angle2) * radius;
            float u1 = (float)i / segments;
            float u2 = (float)(i + 1) / segments;
            addVertex(builder, pose, x1, y1, 0, u1, 0, color);
            addVertex(builder, pose, x2, y2, 0, u2, 0, color);
            addVertex(builder, pose, x2 * (1-width), y2 * (1-width), 0, u2, 1, color);
            addVertex(builder, pose, x1 * (1-width), y1 * (1-width), 0, u1, 1, color);
        }
    }

    private void addVertex(VertexConsumer builder, Matrix4f pose, float x, float y, float z, float u, float v, Vector3f color) {
        builder.vertex(pose, x, y, z)
                .color(color.x(), color.y(), color.z(), 0.8F)
                .uv(u, v)
                .overlayCoords(0, 10)
                .uv2(240, 240)
                .normal(0, 0, 1)
                .endVertex();
    }
}
