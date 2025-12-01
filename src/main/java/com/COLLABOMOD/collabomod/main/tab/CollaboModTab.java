package com.COLLABOMOD.collabomod.main.tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CollaboModTab extends CreativeModeTab {
    public CollaboModTab() {
        super("collabomod_tab");
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(Items.DIAMOND_PICKAXE);
    }
}
