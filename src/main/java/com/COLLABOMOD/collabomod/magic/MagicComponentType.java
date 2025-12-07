package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.entity.EntityMaterialBurst;
import com.COLLABOMOD.collabomod.network.NetworkHandler;
import com.COLLABOMOD.collabomod.network.PacketMaterialBurst;
import com.COLLABOMOD.collabomod.util.MagicSpellType;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public enum MagicComponentType {

    // ■ 1. 射撃系コンポーネント
    // エア・バレット
    PROJECTILE_AIR(
            (level, caster, origin, rotX, rotY) -> {
                EntityAirBullet bullet = new EntityAirBullet(level, caster);
                bullet.setPos(origin.x, origin.y, origin.z);
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
            20
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
            40
    ),

    // ■ 2. 戦略級コンポーネント
    // マテリアル・バースト
    MATERIAL_BURST(
            (level, caster, origin, rotX, rotY) -> {
                EntityMaterialBurst burst = new EntityMaterialBurst(level, origin.x, origin.y, origin.z);
                level.addFreshEntity(burst);
                level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 100.0F, 0.5F);
            },
            // 優先度100！
            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F),
            50
    );

    // --- フィールド ---
    public final ComponentLogic logic;
    public final VisualMetadata visuals; // 変更: 直接Metadataを持つ
    public final int cost;

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
}