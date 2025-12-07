package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import com.mojang.math.Vector3f;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public enum MagicComponentType {

    // ■■■ 1. 複合魔法コンポーネント (ItemRegisterで使用) ■■■
    // これらが「ディスク」として登録されているものです

    // エア・バレット
    PROJECTILE_AIR(
            (level, caster, origin, rotX, rotY) -> {
                EntityAirBullet bullet = new EntityAirBullet(level, caster);
                bullet.setPos(origin.x, origin.y, origin.z);

                // 回転からベクトル計算
                float f = 0.017453292F;
                double x = -Math.sin(rotY * f) * Math.cos(rotX * f);
                double y = -Math.sin(rotX * f);
                double z = Math.cos(rotY * f) * Math.cos(rotX * f);

                bullet.shoot(x, y, z, 3.0F, 0.5F);
                level.addFreshEntity(bullet);
                level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
            },
            // VisualMetadata(ID, Priority, Color, Scale)
            new VisualMetadata("magic_circle", 5, new Vector3f(0.9F, 0.9F, 1.0F), 1.0F),
            20 // cost
    ),

    // グラム・デモリッション
    PROJECTILE_GRAM(
            (level, caster, origin, rotX, rotY) -> {
                EntityGramDemolition projectile = new EntityGramDemolition(level, caster);
                projectile.setPos(origin.x, origin.y, origin.z);
                projectile.shootFromRotation(caster, rotX, rotY, 0.0F, 4.0F, 1.0F);
                projectile.setDamageMultiplier(1.0F);
                level.addFreshEntity(projectile);
                level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.2F, 0.9F, 1.0F), 1.0F),
            40 // cost
    ),

    // マテリアル・バースト
    MATERIAL_BURST(
            (level, caster, origin, rotX, rotY) -> {
                EntityMaterialBurst burst = new EntityMaterialBurst(level, origin.x, origin.y, origin.z);
                level.addFreshEntity(burst);
                level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 100.0F, 0.5F);
            },
            // 優先度100 (見た目を強制上書き)
            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F),
            50 // cost
    );

    // ■■■ 2. 基礎コンポーネント (将来の拡張用) ■■■
    /* ACT_SHOOT( ... ),
    ATTRIB_AIR( ... ),
    など、細分化した部品は後でここに追加していけばOKです。
    現在はエラー回避のため、上記3つがあれば動きます。
    */


    // --- フィールド変数 ---
    public final ComponentLogic logic;
    public final VisualMetadata visuals;
    public final int cost; // ★これがないとSpellResolverでエラーになります

    // コンストラクタ
    MagicComponentType(ComponentLogic logic, VisualMetadata visuals, int cost) {
        this.logic = logic;
        this.visuals = visuals;
        this.cost = cost;
    }

    // ロジック実行用インターフェース
    @FunctionalInterface
    public interface ComponentLogic {
        void execute(Level level, LivingEntity caster, Vec3 origin, float rotX, float rotY);
    }

    // SpellContext用のapplyメソッド (将来的に使用)
    public void apply(SpellContext context) {
        // 現在はロジック直書き型なので、ここは空でもOK
        // 将来的にパラメータ変動型にする場合、ここに記述します
    }
}