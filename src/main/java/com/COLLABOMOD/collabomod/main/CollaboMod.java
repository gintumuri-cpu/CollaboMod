package com.COLLABOMOD.collabomod.main;

import com.COLLABOMOD.collabomod.command.PsionCommand;
import com.COLLABOMOD.collabomod.learning.AIInferenceCache;
import com.COLLABOMOD.collabomod.learning.ExternalAIConfig;
import com.COLLABOMOD.collabomod.learning.ExternalAIService;
import com.COLLABOMOD.collabomod.register.*;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.COLLABOMOD.collabomod.main.tab.CollaboModBlockTab;
import com.COLLABOMOD.collabomod.main.tab.CollaboModTab;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("collabo_mod")
public class CollaboMod {
    // Mod_ID
    public static final String MOD_ID = "collabo_mod";
    // クリエイティブタブの登録
    public static final CreativeModeTab COLLABOMOD_TAB = new CollaboModTab();
    public static final CreativeModeTab COLLABOMOD_BLOCK_TAB = new CollaboModBlockTab();

    public CollaboMod() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        eventBus.addListener(this::commonSetup);
        // この下に追加していく

        // アイテムの登録
        ItemRegister.register(eventBus);
        // ブロックの登録
        BlockRegister.register(eventBus);
        NetworkHandler.register();
        EntityRegister.register(eventBus);
        BlockEntityRegister.register(eventBus);
        MenuTypeRegister.register(eventBus);
        ParticleRegister.register(eventBus);

        // Forgeイベントバスにサーバー停止リスナーを登録
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 外部AI通信レイヤーの初期化
        ExternalAIConfig.load();
        ExternalAIService.getInstance().initialize();
        AIInferenceCache.getInstance().load();
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PsionCommand.register(event.getDispatcher());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        // キャッシュをディスクに保存
        AIInferenceCache.getInstance().save();
        // HTTPクライアントのスレッドプールをシャットダウン
        ExternalAIService.getInstance().shutdown();
        System.out.println("[External AI] Shutdown complete. Cache saved.");
    }
}
