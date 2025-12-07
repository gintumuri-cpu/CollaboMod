package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.capability.MagicStats;
import com.COLLABOMOD.collabomod.entity.EntityGramDemolition;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SpellGramDemolition implements IMagicSpell {

    @Override
    public int getCost() {
        return 40; // コスト
    }

    @Override
    public int getCastTime() {
        return 0; // 即時発動
    }

    // ■ 起動処理 (ItemCADから移植)
    @Override
    public boolean initiate(Level level, Player player, MagicStats stats) {
        int calcArea = stats.getCalculationArea();
        float talentFactor = calcArea / 100.0F;
        float hardwarePerformance = 0.8F; // 本来はCADから取得すべきですが、一旦固定で

        EntityGramDemolition projectile = new EntityGramDemolition(level, player);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 4.0F, 1.0F);
        projectile.setDamageMultiplier(talentFactor * hardwarePerformance);

        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);

        return true;
    }

    // ■ 発動処理 (グラムには魔法式がないので呼ばれない)
    @Override
    public void execute(Level level, LivingEntity caster, Vec3 origin, float rotX, float rotY) {
        // Do nothing
    }
}
