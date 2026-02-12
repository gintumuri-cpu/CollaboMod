package com.COLLABOMOD.collabomod.client.renderer.executor;

import com.COLLABOMOD.collabomod.magic.command.SoundCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class SoundExecutor implements ICommandExecutor<SoundCommand> {
    @Override
    public void execute(SoundCommand command, Level level, Entity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                ForgeRegistries.SOUND_EVENTS.getValue(command.soundId),
                SoundSource.NEUTRAL, command.volume, command.pitch, false);
    }
}