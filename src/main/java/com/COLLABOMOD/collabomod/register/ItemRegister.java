package com.COLLABOMOD.collabomod.register;


import com.COLLABOMOD.collabomod.item.*;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegister {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, CollaboMod.MOD_ID);

    public static final RegistryObject<Item> TEST_CAD = ITEMS.register("test_cad", ItemCAD::new);
    public static final RegistryObject<Item> SILVER_HORN = ITEMS.register("silver_horn", ItemSilverHorn::new);
    public static final RegistryObject<Item> THIRD_EYE = ITEMS.register("third_eye", ItemThirdEye::new);
    public static final RegistryObject<Item> EMPTY_CAD = ITEMS.register("empty_cad", ItemEmptyCAD::new);
    public static final RegistryObject<Item> EVALUATOR = ITEMS.register("evaluator", ItemEvaluator::new);

//    public static final RegistryObject<Item> COMP_AIR_BULLET = ITEMS.register("comp_air_bullet",
//            () -> new ItemSpellComponent(com.COLLABOMOD.collabomod.magic.MagicComponentType.PROJECTILE_AIR));
//
//    public static final RegistryObject<Item> COMP_GRAM_DEMOLITION = ITEMS.register("comp_gram_demolition",
//            () -> new ItemSpellComponent(com.COLLABOMOD.collabomod.magic.MagicComponentType.PROJECTILE_GRAM));
//
//    public static final RegistryObject<Item> COMP_MATERIAL_BURST = ITEMS.register("comp_material_burst",
//            () -> new ItemSpellComponent(com.COLLABOMOD.collabomod.magic.MagicComponentType.MATERIAL_BURST));

    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }

}
