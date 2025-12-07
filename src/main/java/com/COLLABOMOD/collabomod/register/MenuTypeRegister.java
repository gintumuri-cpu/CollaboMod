package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.gui.MagicConsoleMenu;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuTypeRegister {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.CONTAINERS, CollaboMod.MOD_ID);

    public static final RegistryObject<MenuType<MagicConsoleMenu>> MAGIC_CONSOLE_MENU = MENUS.register("magic_console_menu",
            () -> IForgeMenuType.create(MagicConsoleMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
