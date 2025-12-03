package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.client.ClientEvents;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemThirdEye extends Item{

    public ItemThirdEye() {
        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB).stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // サーバー側は何もしない（クライアントからの座標通知を待つ）
        if (!level.isClientSide) {
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }

        // クライアント側: エレメンタル・サイトを「サード・アイモード」で起動
        // ※ClientEventsのメソッドを直接呼び出す形になります
        ClientEvents.toggleThirdEyeMode();

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
