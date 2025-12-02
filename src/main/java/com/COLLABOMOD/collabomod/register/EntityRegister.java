package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegister {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, CollaboMod.MOD_ID);

    // グラム・デモリッションの登録
    public static final RegistryObject<EntityType<EntityGramDemolition>> GRAM_DEMOLITION = ENTITIES.register("gram_demolition",
            () -> EntityType.Builder.<EntityGramDemolition>of(EntityGramDemolition::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // 当たり判定サイズ
                    .clientTrackingRange(4) // 描画距離
                    .updateInterval(20)
                    .build("gram_demolition"));

    public static void register(IEventBus eventBus) {

        ENTITIES.register(eventBus);
    }
}
