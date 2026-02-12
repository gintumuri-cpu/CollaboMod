package com.COLLABOMOD.collabomod.client.renderer.executor;

import com.COLLABOMOD.collabomod.magic.command.MeshCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class MeshExecutor implements ICommandExecutor<MeshCommand> {

    @Override
    public void execute(MeshCommand command, Level level, Entity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // コマンドの開始時間からの経過時間を計算
        // entity.tickCount は整数なので、partialTicks を足して滑らかにする
        // ただし、command.startTime は 0.0~1.0 の正規化された時間
        // ここでは簡易的に、コマンドがアクティブな間はずっと描画されるものとする
        
        // 本来は「コマンドの持続時間」を管理する必要があるが、
        // RenderUniversalMagic側で「開始時間を過ぎたコマンド」は毎フレーム呼ばれる仕組みになっていると仮定する。
        // もし「一回だけ呼ばれる」仕組みなら、エンティティ側に「アクティブなメッシュリスト」を持たせる必要がある。
        // 現状のRenderUniversalMagicの実装を見ると:
        // if (commandTime >= entity.tickCount && commandTime < time) { execute... }
        // となっており、「そのTickに一回だけ」実行される仕組みである。
        
        // ★重要★
        // MeshCommandは「一瞬のイベント」ではなく「持続的な描画」なので、
        // RenderUniversalMagicのループ構造を変えるか、
        // ここでは「描画リクエスト」を投げるだけにするのが正しい。
        // しかし、大幅な改修を避けるため、今回は「MeshCommandは毎フレーム実行される」前提で実装するのではなく、
        // RenderUniversalMagic側で「MeshCommandだけは特別扱い（持続実行）」にするか、
        // あるいは「MeshCommand」自体を「VisualMetadata」の代替として機能させる。
        
        // 今回は「MeshExecutor」内で直接描画を行う。
        // そのため、RenderUniversalMagic側でこのコマンドを「持続的に」呼び出す必要がある。
        
        poseStack.pushPose();
        poseStack.scale(command.scale, command.scale, command.scale);
        
        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());
        
        // 簡易的な球体描画 (RenderUniversalMagicからロジックを流用・簡略化)
        renderMesh(poseStack, builder, command.color.x(), command.color.y(), command.color.z(), 0.8f);
        
        poseStack.popPose();
    }
    
    private void renderMesh(PoseStack poseStack, VertexConsumer builder, float r, float g, float b, float a) {
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();
        int stacks = 8;
        int slices = 8;

        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks;
            float v1 = (float) (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;

                Vector3f p00 = sphere(u0, v0);
                Vector3f p10 = sphere(u1, v0);
                Vector3f p11 = sphere(u1, v1);
                Vector3f p01 = sphere(u0, v1);

                addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
                addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
                addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
                addVertex(builder, m, n, p10, u1, v0, r, g, b, a);
                
                // 裏面
                addVertex(builder, m, n, p10, u1, v0, r, g, b, a);
                addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
                addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
                addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
            }
        }
    }
    
    private Vector3f sphere(float u, float v) {
        float theta = u * 2.0f * (float) Math.PI;
        float phi = (v - 0.5f) * (float) Math.PI;
        float cx = Mth.cos(phi) * Mth.cos(theta);
        float cy = Mth.sin(phi);
        float cz = Mth.cos(phi) * Mth.sin(theta);
        return new Vector3f(cx, cy, cz);
    }

    private void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f n, Vector3f pos, float u, float v, float r, float g, float b, float a) {
        builder.vertex(m, pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(n, 0, 1, 0)
                .endVertex();
    }
}