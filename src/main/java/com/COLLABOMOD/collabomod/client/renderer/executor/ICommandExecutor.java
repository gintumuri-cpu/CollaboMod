package com.COLLABOMOD.collabomod.client.renderer.executor;

import com.COLLABOMOD.collabomod.magic.command.ICommand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface ICommandExecutor<T extends ICommand> {
    void execute(T command, Level level, Entity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight);
}