package com.COLLABOMOD.collabomod.client.renderer.executor;

import com.COLLABOMOD.collabomod.client.texture.ProceduralTextureManager;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
import com.COLLABOMOD.collabomod.magic.command.TextureCommand;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class TextureExecutor implements ICommandExecutor<TextureCommand> {

    @Override
    public void execute(TextureCommand command, Level level, Entity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (entity instanceof EntitySciencePhenomenon phenomenon) {
            // テクスチャを生成または取得
            ResourceLocation texture = ProceduralTextureManager.getInstance().getTexture(command.seed, command.type, command.color);
            
            // エンティティにセット (レンダラーがこれを参照する)
            // 注意: これはクライアントサイドのみの処理。
            // サーバーサイドでTextureCommandが発行されても、実際のテクスチャ生成は各クライアントで行われる。
            phenomenon.setDynamicTexture(texture);
        }
    }
}