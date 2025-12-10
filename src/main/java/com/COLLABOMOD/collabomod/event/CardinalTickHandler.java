package com.COLLABOMOD.collabomod.event;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.world.cardinal.WorldCardinalSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID)
public class CardinalTickHandler {

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        // サーバー側のTick終了時のみ実行
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            if (event.world instanceof ServerLevel serverLevel) {
                // カーディナルシステムを取得して時を進める
                WorldCardinalSystem cardinal = WorldCardinalSystem.get(serverLevel);
                cardinal.tick(serverLevel);
            }
        }
    }
}
