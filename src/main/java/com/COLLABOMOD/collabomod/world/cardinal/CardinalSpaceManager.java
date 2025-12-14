package com.COLLABOMOD.collabomod.world.cardinal;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnchor;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class CardinalSpaceManager {

    private static final Random random = new Random();

    /**
     * カーディナル・システムによる最適座標の算出
     */
    public static Vec3 findOptimalPosition(LivingEntity caster, LivingEntity target, VisualMetadata meta) {
        EnumMagicAnchor anchor = meta.anchorType;
        Vec3 origin = caster.position();

        // ■ 基準ベクトルの計算
        // 視線(LookAngle)ではなく、体の向き(Forward)を使うことで、視点移動によるブレをなくす
        // Y軸成分をカットして水平化
        Vec3 look = caster.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0, look.z).normalize();

        // 右ベクトル (Y軸との外積)
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = new Vec3(0, 1, 0);

        // ■ A. 空中展開 (RANDOM_AIR) -> 【修正】前方空間分散ロジック
        if (anchor == EnumMagicAnchor.RANDOM_AIR) {
            // シード値をTickごとにずらし、同一Tick内でも呼び出し順でばらけるようにする
            long seed = System.nanoTime() ^ caster.tickCount;
            Random localRand = new Random(seed);

            // 1. 奥行き (Depth): 術者から 3.0m ~ 7.0m 離す
            // これにより「目の前」ではなく「空間」に出現する
            double depth = 6.0 + localRand.nextDouble() * 15.0;

            // 2. 横幅 (Width): 左右に ±3.0m (計6m)
            // 広い範囲に散らすことで重なりを防止
            double widthOffset = (localRand.nextDouble() - 0.8) * 10.0;

            // 3. 高さ (Height): 目の高さ ±1.5m
            // 立体的に配置
            double heightOffset = (localRand.nextDouble() - 0.8) * 10.0;

            // 座標合成: 原点(足元) + (前 * depth) + (右 * width) + (上 * (eyeHeight + offset))
            double eyeHeight = caster.getEyeHeight();

            return origin
                    .add(forward.scale(depth))
                    .add(right.scale(widthOffset))
                    .add(up.scale(eyeHeight + heightOffset));
        }

        // ■ B. ターゲット固定 (足元設置)
        else if (anchor == EnumMagicAnchor.TARGET_ANCHORED) {
            if (target != null) {
                return target.position().add(0, 0.1, 0);
            } else {
                // ターゲット不在時は、前方一定距離の地面
                return origin.add(forward.scale(5.0)).with(net.minecraft.core.Direction.Axis.Y, origin.y);
            }
        }

        // ■ C. 術者追従 (CASTER)
        else if (anchor == EnumMagicAnchor.CASTER_ANCHORED) {
            // 目の前 1.5m (ここは視線追従でOK)
            return caster.getEyePosition().add(look.scale(2.5));
        }

        // ■ D. デフォルト (FIXED等)
        else {
            return origin.add(forward.scale(3.0)).add(0, 1.5, 0);
        }
    }
}