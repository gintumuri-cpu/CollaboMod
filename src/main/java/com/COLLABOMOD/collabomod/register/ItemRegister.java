package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.item.ItemCAD;
import com.COLLABOMOD.collabomod.item.ItemSilverHorn;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegister {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CollaboMod.MOD_ID);

    public static final RegistryObject<Item> TEST_CAD = ITEMS.register("test_cad", ItemCAD::new);
    public static final RegistryObject<Item> SILVER_HORN = ITEMS.register("silver_horn", ItemSilverHorn::new);

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }

}
