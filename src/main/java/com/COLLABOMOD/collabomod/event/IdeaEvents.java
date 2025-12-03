package com.COLLABOMOD.collabomod.event;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.world.idea.IdeaDimensionData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID)
public class IdeaEvents {

    // エンティティがダメージを受ける「直前」に、その状態をイデアに保存する
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && !entity.level.isClientSide) {
            if (entity.level instanceof ServerLevel serverLevel) {
                // イデアにアクセス
                IdeaDimensionData idea = IdeaDimensionData.get(serverLevel);

                // 現在（ダメージを受ける前）の状態を記録
                idea.recordEntityState(entity, serverLevel.getGameTime());

                // ログ確認用（実装時は削除可）
                // System.out.println("Eidos Recorded: " + entity.getName().getString() + " HP:" + entity.getHealth());
            }
        }
    }
}
