package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.PhysicsMetadata;
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
import java.util.Random;

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
        ResourceLocation dynamic = entity.getDynamicTexture();
        if (dynamic != null) {
            return dynamic;
        }
        return BEAM_TEXTURE;
    }

    @Override
    public void render(EntitySciencePhenomenon entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        boolean hasMeshCommand = false;

        // 1. コマンドリストの更新と実行
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
            for (ICommand command : commandQueue) {
                if (command instanceof MeshCommand) {
                    hasMeshCommand = true;
                    break;
                }
            }
        }

        float time = entity.tickCount + partialTicks;

        // コマンド実行
        for (ICommand command : commandQueue) {
            float commandTime = command.getStartTime() * entity.maxLifeTime;
            if (command instanceof MeshCommand) {
                if (commandTime <= time) {
                    this.executeCommand(command, entity, partialTicks, poseStack, buffer, packedLight);
                }
            } else {
                if (commandTime >= entity.tickCount && commandTime < time) {
                    this.executeCommand(command, entity, partialTicks, poseStack, buffer, packedLight);
                }
            }
        }

        // 2. レガシー描画 (VisualMetadata) — 多層描画に拡張
        if (!hasMeshCommand) {
            VisualMetadata meta = entity.getVisualMetadata();
            if (meta != null && !meta.timeline.isEmpty()) {
                renderMultiLayerVisuals(entity, meta, time, poseStack, buffer);
            }
        }
    }

    // ========================================================================
    // 多層描画エンジン
    // ========================================================================

    private void renderMultiLayerVisuals(EntitySciencePhenomenon entity, VisualMetadata meta, float time,
            PoseStack poseStack, MultiBufferSource buffer) {
        float progress = Mth.clamp(time / (float) entity.maxLifeTime, 0.0f, 1.0f);

        // タイムライン補間
        VisualKeyframe prevFrame = meta.timeline.get(0);
        VisualKeyframe nextFrame = meta.timeline.get(meta.timeline.size() - 1);

        for (int i = 0; i < meta.timeline.size() - 1; i++) {
            if (progress >= meta.timeline.get(i).timeStamp && progress <= meta.timeline.get(i + 1).timeStamp) {
                prevFrame = meta.timeline.get(i);
                nextFrame = meta.timeline.get(i + 1);
                break;
            }
        }

        float frameDuration = nextFrame.timeStamp - prevFrame.timeStamp;
        float interpProgress = (frameDuration > 0) ? (progress - prevFrame.timeStamp) / frameDuration : 1.0f;

        float scale = Mth.lerp(interpProgress, prevFrame.scale, nextFrame.scale);
        float r = Mth.lerp(interpProgress, prevFrame.mainColor.x(), nextFrame.mainColor.x());
        float g = Mth.lerp(interpProgress, prevFrame.mainColor.y(), nextFrame.mainColor.y());
        float b = Mth.lerp(interpProgress, prevFrame.mainColor.z(), nextFrame.mainColor.z());
        float a = Mth.lerp(interpProgress, prevFrame.alpha, nextFrame.alpha);

        if (a <= 0.01f)
            return;

        // 形状ベクトル補間
        float[] interpRawVec = new float[5];
        if (prevFrame.rawVector != null && nextFrame.rawVector != null && prevFrame.rawVector.length == 5
                && nextFrame.rawVector.length == 5) {
            for (int i = 0; i < 5; i++) {
                interpRawVec[i] = Mth.lerp(interpProgress, prevFrame.rawVector[i], nextFrame.rawVector[i]);
            }
        }

        float wHeat = interpRawVec.length > 0 ? interpRawVec[0] : 0;
        float wCold = interpRawVec.length > 1 ? interpRawVec[1] : 0;
        float wMotion = interpRawVec.length > 2 ? interpRawVec[2] : 0;
        float wEntropy = interpRawVec.length > 3 ? interpRawVec[3] : 0;
        float wDivine = interpRawVec.length > 4 ? interpRawVec[4] : 0;

        EnumMagicShape shape = meta.shape;
        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        // ■ Layer 1: 外殻 (Outer Shell) — 半透明、拡大
        poseStack.pushPose();
        {
            float outerScale = scale * 1.3f;
            poseStack.scale(outerScale, outerScale, outerScale);
            float outerA = a * 0.25f; // 半透明
            // サブカラーまたは補色で外殻を描画
            float outerR = Mth.clamp(r * 0.6f + 0.2f, 0, 1);
            float outerG = Mth.clamp(g * 0.6f + 0.2f, 0, 1);
            float outerB = Mth.clamp(b * 0.6f + 0.2f, 0, 1);
            renderShapedMesh(poseStack, builder, time, shape, wHeat, wCold, wMotion, wEntropy, wDivine, outerR, outerG,
                    outerB, outerA);
        }
        poseStack.popPose();

        // ■ Layer 2: メインボディ (Main Body)
        poseStack.pushPose();
        {
            poseStack.scale(scale, scale, scale);
            renderShapedMesh(poseStack, builder, time, shape, wHeat, wCold, wMotion, wEntropy, wDivine, r, g, b, a);
        }
        poseStack.popPose();

        // ■ Layer 3: 内核 (Core) — 高輝度、縮小
        poseStack.pushPose();
        {
            float coreScale = scale * 0.4f;
            poseStack.scale(coreScale, coreScale, coreScale);
            // 白寄りの高輝度色
            float coreR = Mth.clamp(r * 0.3f + 0.7f, 0, 1);
            float coreG = Mth.clamp(g * 0.3f + 0.7f, 0, 1);
            float coreB = Mth.clamp(b * 0.3f + 0.7f, 0, 1);
            renderShapedMesh(poseStack, builder, time * 1.5f, shape, wHeat, wCold, wMotion, wEntropy, wDivine, coreR,
                    coreG, coreB, Math.min(1.0f, a * 1.5f));
        }
        poseStack.popPose();

        // ■ Layer 4: パーティクル雲 (属性依存)
        if (wHeat > 0.2f || wCold > 0.2f || wDivine > 0.3f || wEntropy > 0.3f) {
            renderParticleCloud(poseStack, builder, time, scale, wHeat, wCold, wEntropy, wDivine, r, g, b, a);
        }

        // ■ Layer 5: 衝撃波 (RADIAL発動時にリング状メッシュが高速拡大)
        PhysicsMetadata phy = entity.getPhysicsMetadata();
        if (phy != null && phy.forceType == PhysicsMetadata.EnumForceType.RADIAL) {
            renderShockwave(poseStack, builder, time, entity.lifeTime, entity.maxLifeTime, scale, r, g, b);
        }

        // ■ Layer 6: 残像トレイル (DIRECTIONAL)
        if (phy != null && phy.forceType == PhysicsMetadata.EnumForceType.DIRECTIONAL) {
            renderAfterimage(poseStack, builder, time, shape, scale, wHeat, wCold, wMotion, wEntropy, wDivine, r, g, b,
                    a);
        }

        // ■ Layer 7: 属性固有アニメーション
        renderAttributeAnimation(poseStack, builder, time, scale, wHeat, wCold, wEntropy, wDivine, r, g, b, a);
    }

    // ========================================================================
    // Phase 6: 動的アニメーション
    // ========================================================================

    /**
     * 衝撃波: RADIAL発動時に薄いリングが高速拡大
     */
    private void renderShockwave(PoseStack poseStack, VertexConsumer builder, float time,
            int lifeTime, int maxLifeTime, float scale,
            float r, float g, float b) {
        float shockProgress = Math.min(1.0f, (float) lifeTime / 10.0f);
        float shockRadius = scale * 0.5f + shockProgress * scale * 3.0f;
        float shockAlpha = (1.0f - shockProgress) * 0.6f;
        if (shockAlpha <= 0.01f)
            return;

        poseStack.pushPose();
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();
        int slices = 32;
        float width = 0.15f * scale * (1.0f - shockProgress * 0.5f);
        for (int j = 0; j < slices; j++) {
            float theta0 = (float) j / slices * 2.0f * (float) Math.PI;
            float theta1 = (float) (j + 1) / slices * 2.0f * (float) Math.PI;
            Vector3f p00 = new Vector3f(Mth.cos(theta0) * (shockRadius - width), 0,
                    Mth.sin(theta0) * (shockRadius - width));
            Vector3f p10 = new Vector3f(Mth.cos(theta1) * (shockRadius - width), 0,
                    Mth.sin(theta1) * (shockRadius - width));
            Vector3f p11 = new Vector3f(Mth.cos(theta1) * (shockRadius + width), 0,
                    Mth.sin(theta1) * (shockRadius + width));
            Vector3f p01 = new Vector3f(Mth.cos(theta0) * (shockRadius + width), 0,
                    Mth.sin(theta0) * (shockRadius + width));
            addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, shockAlpha);
        }
        poseStack.popPose();
    }

    /**
     * 残像トレイル: DIRECTIONAL発動時に複数の半透明コピーを後方に描画
     */
    private void renderAfterimage(PoseStack poseStack, VertexConsumer builder, float time,
            EnumMagicShape shape, float scale,
            float wHeat, float wCold, float wMotion, float wEntropy, float wDivine,
            float r, float g, float b, float a) {
        int trailCount = 3;
        for (int t = 1; t <= trailCount; t++) {
            poseStack.pushPose();
            float offset = t * 0.8f;
            poseStack.translate(0, 0, offset); // 後方にオフセット
            float trailScale = scale * (1.0f - t * 0.15f);
            float trailAlpha = a * (0.4f - t * 0.1f);
            if (trailAlpha <= 0.01f) {
                poseStack.popPose();
                continue;
            }
            poseStack.scale(trailScale, trailScale, trailScale);
            renderShapedMesh(poseStack, builder, time - t * 2.0f, shape, wHeat, wCold, wMotion, wEntropy, wDivine, r, g,
                    b, trailAlpha);
            poseStack.popPose();
        }
    }

    /**
     * 属性固有アニメーション
     */
    private void renderAttributeAnimation(PoseStack poseStack, VertexConsumer builder, float time, float scale,
            float wHeat, float wCold, float wEntropy, float wDivine,
            float r, float g, float b, float a) {
        Matrix4f m;
        Matrix3f n;

        // ■ 炎: 上昇する火の粉
        if (wHeat > 0.4f) {
            poseStack.pushPose();
            m = poseStack.last().pose();
            n = poseStack.last().normal();
            int sparkCount = (int) (wHeat * 15);
            long seed = (long) (time * 5);
            Random rng = new Random(seed);
            float sparkSize = 0.04f * scale;
            for (int i = 0; i < sparkCount; i++) {
                float sx = (rng.nextFloat() - 0.5f) * scale * 2.0f;
                float sy = (time * 0.3f + rng.nextFloat() * 2.0f) % (scale * 3.0f);
                float sz = (rng.nextFloat() - 0.5f) * scale * 2.0f;
                sx += Mth.sin(time * 0.5f + i) * 0.2f;
                float sa = a * (0.6f + rng.nextFloat() * 0.4f);
                Vector3f p00 = new Vector3f(sx - sparkSize, sy - sparkSize, sz);
                Vector3f p10 = new Vector3f(sx + sparkSize, sy - sparkSize, sz);
                Vector3f p11 = new Vector3f(sx + sparkSize, sy + sparkSize, sz);
                Vector3f p01 = new Vector3f(sx - sparkSize, sy + sparkSize, sz);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, 1.0f, 0.6f, 0.1f, sa);
            }
            poseStack.popPose();
        }

        // ■ 氷: 結晶が成長→砕散
        if (wCold > 0.4f) {
            poseStack.pushPose();
            float crystalGrow = Math.min(1.0f, (time % 40.0f) / 20.0f);
            float crystalFade = (time % 40.0f) > 20.0f ? 1.0f - ((time % 40.0f) - 20.0f) / 20.0f : 1.0f;
            float cs = scale * 0.6f * crystalGrow;
            poseStack.scale(cs, cs, cs);
            m = poseStack.last().pose();
            n = poseStack.last().normal();

            // 寄生結晶: 6方向のスパイク
            int spikes = 6;
            for (int i = 0; i < spikes; i++) {
                float angle = (float) i / spikes * 2.0f * (float) Math.PI;
                float tipX = Mth.cos(angle) * 1.5f;
                float tipY = Mth.sin(angle * 2.0f + time * 0.1f) * 0.5f;
                float tipZ = Mth.sin(angle) * 1.5f;
                Vector3f tip = new Vector3f(tipX, tipY, tipZ);
                Vector3f base0 = new Vector3f(Mth.cos(angle - 0.2f) * 0.2f, -0.1f, Mth.sin(angle - 0.2f) * 0.2f);
                Vector3f base1 = new Vector3f(Mth.cos(angle + 0.2f) * 0.2f, 0.1f, Mth.sin(angle + 0.2f) * 0.2f);
                addTriangle(builder, m, n, base0, base1, tip, 0.7f, 0.9f, 1.0f, a * crystalFade * 0.6f);
            }
            poseStack.popPose();
        }

        // ■ 神聖: 回転する幾何学リング
        if (wDivine > 0.3f) {
            for (int ring = 0; ring < 2; ring++) {
                poseStack.pushPose();
                float ringRot = time * 0.08f * (ring + 1);
                if (ring == 0) {
                    poseStack.mulPose(Vector3f.YP.rotation(ringRot));
                } else {
                    poseStack.mulPose(Vector3f.XP.rotation(ringRot));
                }
                m = poseStack.last().pose();
                n = poseStack.last().normal();

                float ringRadius = scale * (1.5f + ring * 0.5f);
                float ringWidth = 0.06f * scale;
                int slices = 24;
                for (int j = 0; j < slices; j++) {
                    float theta0 = (float) j / slices * 2.0f * (float) Math.PI;
                    float theta1 = (float) (j + 1) / slices * 2.0f * (float) Math.PI;
                    Vector3f p00 = new Vector3f(Mth.cos(theta0) * (ringRadius - ringWidth), 0,
                            Mth.sin(theta0) * (ringRadius - ringWidth));
                    Vector3f p10 = new Vector3f(Mth.cos(theta1) * (ringRadius - ringWidth), 0,
                            Mth.sin(theta1) * (ringRadius - ringWidth));
                    Vector3f p11 = new Vector3f(Mth.cos(theta1) * (ringRadius + ringWidth), 0,
                            Mth.sin(theta1) * (ringRadius + ringWidth));
                    Vector3f p01 = new Vector3f(Mth.cos(theta0) * (ringRadius + ringWidth), 0,
                            Mth.sin(theta0) * (ringRadius + ringWidth));
                    addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, 1.0f, 0.95f, 0.7f, a * 0.4f * wDivine);
                }
                poseStack.popPose();
            }
        }

        // ■ 混沌: 不安定に歪むグリッチエフェクト
        if (wEntropy > 0.4f) {
            poseStack.pushPose();
            long glitchSeed = (long) (time * 10);
            Random glitchRng = new Random(glitchSeed);
            int glitchCount = (int) (wEntropy * 8);
            m = poseStack.last().pose();
            n = poseStack.last().normal();
            for (int i = 0; i < glitchCount; i++) {
                float gx = (glitchRng.nextFloat() - 0.5f) * scale * 2.5f;
                float gy = (glitchRng.nextFloat() - 0.5f) * scale * 2.5f;
                float gz = (glitchRng.nextFloat() - 0.5f) * scale * 2.5f;
                float gw = 0.1f + glitchRng.nextFloat() * 0.3f;
                float gh = 0.02f + glitchRng.nextFloat() * 0.05f;
                float gr = glitchRng.nextFloat();
                float gg = glitchRng.nextFloat();
                float gb = glitchRng.nextFloat();
                Vector3f p00 = new Vector3f(gx, gy, gz);
                Vector3f p10 = new Vector3f(gx + gw, gy, gz);
                Vector3f p11 = new Vector3f(gx + gw, gy + gh, gz);
                Vector3f p01 = new Vector3f(gx, gy + gh, gz);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, gr, gg, gb, a * 0.5f);
            }
            poseStack.popPose();
        }
    }

    // ========================================================================
    // 形状別メッシュ生成
    // ========================================================================

    private void renderShapedMesh(PoseStack poseStack, VertexConsumer builder, float time,
            EnumMagicShape shape, float wHeat, float wCold, float wMotion,
            float wEntropy, float wDivine,
            float r, float g, float b, float a) {
        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();

        switch (shape) {
            case BEAM:
            case CYLINDER:
                renderCylinder(m, n, builder, time, 0.3f, 3.0f, wHeat, wMotion, r, g, b, a);
                break;
            case CONE:
                renderCone(m, n, builder, time, 1.0f, 3.0f, wHeat, r, g, b, a);
                break;
            case RING:
            case COMPLEX_CIRCLE:
                renderTorus(m, n, builder, time, 1.5f, 0.2f, wDivine, r, g, b, a);
                break;
            case VORTEX:
                renderVortex(m, n, builder, time, wEntropy, wMotion, r, g, b, a);
                break;
            case LIGHTNING:
                renderLightningBolt(m, n, builder, time, wHeat, r, g, b, a);
                break;
            case CRYSTAL:
                renderCrystal(m, n, builder, time, wCold, r, g, b, a);
                break;
            case CUBE:
                renderCube(m, n, builder, time, wEntropy, r, g, b, a);
                break;
            case RIPPLE:
                renderRipple(m, n, builder, time, r, g, b, a);
                break;
            case PARTICLE_MIST:
                renderMist(m, n, builder, time, wCold, r, g, b, a);
                break;
            case SPHERE:
            default:
                renderParametricSphere(m, n, builder, time, wHeat, wCold, wMotion, wEntropy, wDivine, r, g, b, a);
                break;
        }
    }

    // --- SPHERE (既存のパラメトリック球体) ---
    private void renderParametricSphere(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wHeat, float wCold, float wMotion, float wEntropy, float wDivine,
            float r, float g, float b, float a) {
        int stacks = 16;
        int slices = 16;
        for (int i = 0; i < stacks; i++) {
            float v0 = (float) i / stacks;
            float v1 = (float) (i + 1) / stacks;
            for (int j = 0; j < slices; j++) {
                float u0 = (float) j / slices;
                float u1 = (float) (j + 1) / slices;
                Vector3f p00 = calculateSphereVertex(u0, v0, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p10 = calculateSphereVertex(u1, v0, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p11 = calculateSphereVertex(u1, v1, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                Vector3f p01 = calculateSphereVertex(u0, v1, time, wHeat, wCold, wMotion, wEntropy, wDivine);
                addQuad(builder, m, n, p00, p01, p11, p10, u0, v0, u1, v1, r, g, b, a);
            }
        }
    }

    private Vector3f calculateSphereVertex(float u, float v, float time,
            float wHeat, float wCold, float wMotion, float wEntropy, float wDivine) {
        float theta = u * 2.0f * (float) Math.PI;
        float phi = (v - 0.5f) * (float) Math.PI;
        float cx = Mth.cos(phi) * Mth.cos(theta);
        float cy = Mth.sin(phi);
        float cz = Mth.cos(phi) * Mth.sin(theta);

        if (wMotion > 0) {
            float stretch = 1.0f + wMotion * 5.0f;
            float thinness = 1.0f / (1.0f + wMotion * 2.0f);
            cy *= stretch;
            cx *= thinness;
            cz *= thinness;
        }
        if (wEntropy > 0) {
            float twistAngle = cy * wEntropy * 3.0f + time * wEntropy * 0.2f;
            float nx = cx * Mth.cos(twistAngle) - cz * Mth.sin(twistAngle);
            float nz = cx * Mth.sin(twistAngle) + cz * Mth.cos(twistAngle);
            cx = nx;
            cz = nz;
        }
        if (wHeat > 0) {
            float noise = Mth.sin(u * 10.0f + time) * Mth.cos(v * 10.0f + time * 0.5f);
            float displacement = 1.0f + (noise * wHeat * 0.3f);
            cx *= displacement;
            cy *= displacement;
            cz *= displacement;
        }
        if (wCold > 0) {
            float step = 0.5f;
            float qx = Math.round(cx / step) * step;
            float qy = Math.round(cy / step) * step;
            float qz = Math.round(cz / step) * step;
            cx = Mth.lerp(wCold, cx, qx);
            cy = Mth.lerp(wCold, cy, qy);
            cz = Mth.lerp(wCold, cz, qz);
        }
        if (wDivine > 0) {
            float pulse = 1.0f + Mth.sin(time * 0.1f) * 0.1f * wDivine;
            cx *= pulse;
            cy *= pulse;
            cz *= pulse;
        }
        return new Vector3f(cx, cy, cz);
    }

    // --- CYLINDER / BEAM ---
    private void renderCylinder(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float radius, float length, float wHeat, float wMotion,
            float r, float g, float b, float a) {
        int slices = 16;
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float y0 = -length / 2.0f + (length * i / segments);
            float y1 = -length / 2.0f + (length * (i + 1) / segments);
            float segProgress = (float) i / segments;
            // ビーム先端に向かって細くなる
            float r0 = radius * (1.0f - segProgress * 0.3f * wMotion);
            float r1 = radius * (1.0f - (segProgress + 1.0f / segments) * 0.3f * wMotion);
            // 炎の揺らぎ
            float flicker = wHeat > 0 ? Mth.sin(time * 2.0f + segProgress * 8.0f) * wHeat * 0.15f : 0;
            for (int j = 0; j < slices; j++) {
                float theta0 = (float) j / slices * 2.0f * (float) Math.PI;
                float theta1 = (float) (j + 1) / slices * 2.0f * (float) Math.PI;
                Vector3f p00 = new Vector3f(Mth.cos(theta0) * (r0 + flicker), y0, Mth.sin(theta0) * (r0 + flicker));
                Vector3f p10 = new Vector3f(Mth.cos(theta1) * (r0 + flicker), y0, Mth.sin(theta1) * (r0 + flicker));
                Vector3f p11 = new Vector3f(Mth.cos(theta1) * (r1 + flicker), y1, Mth.sin(theta1) * (r1 + flicker));
                Vector3f p01 = new Vector3f(Mth.cos(theta0) * (r1 + flicker), y1, Mth.sin(theta0) * (r1 + flicker));
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, a);
            }
        }
    }

    // --- CONE (火柱・ブレス) ---
    private void renderCone(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float baseRadius, float height, float wHeat,
            float r, float g, float b, float a) {
        int slices = 16;
        int segments = 10;
        for (int i = 0; i < segments; i++) {
            float segProgress0 = (float) i / segments;
            float segProgress1 = (float) (i + 1) / segments;
            float y0 = segProgress0 * height;
            float y1 = segProgress1 * height;
            float r0 = baseRadius * (1.0f - segProgress0);
            float r1 = baseRadius * (1.0f - segProgress1);
            float flicker = wHeat > 0 ? Mth.sin(time * 3.0f + segProgress0 * 10.0f) * wHeat * 0.2f : 0;
            for (int j = 0; j < slices; j++) {
                float theta0 = (float) j / slices * 2.0f * (float) Math.PI;
                float theta1 = (float) (j + 1) / slices * 2.0f * (float) Math.PI;
                Vector3f p00 = new Vector3f(Mth.cos(theta0) * (r0 + flicker), y0, Mth.sin(theta0) * (r0 + flicker));
                Vector3f p10 = new Vector3f(Mth.cos(theta1) * (r0 + flicker), y0, Mth.sin(theta1) * (r0 + flicker));
                Vector3f p11 = new Vector3f(Mth.cos(theta1) * r1, y1, Mth.sin(theta1) * r1);
                Vector3f p01 = new Vector3f(Mth.cos(theta0) * r1, y1, Mth.sin(theta0) * r1);
                // 先端に行くほど明るく
                float segR = Mth.lerp(segProgress0, r, 1.0f);
                float segG = Mth.lerp(segProgress0, g, 0.9f);
                float segB = Mth.lerp(segProgress0, b, 0.5f);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, segR, segG, segB,
                        a * (1.0f - segProgress0 * 0.3f));
            }
        }
    }

    // --- RING / TORUS (魔法陣、後光) ---
    private void renderTorus(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float majorR, float minorR, float wDivine,
            float r, float g, float b, float a) {
        int majorSegs = 24;
        int minorSegs = 12;
        float pulse = 1.0f + Mth.sin(time * 0.15f) * 0.08f * (1.0f + wDivine);
        for (int i = 0; i < majorSegs; i++) {
            float phi0 = (float) i / majorSegs * 2.0f * (float) Math.PI + time * 0.05f;
            float phi1 = (float) (i + 1) / majorSegs * 2.0f * (float) Math.PI + time * 0.05f;
            for (int j = 0; j < minorSegs; j++) {
                float theta0 = (float) j / minorSegs * 2.0f * (float) Math.PI;
                float theta1 = (float) (j + 1) / minorSegs * 2.0f * (float) Math.PI;
                Vector3f p00 = torusPoint(phi0, theta0, majorR * pulse, minorR);
                Vector3f p10 = torusPoint(phi1, theta0, majorR * pulse, minorR);
                Vector3f p11 = torusPoint(phi1, theta1, majorR * pulse, minorR);
                Vector3f p01 = torusPoint(phi0, theta1, majorR * pulse, minorR);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, a);
            }
        }
    }

    private Vector3f torusPoint(float phi, float theta, float R, float rr) {
        float x = (R + rr * Mth.cos(theta)) * Mth.cos(phi);
        float y = rr * Mth.sin(theta);
        float z = (R + rr * Mth.cos(theta)) * Mth.sin(phi);
        return new Vector3f(x, y, z);
    }

    // --- VORTEX (渦巻き) ---
    private void renderVortex(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wEntropy, float wMotion,
            float r, float g, float b, float a) {
        int arms = 3 + (int) (wEntropy * 3);
        int segments = 20;
        float width = 0.15f;
        for (int arm = 0; arm < arms; arm++) {
            float armOffset = (float) arm / arms * 2.0f * (float) Math.PI;
            for (int i = 0; i < segments; i++) {
                float t0 = (float) i / segments;
                float t1 = (float) (i + 1) / segments;
                float angle0 = armOffset + t0 * 4.0f * (float) Math.PI + time * 0.2f;
                float angle1 = armOffset + t1 * 4.0f * (float) Math.PI + time * 0.2f;
                float rad0 = t0 * 2.0f;
                float rad1 = t1 * 2.0f;
                float y0 = t0 * 2.0f * (1.0f + wMotion);
                float y1 = t1 * 2.0f * (1.0f + wMotion);
                Vector3f p00 = new Vector3f(Mth.cos(angle0) * (rad0 - width), y0, Mth.sin(angle0) * (rad0 - width));
                Vector3f p10 = new Vector3f(Mth.cos(angle0) * (rad0 + width), y0, Mth.sin(angle0) * (rad0 + width));
                Vector3f p11 = new Vector3f(Mth.cos(angle1) * (rad1 + width), y1, Mth.sin(angle1) * (rad1 + width));
                Vector3f p01 = new Vector3f(Mth.cos(angle1) * (rad1 - width), y1, Mth.sin(angle1) * (rad1 - width));
                float segA = a * (1.0f - t0 * 0.5f);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, segA);
            }
        }
    }

    // --- LIGHTNING (稲妻) ---
    private void renderLightningBolt(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wHeat, float r, float g, float b, float a) {
        // 決定論的乱数（tickベース）。tick変化でフレーム更新
        Random rng = new Random((long) (time * 3));
        int branches = 3 + rng.nextInt(3);

        for (int br = 0; br < branches; br++) {
            int segments = 6 + rng.nextInt(4);
            float boltWidth = 0.06f + rng.nextFloat() * 0.04f;
            float px = 0, py = 0, pz = 0;
            float dirX = (rng.nextFloat() - 0.5f) * 0.5f;
            float dirY = 1.0f + rng.nextFloat() * 0.5f;
            float dirZ = (rng.nextFloat() - 0.5f) * 0.5f;
            for (int i = 0; i < segments; i++) {
                float nx = px + dirX * 0.5f + (rng.nextFloat() - 0.5f) * 0.6f;
                float ny = py + dirY * 0.5f;
                float nz = pz + dirZ * 0.5f + (rng.nextFloat() - 0.5f) * 0.6f;
                // 稲妻を細い四角柱として描画
                Vector3f p00 = new Vector3f(px - boltWidth, py, pz);
                Vector3f p10 = new Vector3f(px + boltWidth, py, pz);
                Vector3f p11 = new Vector3f(nx + boltWidth, ny, nz);
                Vector3f p01 = new Vector3f(nx - boltWidth, ny, nz);
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, a);
                // 横面
                Vector3f p00b = new Vector3f(px, py, pz - boltWidth);
                Vector3f p10b = new Vector3f(px, py, pz + boltWidth);
                Vector3f p11b = new Vector3f(nx, ny, nz + boltWidth);
                Vector3f p01b = new Vector3f(nx, ny, nz - boltWidth);
                addQuad(builder, m, n, p00b, p01b, p11b, p10b, 0, 0, 1, 1, r, g, b, a);
                px = nx;
                py = ny;
                pz = nz;
            }
        }
    }

    // --- CRYSTAL (結晶) ---
    private void renderCrystal(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wCold, float r, float g, float b, float a) {
        int spikes = 6 + (int) (wCold * 4);
        float baseSize = 0.3f;
        float spikeLength = 1.5f + wCold;
        float rotation = time * 0.05f;

        for (int i = 0; i < spikes; i++) {
            float angle = (float) i / spikes * 2.0f * (float) Math.PI + rotation;
            float tilt = Mth.sin(angle * 2.0f + time * 0.1f) * 0.3f;
            float tipX = Mth.cos(angle) * 0.3f;
            float tipY = spikeLength + Mth.sin(time * 0.2f + i) * 0.2f;
            float tipZ = Mth.sin(angle) * 0.3f;
            // 四角錐の側面
            Vector3f tip = new Vector3f(tipX, tipY + tilt, tipZ);
            float bx0 = Mth.cos(angle - 0.3f) * baseSize;
            float bz0 = Mth.sin(angle - 0.3f) * baseSize;
            float bx1 = Mth.cos(angle + 0.3f) * baseSize;
            float bz1 = Mth.sin(angle + 0.3f) * baseSize;
            Vector3f base0 = new Vector3f(bx0, -0.3f, bz0);
            Vector3f base1 = new Vector3f(bx1, -0.3f, bz1);
            Vector3f base2 = new Vector3f(0, -0.3f, 0);
            // 面1
            addTriangle(builder, m, n, base0, base1, tip, r, g, b, a);
            // 面2
            addTriangle(builder, m, n, base1, base2, tip, r, g, b, a);
            // 面3
            addTriangle(builder, m, n, base2, base0, tip, r, g, b, a);

            // 下向きの鏡像スパイク
            Vector3f tipDown = new Vector3f(tipX, -(tipY + tilt), tipZ);
            Vector3f baseUp0 = new Vector3f(bx0, 0.3f, bz0);
            Vector3f baseUp1 = new Vector3f(bx1, 0.3f, bz1);
            Vector3f baseUp2 = new Vector3f(0, 0.3f, 0);
            addTriangle(builder, m, n, baseUp1, baseUp0, tipDown, r, g, b, a);
            addTriangle(builder, m, n, baseUp2, baseUp1, tipDown, r, g, b, a);
            addTriangle(builder, m, n, baseUp0, baseUp2, tipDown, r, g, b, a);
        }
    }

    // --- CUBE ---
    private void renderCube(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wEntropy, float r, float g, float b, float a) {
        float s = 0.8f;
        float rot = time * 0.1f * (1.0f + wEntropy);
        float cr = Mth.cos(rot), sr = Mth.sin(rot);
        // 8頂点
        Vector3f[] verts = new Vector3f[8];
        float[][] corners = { { -s, -s, -s }, { s, -s, -s }, { s, s, -s }, { -s, s, -s }, { -s, -s, s }, { s, -s, s },
                { s, s, s }, { -s, s, s } };
        for (int i = 0; i < 8; i++) {
            float x = corners[i][0], y = corners[i][1], z = corners[i][2];
            float rx = x * cr - z * sr;
            float rz = x * sr + z * cr;
            verts[i] = new Vector3f(rx, y, rz);
        }
        int[][] faces = { { 0, 1, 2, 3 }, { 5, 4, 7, 6 }, { 1, 5, 6, 2 }, { 4, 0, 3, 7 }, { 4, 5, 1, 0 },
                { 3, 2, 6, 7 } };
        for (int[] face : faces) {
            addQuad(builder, m, n, verts[face[0]], verts[face[1]], verts[face[2]], verts[face[3]], 0, 0, 1, 1, r, g, b,
                    a);
        }
    }

    // --- RIPPLE (波紋) ---
    private void renderRipple(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float r, float g, float b, float a) {
        int rings = 5;
        for (int ring = 0; ring < rings; ring++) {
            float radius = (ring + 1) * 0.5f + Mth.sin(time * 0.3f) * 0.2f;
            float ringA = a * (1.0f - (float) ring / rings * 0.6f);
            float y = Mth.sin(time * 0.5f + ring * 0.5f) * 0.1f;
            int slices = 24;
            float width = 0.08f;
            for (int j = 0; j < slices; j++) {
                float theta0 = (float) j / slices * 2.0f * (float) Math.PI;
                float theta1 = (float) (j + 1) / slices * 2.0f * (float) Math.PI;
                Vector3f p00 = new Vector3f(Mth.cos(theta0) * (radius - width), y, Mth.sin(theta0) * (radius - width));
                Vector3f p10 = new Vector3f(Mth.cos(theta1) * (radius - width), y, Mth.sin(theta1) * (radius - width));
                Vector3f p11 = new Vector3f(Mth.cos(theta1) * (radius + width), y, Mth.sin(theta1) * (radius + width));
                Vector3f p01 = new Vector3f(Mth.cos(theta0) * (radius + width), y, Mth.sin(theta0) * (radius + width));
                addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, ringA);
            }
        }
    }

    // --- PARTICLE_MIST (霧) ---
    private void renderMist(Matrix4f m, Matrix3f n, VertexConsumer builder, float time,
            float wCold, float r, float g, float b, float a) {
        Random rng = new Random(42);
        int particles = 30 + (int) (wCold * 20);
        float size = 0.12f;
        for (int i = 0; i < particles; i++) {
            float px = (rng.nextFloat() - 0.5f) * 3.0f;
            float py = (rng.nextFloat() - 0.5f) * 3.0f;
            float pz = (rng.nextFloat() - 0.5f) * 3.0f;
            // ゆっくり漂う
            px += Mth.sin(time * 0.05f + i * 0.3f) * 0.3f;
            py += Mth.cos(time * 0.05f + i * 0.7f) * 0.3f;
            float pa = a * (0.2f + rng.nextFloat() * 0.3f);
            // ビルボードクワッド
            Vector3f p00 = new Vector3f(px - size, py - size, pz);
            Vector3f p10 = new Vector3f(px + size, py - size, pz);
            Vector3f p11 = new Vector3f(px + size, py + size, pz);
            Vector3f p01 = new Vector3f(px - size, py + size, pz);
            addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, r, g, b, pa);
        }
    }

    // ========================================================================
    // パーティクル雲 (属性別散布ポイント)
    // ========================================================================

    private void renderParticleCloud(PoseStack poseStack, VertexConsumer builder, float time, float scale,
            float wHeat, float wCold, float wEntropy, float wDivine,
            float r, float g, float b, float a) {
        poseStack.pushPose();

        Matrix4f m = poseStack.last().pose();
        Matrix3f n = poseStack.last().normal();

        int count = 12 + (int) ((wHeat + wCold + wEntropy + wDivine) * 8);
        float size = 0.06f * scale;
        long seed = (long) (time * 2);
        Random rng = new Random(seed);

        for (int i = 0; i < count; i++) {
            float angle = rng.nextFloat() * 2.0f * (float) Math.PI;
            float dist = (0.5f + rng.nextFloat() * 1.5f) * scale;
            float px = Mth.cos(angle + time * 0.1f * (1 + i % 3)) * dist;
            float py = (rng.nextFloat() - 0.5f) * scale * 2.0f;
            float pz = Mth.sin(angle + time * 0.1f * (1 + i % 3)) * dist;
            float pa = a * (0.3f + rng.nextFloat() * 0.4f);

            // 属性による色変化
            float pr = r, pg = g, pb = b;
            if (wHeat > 0.3f) {
                pr = 1.0f;
                pg *= 0.5f;
                pb *= 0.2f;
            } // 火の粉
            if (wCold > 0.3f) {
                pr = 0.7f;
                pg = 0.9f;
                pb = 1.0f;
            } // 霜
            if (wDivine > 0.3f) {
                pr = 1.0f;
                pg = 1.0f;
                pb = 0.8f;
            } // 光

            Vector3f p00 = new Vector3f(px - size, py - size, pz);
            Vector3f p10 = new Vector3f(px + size, py - size, pz);
            Vector3f p11 = new Vector3f(px + size, py + size, pz);
            Vector3f p01 = new Vector3f(px - size, py + size, pz);
            addQuad(builder, m, n, p00, p01, p11, p10, 0, 0, 1, 1, pr, pg, pb, pa);
        }

        poseStack.popPose();
    }

    // ========================================================================
    // ユーティリティ
    // ========================================================================

    private <T extends ICommand> void executeCommand(T command, EntitySciencePhenomenon entity, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
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

    private void addQuad(VertexConsumer builder, Matrix4f m, Matrix3f n,
            Vector3f p00, Vector3f p01, Vector3f p11, Vector3f p10,
            float u0, float v0, float u1, float v1,
            float r, float g, float b, float a) {
        // 表
        addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
        addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
        addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
        addVertex(builder, m, n, p10, u1, v0, r, g, b, a);
        // 裏
        addVertex(builder, m, n, p10, u1, v0, r, g, b, a);
        addVertex(builder, m, n, p11, u1, v1, r, g, b, a);
        addVertex(builder, m, n, p01, u0, v1, r, g, b, a);
        addVertex(builder, m, n, p00, u0, v0, r, g, b, a);
    }

    private void addTriangle(VertexConsumer builder, Matrix4f m, Matrix3f n,
            Vector3f p0, Vector3f p1, Vector3f p2,
            float r, float g, float b, float a) {
        // lightningのRenderTypeはQUAD形式なので三角形を退化四角形として描画
        addVertex(builder, m, n, p0, 0, 0, r, g, b, a);
        addVertex(builder, m, n, p1, 0, 1, r, g, b, a);
        addVertex(builder, m, n, p2, 1, 1, r, g, b, a);
        addVertex(builder, m, n, p2, 1, 0, r, g, b, a); // 退化
    }

    private void addVertex(VertexConsumer builder, Matrix4f m, Matrix3f n, Vector3f pos, float u, float v, float r,
            float g, float b, float a) {
        builder.vertex(m, pos.x(), pos.y(), pos.z())
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(n, 0, 1, 0)
                .endVertex();
    }
}