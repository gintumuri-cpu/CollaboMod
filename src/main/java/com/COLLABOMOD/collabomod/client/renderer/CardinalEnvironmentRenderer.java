package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.ClientCardinalSystem;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.world.cardinal.EnvironmentChunkData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)
public class CardinalEnvironmentRenderer {

    private static final Random random = new Random();
    private static Field fpsField = null;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // ■ Phase 4: クライアント負荷監視 (FPS based LOD)
        int fps = getClientFPS(mc);

        // 描画距離の調整: FPSが高いなら遠くまで、低いなら近くのみ
        int renderDistance = (fps > 50) ? 4 : (fps > 30) ? 2 : 1;

        // パーティクル生成確率の調整
        float qualityFactor = (fps > 50) ? 1.0F : (fps > 30) ? 0.5F : 0.1F;

        ChunkPos playerChunk = mc.player.chunkPosition();
        Map<Long, EnvironmentChunkData> dataMap = ClientCardinalSystem.getAllData();

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                long chunkKey = ChunkPos.asLong(playerChunk.x + x, playerChunk.z + z);
                if (dataMap.containsKey(chunkKey)) {
                    ChunkPos cp = new ChunkPos(chunkKey);
                    renderChunkEffects(mc.level, cp, dataMap.get(chunkKey), qualityFactor);
                }
            }
        }
    }

    private static int getClientFPS(Minecraft mc) {
        try {
            if (fpsField == null) {
                // 開発環境と本番環境でフィールド名が違うため、両方を試す
                try {
                    // 開発環境 (Mojang mappings)
                    fpsField = Minecraft.class.getDeclaredField("fps");
                } catch (NoSuchFieldException e) {
                    // 本番環境 (SRG mappings)
                    fpsField = ObfuscationReflectionHelper.findField(Minecraft.class, "field_71470_ab");
                }
                fpsField.setAccessible(true);
            }
            return fpsField.getInt(mc);
        } catch (Exception e) {
            // エラー時は安全側に倒して60を返す（処理を止めないため）
            return 60;
        }
    }

    private static void renderChunkEffects(Level level, ChunkPos cp, EnvironmentChunkData data, float qualityFactor) {
        Map<Long, Float> temps = data.getAllTemperatures();

        temps.forEach((localPosKey, temp) -> {
            if (Math.abs(temp - 300.0F) < 50.0F) return;

            // 基本確率(0.02) * 品質係数(FPS依存)
            if (random.nextFloat() > (0.02F * qualityFactor)) return;

            int lx = BlockPos.getX(localPosKey);
            int ly = BlockPos.getY(localPosKey);
            int lz = BlockPos.getZ(localPosKey);
            BlockPos pos = cp.getBlockAt(lx, ly, lz);

            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

            if (temp > 1000.0F) {
                if (temp > 3000.0F) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0.1, 0);
                } else {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
                    if (random.nextFloat() < 0.1F) {
                        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
                    }
                }
            } else if (temp < 200.0F) {
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.05, 0);
            }
        });
    }
}