package com.COLLABOMOD.collabomod.client;

import com.COLLABOMOD.collabomod.capability.MagicStatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ClientPacketHandler {
    public static void handlePacket(int current, int max, int area, int load) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(MagicStatsProvider.PLAYER_MAGIC_STATS).ifPresent(stats -> {
                stats.syncClient(current, max, area, load);
            });
        }
    }
}
