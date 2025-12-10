package com.COLLABOMOD.collabomod.register;

import com.COLLABOMOD.collabomod.entity.*;
import com.COLLABOMOD.collabomod.main.CollaboMod;
import com.COLLABOMOD.collabomod.entity.EntitySciencePhenomenon;
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
    public static final RegistryObject<EntityType<EntityMistDispersion>> MIST_DISPERSION = ENTITIES.register("mist_dispersion",
            () -> EntityType.Builder.<EntityMistDispersion>of(EntityMistDispersion::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F) // 弾は小さく目立たない
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("mist_dispersion"));

    public static final RegistryObject<EntityType<EntitySciencePhenomenon>> MATERIAL_BURST = ENTITIES.register("material_burst",
            () -> EntityType.Builder.<EntitySciencePhenomenon>of(
                            // ★修正: ::new でエラーが出る場合は、明示的にラムダ式で書く
                            (type, level) -> new EntitySciencePhenomenon(type, level),
                            MobCategory.MISC
                    )
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("material_burst"));

    public static final RegistryObject<EntityType<EntityAirBullet>> AIR_BULLET = ENTITIES.register("air_bullet",
            () -> EntityType.Builder.<EntityAirBullet>of(EntityAirBullet::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // 当たり判定のサイズ
                    .clientTrackingRange(4) // 描画距離
                    .updateInterval(20)     // 更新頻度
                    .build("air_bullet"));

    public static final RegistryObject<EntityType<EntityMagicSequence>> MAGIC_SEQUENCE = ENTITIES.register("magic_sequence",
            () -> EntityType.Builder.<EntityMagicSequence>of(EntityMagicSequence::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F) // サイズ
                    .clientTrackingRange(10) // 描画距離
                    .updateInterval(1)       // 更新頻度（回転アニメーションのため1）
                    .build("magic_sequence"));

    public static final RegistryObject<EntityType<EntitySciencePhenomenon>> SCIENCE_PHENOMENON = ENTITIES.register("science_phenomenon",
            () -> EntityType.Builder.<EntitySciencePhenomenon>of((t, l) -> new EntitySciencePhenomenon(t, l), MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1).build("science_phenomenon"));

    public static void register(IEventBus eventBus) {

        ENTITIES.register(eventBus);
    }
}
