package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.ClientCardinalSystem;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.world.cardinal.EnvironmentChunkData;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Random;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, value = Dist.CLIENT)
public class CardinalEnvironmentRenderer {

    private static final Random random = new Random();

    // 描画範囲（ブロック数）
    private static final int VISUAL_RANGE = 24;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;

        BlockPos playerPos = mc.player.blockPosition();

        // クライアントが保持している全チャンクデータを走査
        Map<Long, EnvironmentChunkData> allData = ClientCardinalSystem.getAllData();

        allData.forEach((chunkKey, data) -> {
            ChunkPos chunkPos = new ChunkPos(chunkKey);

            // プレイヤーから遠すぎるチャンクは無視 (軽量化)
            // チャンクの中心座標で簡易判定
            BlockPos chunkCenter = chunkPos.getMiddleBlockPosition(0);
            if (chunkCenter.distSqr(playerPos) > (VISUAL_RANGE + 16) * (VISUAL_RANGE + 16)) return;

            // 1. 温度の可視化
            data.getAllTemperatures().forEach((localKey, temp) -> {
                // 300K(常温)付近は無視
                if (Math.abs(temp - 300.0F) < 50.0F) return;

                // 確率判定 (温度が高いほど高確率)
                // 例: 1000Kで 5% くらいの確率
                float chance = (Math.abs(temp - 300.0F) / 10000.0F);
                if (random.nextFloat() > chance) return;

                BlockPos targetPos = restoreWorldPos(chunkPos, localKey);

                // 距離チェック (詳細)
                if (targetPos.distSqr(playerPos) > VISUAL_RANGE * VISUAL_RANGE) return;

                // 色の決定 (高温=赤, 低温=白青)
                Vector3f color;
                if (temp > 300.0F) {
                    color = new Vector3f(1.0F, 0.4F, 0.0F); // Orange-Red
                } else {
                    color = new Vector3f(0.5F, 0.8F, 1.0F); // Light Blue
                }

                spawnParticle(level, targetPos, color);
            });

            // 2. サイオン濃度の可視化
            data.getAllPsionDensities().forEach((localKey, density) -> {
                if (density <= 100.0F) return; // 通常以下は無視

                // 濃度が高いほど高確率
                float chance = (density - 100.0F) / 1000.0F;
                if (random.nextFloat() > chance) return;

                BlockPos targetPos = restoreWorldPos(chunkPos, localKey);

                if (targetPos.distSqr(playerPos) > VISUAL_RANGE * VISUAL_RANGE) return;

                // サイオンカラー (シアン)
                spawnParticle(level, targetPos, new Vector3f(0.0F, 1.0F, 1.0F));
            });
        });
    }

    // ローカル座標キー(Long)からワールド座標(BlockPos)を復元
    private static BlockPos restoreWorldPos(ChunkPos chunkPos, long localKey) {
        BlockPos local = BlockPos.of(localKey);
        int x = chunkPos.getMinBlockX() + local.getX();
        int y = local.getY();
        int z = chunkPos.getMinBlockZ() + local.getZ();
        return new BlockPos(x, y, z);
    }

    private static void spawnParticle(Level level, BlockPos pos, Vector3f color) {
        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();

        // DustParticle (RGB + Scale)
        level.addParticle(new DustParticleOptions(color, 1.0F), x, y, z, 0, 0, 0);
    }
}