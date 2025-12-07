package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.COLLABOMOD.collabomod.block.MagicConsoleBlock;

import java.util.function.Supplier;

public class BlockRegister {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, CollaboMod.MOD_ID);
    public static final RegistryObject<Block> MAGIC_CONSOLE = registerBlockItem("magic_console", MagicConsoleBlock::new);





    // ブロックアイテム作成用メソッド 基本的に触らない
    private static <T extends Block> RegistryObject<T> registerBlockItem(String name, Supplier<T> supplier) {
        // レジストリにブロックを追加
        RegistryObject<T> block = BLOCKS.register(name, supplier);
        // ブロックアイテムをアイテムレジストリに追加
        ItemRegister.ITEMS.register(name,
                () -> new BlockItem(block.get(), new Item.Properties().tab(CollaboMod.COLLABOMOD_BLOCK_TAB)));
        return block;
    }

    public static void register(IEventBus eventBus) {
        // レジストリをイベントバスに登録
        BLOCKS.register(eventBus);
    }
}
