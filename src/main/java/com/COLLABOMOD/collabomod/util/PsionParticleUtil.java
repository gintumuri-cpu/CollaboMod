package com.COLLABOMOD.collabomod.util;

import com.mojang.math.Vector3f; // もしエラーが出る場合は net.minecraft.core.Vector3f か com.mojang.math.Vector3f
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PsionParticleUtil {
    // サイオンの基本色（シアンブルー）
    // DustParticleOptions は RGB (0.0 ~ 1.0) で指定します
    // R:0.2, G:0.9, B:1.0 -> 明るい水色
    private static final Vector3f PSION_COLOR = new Vector3f(0.2F, 0.9F, 1.0F);

    /**
     * 指定した位置から、視線方向に垂直なリング（波動）を描画する
     * @param level ワールド
     * @param center 中心座標（銃口の位置）
     * @param direction 飛ばす方向（視線ベクトル）
     * @param radius 半径
     * @param density 粒子の密度（円を何分割するか）
     */
    public static void spawnPsionRing(Level level, Vec3 center, Vec3 direction, float radius, int density) {
        // 方向ベクトルを正規化
        Vec3 view = direction.normalize();

        // ■ 円を描くための基準軸（右ベクトルと上ベクトル）を計算する
        // 1. 「右ベクトル」 = 「視線」と「真上(0,1,0)」の外積
        Vec3 right = view.cross(new Vec3(0, 1, 0)).normalize();

        // ※真上や真下を向いていると外積が0になるため対策
        if (right.lengthSqr() < 1.0E-4D) {
            // 視線がY軸並行なら、X軸を右とする
            right = new Vec3(1, 0, 0);
        }

        // 2. 「上ベクトル」 = 「右」と「視線」の外積（これでカメラに対して垂直な面ができる）
        Vec3 up = right.cross(view).normalize();

        // ■ パーティクルの設定
        // scale: 1.0F (大きさ)
        ParticleOptions particle = new DustParticleOptions(PSION_COLOR, 1.0F);

        // ■ 円周上にパーティクルを配置
        for (int i = 0; i < density; i++) {
            // 角度 (0 ～ 2π)
            double angle = 2 * Math.PI * i / density;

            // 円周上の点の計算
            // P = Center + (Right * cosθ * r) + (Up * sinθ * r)
            double xOffset = (right.x * Math.cos(angle) + up.x * Math.sin(angle)) * radius;
            double yOffset = (right.y * Math.cos(angle) + up.y * Math.sin(angle)) * radius;
            double zOffset = (right.z * Math.cos(angle) + up.z * Math.sin(angle)) * radius;

            level.addParticle(particle,
                    center.x + xOffset,
                    center.y + yOffset,
                    center.z + zOffset,
                    0, 0, 0); // 速度は0
        }
    }
}
