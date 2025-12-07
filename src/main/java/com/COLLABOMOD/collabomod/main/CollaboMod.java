package com.COLLABOMOD.collabomod.main;

import com.COLLABOMOD.collabomod.command.PsionCommand;
import com.COLLABOMOD.collabomod.register.*;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.COLLABOMOD.collabomod.main.tab.CollaboModBlockTab;
import com.COLLABOMOD.collabomod.main.tab.CollaboModTab;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("collabo_mod")
public class CollaboMod {
    //Mod_ID
    public static final String MOD_ID = "collabo_mod";
    //クリエイティブタブの登録
    public static final CreativeModeTab COLLABOMOD_TAB = new CollaboModTab();
    public static final CreativeModeTab COLLABOMOD_BLOCK_TAB = new CollaboModBlockTab();

    public CollaboMod(){
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.addListener(this::commonSetup);
        //この下に追加していく

        //アイテムの登録
        ItemRegister.register(eventBus);
        //ブロックの登録
        BlockRegister.register(eventBus);
        NetworkHandler.register();
        EntityRegister.register(eventBus);
        BlockEntityRegister.register(eventBus);
        MenuTypeRegister.register(eventBus);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
    private void onRegisterCommands(RegisterCommandsEvent event) {
        PsionCommand.register(event.getDispatcher());
    }

}
