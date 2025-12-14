package com.COLLABOMOD.collabomod.client.renderer;

import com.COLLABOMOD.collabomod.client.ClientCardinalSystem;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.register.ParticleRegister; // 追加
import com.COLLABOMOD.collabomod.world.cardinal.EnvironmentChunkData;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
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
    private static final int VISUAL_RANGE = 24;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;

        BlockPos playerPos = mc.player.blockPosition();
        Map<Long, EnvironmentChunkData> allData = ClientCardinalSystem.getAllData();

        allData.forEach((chunkKey, data) -> {
            ChunkPos chunkPos = new ChunkPos(chunkKey);
            BlockPos chunkCenter = chunkPos.getMiddleBlockPosition(0);
            if (chunkCenter.distSqr(playerPos) > (VISUAL_RANGE + 16) * (VISUAL_RANGE + 16)) return;

            // 1. 温度
            data.getAllTemperatures().forEach((localKey, temp) -> {
                if (Math.abs(temp - 300.0F) < 50.0F) return;

                float chance = (Math.abs(temp - 300.0F) / 5000.0F); // 確率調整
                if (random.nextFloat() > chance) return;

                BlockPos targetPos = restoreWorldPos(chunkPos, localKey);
                if (targetPos.distSqr(playerPos) > VISUAL_RANGE * VISUAL_RANGE) return;

                Vector3f color;
                if (temp > 300.0F) {
                    color = new Vector3f(1.0F, 0.3F, 0.1F); // 熱: オレンジ
                } else {
                    color = new Vector3f(0.5F, 0.8F, 1.0F); // 冷: 水色
                }
                spawnParticle(level, targetPos, color);
            });

            // 2. サイオン
            data.getAllPsionDensities().forEach((localKey, density) -> {
                if (density <= 100.0F) return;
                float chance = (density - 100.0F) / 1000.0F;
                if (random.nextFloat() > chance) return;

                BlockPos targetPos = restoreWorldPos(chunkPos, localKey);
                if (targetPos.distSqr(playerPos) > VISUAL_RANGE * VISUAL_RANGE) return;

                spawnParticle(level, targetPos, new Vector3f(0.0F, 1.0F, 0.8F)); // サイオン: シアン
            });
        });
    }

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

        // ■ 変更: カスタムパーティクルを使用
        // colorを速度引数(dx, dy, dz)として渡すトリックを使用
        level.addParticle(ParticleRegister.GLOW_PARTICLE.get(), x, y, z, color.x(), color.y(), color.z());
    }
}