package com.COLLABOMOD.collabomod.main;


import com.COLLABOMOD.collabomod.client.KeyInit;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.COLLABOMOD.collabomod.client.renderer.RenderMaterialBurst;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventBusSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
            // グラム・デモリッションはモデルを持たず、パーティクルだけで表現するため
            // "NoopRenderer"（何もしないレンダラー＝透明）を割り当てます
            EntityRenderers.register(EntityRegister.GRAM_DEMOLITION.get(), NoopRenderer::new);
            EntityRenderers.register(EntityRegister.MIST_DISPERSION.get(), NoopRenderer::new);
            KeyInit.register();

            // ■ 修正: マテリアル・バーストに専用レンダラーを割り当て
            EntityRenderers.register(EntityRegister.MATERIAL_BURST.get(), RenderMaterialBurst::new);
        }
    }

