package com.COLLABOMOD.collabomod.main.tab;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class CollaboModBlockTab extends CreativeModeTab {
    public CollaboModBlockTab() {
        super("collabomod_block_tab");
    }

    @Override
    public ItemStack makeIcon() {
        return new ItemStack(Blocks.BEDROCK);
    }
}
