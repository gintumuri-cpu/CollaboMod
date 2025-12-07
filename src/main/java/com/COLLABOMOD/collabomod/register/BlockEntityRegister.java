package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.block.entity.MagicConsoleBlockEntity;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegister {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, CollaboMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<MagicConsoleBlockEntity>> MAGIC_CONSOLE_ENTITY = BLOCK_ENTITIES.register("magic_console_entity",
            () -> BlockEntityType.Builder.of(MagicConsoleBlockEntity::new, BlockRegister.MAGIC_CONSOLE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}