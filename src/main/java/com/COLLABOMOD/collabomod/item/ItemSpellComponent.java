//package com.COLLABOMOD.collabomod.item;
//
//import com.COLLABOMOD.collabomod.magic.MagicComponentType;
//import com.COLLABOMOD.collabomod.main.CollaboMod;
//import net.minecraft.network.chat.TextComponent;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.item.TooltipFlag;
//import net.minecraft.world.level.Level;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//
//public class ItemSpellComponent extends Item {
//
//    // 古いクラスではなく、新しい Enum を保持するように変更
//    private final MagicComponentType type;
//
//    public ItemSpellComponent(MagicComponentType type) {
//        super(new Item.Properties().tab(CollaboMod.COLLABOMOD_TAB));
//        this.type = type;
//    }
//
//    public MagicComponentType getComponentType() {
//        return type;
//    }
//
//    @Override
//    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
//        tooltip.add(new TextComponent("§7種別: " + type.name()));
//        tooltip.add(new TextComponent("§9コスト: " + type.cost));
//        super.appendHoverText(stack, level, tooltip, flag);
//    }
//}
