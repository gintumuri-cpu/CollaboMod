package com.COLLABOMOD.collabomod.client.renderer.executor;

import com.COLLABOMOD.collabomod.magic.command.ParticleCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ParticleExecutor implements ICommandExecutor<ParticleCommand> {
    @Override
    public void execute(ParticleCommand command, Level level, Entity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(command.particleId);
        
        if (particleType instanceof ParticleOptions) {
            ParticleOptions particleOptions = (ParticleOptions) particleType;
            for (int i = 0; i < command.count; i++) {
                // ここでは単純にエンティティの位置にスポーンさせる
                level.addParticle(particleOptions, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(),
                        (level.random.nextDouble() - 0.5) * 2.0,
                        (level.random.nextDouble() - 0.5) * 2.0,
                        (level.random.nextDouble() - 0.5) * 2.0);
            }
        }
    }
}