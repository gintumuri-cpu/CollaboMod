package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.VisualKeyframe;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.COLLABOMOD.collabomod.magic.command.CommandType;
import com.COLLABOMOD.collabomod.magic.command.MeshCommand;
import com.COLLABOMOD.collabomod.magic.command.TextureCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.COLLABOMOD.collabomod.client.renderer.executor.ICommandExecutor;
import com.COLLABOMOD.collabomod.client.renderer.executor.MeshExecutor;
import com.COLLABOMOD.collabomod.client.renderer.executor.ParticleExecutor;
import com.COLLABOMOD.collabomod.client.renderer.executor.SoundExecutor;
import com.COLLABOMOD.collabomod.client.renderer.executor.TextureExecutor;
import com.COLLABOMOD.collabomod.magic.command.ICommand;
import com.COLLABOMOD.collabomod.magic.command.ParticleCommand;
import com.COLLABOMOD.collabomod.magic.command.SoundCommand;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class RenderUniversalMagic extends EntityRenderer<EntitySciencePhenomenon> {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation("textures/entity/beacon_beam.png");

    private final Map<CommandType<?>, ICommandExecutor<?>> executors = new HashMap<>();
    private final List<ICommand> commandQueue = new ArrayList<>();
    private int lastTick = -1;

    public RenderUniversalMagic(EntityRendererProvider.Context context) {
        super(context);
        this.registerExecutors();
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySciencePhenomenon entity) {
        // 動的テクスチャが設定されていればそれを使う
        ResourceLocation dynamic = entity.getDynamicTexture();
        if (dynamic != null) {
            return dynamic;
        }
        return BEAM_TEXTURE;
    }

    @Override
    public void render(EntitySciencePhenomenon entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        boolean hasMeshCommand = false;

        // 1. コマンドリストの更新と実行
        // Tickが変わったら、新しい命令リストを準備する
        if (entity.tickCount != lastTick) {
            lastTick = entity.tickCount;
            commandQueue.clear();

            ListTag commandListNBT = entity.getCommandList();
            if (commandListNBT != null) {
                for (int i = 0; i < commandListNBT.size(); i++) {
                    CompoundTag cmdNbt = commandListNBT.getCompound(i);
                    try {
                        ICommand command = CommandType.fromNbt(cmdNbt);
                        commandQueue.add(command);
                        if (command instanceof MeshCommand) {
                            hasMeshCommand = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // Tickが変わらない場合でも、hasMeshCommandフラグは再計算
            for (ICommand command : commandQueue) {
                if (command instanceof MeshCommand) {
                    hasMeshCommand = true;
                    break;
                }
            }
        }

        float time = entity.tickCount + partialTicks;

        // 実行されるべきコマンドを実行
        for (ICommand command : commandQueue) {
            // 実行時間になったコマンドを実行する
            // partialTicksを考慮して、tickの間のコマンドも実行する
            float commandTime = command.getStartTime() * entity.maxLifeTime;
            
            // MeshCommandは持続的に実行する必要があるため、開始時間を過ぎていれば実行する
            if (command instanceof MeshCommand) {
                if (commandTime <= time) {
                    this.executeCommand(command, entity, partialTicks, poseStack, buffer, packedLight);
                }
            } else {
                // 通常のコマンド（パーティクル、サウンド、テクスチャ）は、そのTick内でのみ実行
                if (commandTime >= entity.tickCount && commandTime < time) {
                    this.executeCommand(command, entity, partialTicks, poseStack, buffer, packedLight);
                }
            }
        }

        // 2. 既存のメッシュ描画 (VisualMetadata)
        // MeshCommandがある場合は、レガシー描画をスキップ
        if (!hasMeshCommand) {
            VisualMetadata meta = entity.getVisualMetadata();
            if (meta != null && !meta.timeline.isEmpty()) {
                renderLegacyVisuals(entity, meta, time, poseStack, buffer);
            }
        }
    }

    private void renderLegacyVisuals(EntitySciencePhenomenon entity, VisualMetadata meta, float time, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        // 進行度 (0.0 ~ 1.0) をエンティティの寿命から計算
        float progress = Mth.clamp(time / (float) entity.maxLifeTime, 0.0f, 1.0f);

        // --- タイムライン補間 ---
        // 現在の進行度に対応するキーフレームを探す
        VisualKeyframe prevFrame = meta.timeline.get(0);
        VisualKeyframe nextFrame = meta.timeline.get(meta.timeline.size() - 1);

        for (int i = 0; i < meta.timeline.size() - 1; i++) {
            if (progress >= meta.timeline.get(i).timeStamp && progress <= meta.timeline.get(i + 1).timeStamp) {
                prevFrame = meta.timeline.get(i);
                nextFrame = meta.timeline.get(i + 1);
                break;
            }
        }

        // 2つのキーフレーム間での進行度を計算
        float frameDuration = nextFrame.timeStamp - prevFrame.timeStamp;
        float interpProgress = (frameDuration > 0) ? (progress - prevFrame.timeStamp) / frameDuration : 1.0f;

        // パラメータを線形補間
        float scale = Mth.lerp(interpProgress, prevFrame.scale, nextFrame.scale);
        float r = Mth.lerp(interpProgress, prevFrame.mainColor.x(), nextFrame.mainColor.x());
        float g = Mth.lerp(interpProgress, prevFrame.mainColor.y(), nextFrame.mainColor.y());
        float b = Mth.lerp(interpProgress, prevFrame.mainColor.z(), nextFrame.mainColor.z());
        float a = Mth.lerp(interpProgress, prevFrame.alpha, nextFrame.alpha);

        // 透明度がほぼ0なら描画しない
        if (a <= 0.01f) {
            poseStack.popPose();
            return;
        }

        // 形状ベクトルも補間
        float[] interpRawVec = new float[5];
        if (prevFrame.rawVector != null && nextFrame.rawVector != null && prevFrame.rawVector.length == 5 && nextFrame.rawVector.length == 5) {
            for (int i = 0; i < 5; i++) {
                interpRawVec[i] = Mth.lerp(interpProgress, prevFrame.rawVector[i], nextFrame.rawVector[i]);
            }
        }

        float wHeat = interpRawVec.length > 0 ? interpRawVec[0] : 0;
        float wCold = interpRawVec.length > 1 ? interpRawVec[1] : 0;
        float wMotion = interpRawVec.length > 2 ? interpRawVec[2] : 0;
        float wEntropy = interpRawVec.length > 3 ? interpRawVec[3] : 0;
        float wDivine = interpRawVec.length > 4 ? interpRawVec[4] : 0;

        // 全体のスケーリング
        poseStack.scale(scale, scale, scale);

        // RenderTypeをlightningに変更し、テクスチャ感をなくし発光させる
        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        // ニューラル・メッシュ生成 (補間されたパラメータを使用)
        renderParametricMesh(poseStack, builder, time, wHeat, wCold, wMotion, wEntropy, wDivine, r, g, b, a);

        poseStack.popPose();
    }

    private <T extends ICommand> void executeCommand(T command, EntitySciencePhenomenon entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ICommandExecutor<T> executor = (ICommandExecutor<T>) executors.get(command.getType());
        if (executor != null) {
            executor.execute(command, entity.level, entity, partialTicks, poseStack, buffer, packedLight);
        }
    }

    private void registerExecutors() {
        executors.put(CommandType.PARTICLE, new ParticleExecutor());
        executors.put(CommandType.SOUND, new SoundExecutor());
        executors.put(CommandType.MESH, new MeshExecutor());
        executors.put(CommandType.TEXTURE, new TextureExecutor());
    }

    /**
     * パラメトリック・メッシュ生成関数
     */
    private void renderParametricMesh(PoseStack poseStack, VertexConsumer builder, float time,
                                      float wHeat, float wCold, float wMotion, float wEntropy, float wDivine,
                                      float r, float g, float b, float a) {

        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();

        // メッシュの解像度
        int stacks = 16;
        int slices = 16;

        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks;
            float v1 = (float) (i + 1) / stacks;

            for (int j = 0; j < slices; j++) {
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;

                // 4つの頂点を計算
                Vector3f p00 = calculateVertex(u0, v0, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p10 = calculateVertex(u1, v0, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p11 = calculateVertex(u1, v1, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p01 = calculateVertex(u0, v1, time, wHeat, wCold, wMotion, wEntropy, wDivine);

                // 描画 (表と裏)
                addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
                addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
                addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
                addVertex(builder, m, n, p10, u1, v0, r, g, b, a);

                addVertex(builder, m, n, p10, u1, v0, r, g, b, a);
                addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
                addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
                addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
            }
        }
    }

    /**
     * 頂点座標計算関数
     */
    private Vector3f calculateVertex(float u, float v, float time,
                                     float wHeat, float wCold, float wMotion, float wEntropy, float wDivine) {

        // 1. ベース形状 (球体座標)
        float theta = u * 2.0f * (float) Math.PI;
        float phi = (v - 0.5f) * (float) Math.PI;

        float cx = Mth.cos(phi) * Mth.cos(theta);
        float cy = Mth.sin(phi);
        float cz = Mth.cos(phi) * Mth.sin(theta);

        // 2. 変形ロジック

        // Motion: 指向性・ビーム化
        if (wMotion > 0) {
            float stretch = 1.0f + wMotion * 5.0f;
            float thinness = 1.0f / (1.0f + wMotion * 2.0f);
            cy *= stretch;
            cx *= thinness;
            cz *= thinness;
        }

        // Entropy: 捻れ・スパイラル
        if (wEntropy > 0) {
            float twistAngle = cy * wEntropy * 3.0f + time * wEntropy * 0.2f;
            float nx = cx * Mth.cos(twistAngle) - cz * Mth.sin(twistAngle);
            float nz = cx * Mth.sin(twistAngle) + cz * Mth.cos(twistAngle);
            cx = nx;
            cz = nz;
        }

        // Heat: ノイズ・脈動
        if (wHeat > 0) {
            float noise = Mth.sin(u * 10.0f + time) * Mth.cos(v * 10.0f + time * 0.5f);
            float displacement = 1.0f + (noise * wHeat * 0.3f);
            cx *= displacement;
            cy *= displacement;
            cz *= displacement;
        }

        // Cold: 量子化・結晶化
        if (wCold > 0) {
            float step = 0.5f;
            float qx = Math.round(cx / step) * step;
            float qy = Math.round(cy / step) * step;
            float qz = Math.round(cz / step) * step;
            cx = Mth.lerp(wCold, cx, qx);
            cy = Mth.lerp(wCold, cy, qy);
            cz = Mth.lerp(wCold, cz, qz);
        }

        // Divine: 安定化・パルス
        if (wDivine > 0) {
            float pulse = 1.0f + Mth.sin(time * 0.1f) * 0.1f * wDivine;
            cx *= pulse;
            cy *= pulse;
            cz *= pulse;
        }

        return new Vector3f(cx, cy, cz);
    }

    private void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f n, Vector3f pos, float u, float v, float r, float g, float b, float a) {
        builder.vertex(m, pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880) // Lightmap Full
                .normal(n, 0, 1, 0)
                .endVertex();
    }
}