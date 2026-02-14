package com.COLLABOMOD.collabomod.main;

import com.COLLABOMOD.collabomod.client.KeyInit;
import com.COLLABOMOD.collabomod.client.particle.MagicGlowParticle;
import com.COLLABOMOD.collabomod.register.EntityRegister;
import com.COLLABOMOD.collabomod.register.ParticleRegister;
import net.minecraft.client.renderer.entity.EntityRenderers;
import com.COLLABOMOD.collabomod.client.renderer.RenderMagicSequence;
import com.COLLABOMOD.collabomod.client.renderer.RenderUniversalMagic;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import com.COLLABOMOD.collabomod.register.MenuTypeRegister;
import com.COLLABOMOD.collabomod.client.gui.MagicConsoleScreen;
import net.minecraftforge.api.distmarker.Dist;
import com.COLLABOMOD.collabomod.register.ParticleRegister;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;

@Mod.EventBusSubscriber(modid = CollaboMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventBusSubscriber {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // グラム・デモリッションはモデルを持たず、パーティクルだけで表現するため
        // "NoopRenderer"（何もしないレンダラー＝透明）を割り当てます
        EntityRenderers.register(EntityRegister.GRAM_DEMOLITION.get(), NoopRenderer::new);
        EntityRenderers.register(EntityRegister.MIST_DISPERSION.get(), NoopRenderer::new);
        EntityRenderers.register(EntityRegister.AIR_BULLET.get(), NoopRenderer::new);
        EntityRenderers.register(EntityRegister.MAGIC_SEQUENCE.get(), RenderMagicSequence::new);
        EntityRenderers.register(EntityRegister.SCIENCE_PHENOMENON.get(), RenderUniversalMagic::new);
        EntityRenderers.register(EntityRegister.RESIDUAL_FIELD.get(), NoopRenderer::new);
        KeyInit.register();

        event.enqueueWork(() -> {
            MenuScreens.register(MenuTypeRegister.MAGIC_CONSOLE_MENU.get(), MagicConsoleScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
        Minecraft.getInstance().particleEngine.register(
                ParticleRegister.GLOW_PARTICLE.get(),
                MagicGlowParticle.Provider::new);
    }
}
