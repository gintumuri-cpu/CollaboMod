package com.COLLABOMOD.collabomod.item;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemEmptyCAD extends ItemCAD {

    public ItemEmptyCAD() {
        super();
        // ItemCADのコンストラクタでタブ登録済みですが、
        // 必要ならここでプロパティを再設定してもOK
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        // NBTがない場合は「空き容量あり」などを表示
        if (!stack.hasTag() || !stack.getTag().contains("Components")) {
            tooltip.add(new TextComponent("§7[未設定] 魔法式がインストールされていません"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
