package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.capability.MagicStats;
import com.COLLABOMOD.collabomod.entity.EntityAirBullet;
import com.COLLABOMOD.collabomod.entity.EntityMagicSequence;
import com.COLLABOMOD.collabomod.util.MagicSpellType; // 旧Enumは削除推奨だが、EntityMagicSequenceがまだ依存しているなら残す
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpellAirBullet implements IMagicSpell {

    private final Random random = new Random();

    @Override
    public int getCost() {
        return 20;
    }

    @Override
    public int getCastTime() {
        return 15; // 0.75秒
    }

    // ■ 起動処理 (ItemCADから移植)
    @Override
    public boolean initiate(Level level, Player player, MagicStats stats) {
        // 1. ターゲット捕捉と座標決定
        double range = 30.0D;
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 endPos = eyePos.add(look.scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);

        EntityHitResult entityResult = ProjectileUtil.getEntityHitResult(
                level, player, eyePos, endPos, searchBox, (e) -> !e.isSpectator() && e.isPickable());

        Vec3 targetPos;
        if (entityResult != null) {
            LivingEntity target = (LivingEntity) entityResult.getEntity();
            targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
        } else {
            HitResult blockResult = player.pick(range, 0.0F, false);
            targetPos = blockResult.getLocation();
        }

        // 2. ランダム配置
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 3.0 + random.nextDouble() * 2.0;
        double heightOffset = (random.nextDouble() - 0.5) * 4.0;

        Vec3 spawnPos = targetPos.add(Math.cos(angle) * dist, heightOffset + 2.0, Math.sin(angle) * dist);

        // 3. 魔法式の生成
        List<MagicComponentType> components = new ArrayList<>();
        components.add(MagicComponentType.PROJECTILE_AIR);

        // ビジュアルメタデータを取得
        VisualMetadata visuals = MagicComponentType.PROJECTILE_AIR.visuals;

        EntityMagicSequence sequence = new EntityMagicSequence(
                level, player, components, visuals, 15, spawnPos
        );

        // 向きの計算 (ターゲットの方を向く)
        double dX = targetPos.x - spawnPos.x;
        double dY = targetPos.y - spawnPos.y;
        double dZ = targetPos.z - spawnPos.z;
        double dist2d = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) (Math.atan2(dZ, dX) * (180 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dY, dist2d) * (180 / Math.PI));

        sequence.setYRot(yaw);
        sequence.setXRot(pitch);

        level.addFreshEntity(sequence);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK, SoundSource.PLAYERS, 1.0F, 2.0F);

        return true; // 成功
    }

    // ■ 発動処理 (EntityMagicSequenceから移植)
    @Override
    public void execute(Level level, LivingEntity caster, Vec3 origin, float rotX, float rotY) {
        EntityAirBullet bullet = new EntityAirBullet(level, caster);
        bullet.setPos(origin.x, origin.y, origin.z);

        // 魔法陣の向きに発射
        bullet.shootFromRotation(caster, rotX, rotY, 0.0F, 3.0F, 0.5F);

        level.addFreshEntity(bullet);
        level.playSound(null, origin.x, origin.y, origin.z,
                SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 2.0F, 1.5F);
    }
}
