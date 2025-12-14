package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegister {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, CollaboMod.MOD_ID);

    // 単純なパーティクルタイプとして登録
    public static final RegistryObject<SimpleParticleType> GLOW_PARTICLE = PARTICLES.register("glow_particle", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}