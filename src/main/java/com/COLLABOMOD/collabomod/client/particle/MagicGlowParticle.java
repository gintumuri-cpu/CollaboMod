package com.COLLABOMOD.collabomod.client.particle;

import com.COLLABOMOD.collabomod.client.ClientCardinalSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class MagicGlowParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final boolean isTrail; // 残像かどうか

    protected MagicGlowParticle(ClientLevel level, double x, double y, double z, double r, double g, double b, SpriteSet sprites, boolean isTrail) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.isTrail = isTrail;

        // 色の設定 (RGB)
        this.rCol = (float) r;
        this.gCol = (float) g;
        this.bCol = (float) b;

        // 寿命とサイズ
        this.lifetime = isTrail ? 5 + random.nextInt(5) : 40 + random.nextInt(20);
        this.quadSize = isTrail ? 0.05F : 0.1F + random.nextFloat() * 0.1F; // サイズランダム
        this.alpha = 1.0F;

        // 初期速度 (ふわっと拡散)
        this.xd = (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.yd = (Math.random() * 2.0D - 1.0D) * 0.02D;
        this.zd = (Math.random() * 2.0D - 1.0D) * 0.02D;

        // テクスチャ設定
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // --- 1. モーション・ベクター (Flow Field) ---
        if (!isTrail) { // 残像は物理演算を受けない（軽量化）
            BlockPos pos = new BlockPos(this.x, this.y, this.z);

            // 温度勾配を取得
            Vec3 flow = ClientCardinalSystem.getThermalGradient(pos);

            // ベクトルがある場合、そちらに加速 (熱源に向かう/熱源から離れる動き)
            if (flow.lengthSqr() > 0.001) {
                // 少しノイズを混ぜて揺らぎを表現
                double noise = (Math.random() - 0.5) * 0.02;
                this.xd += flow.x * 0.05 + noise;
                this.yd += flow.y * 0.05 + 0.005; // 上昇成分を追加
                this.zd += flow.z * 0.05 + noise;
            } else {
                // 流れがない場所でも少し揺らがせる (ブラウン運動)
                this.xd += (Math.random() - 0.5) * 0.002;
                this.yd += (Math.random() - 0.5) * 0.002;
                this.zd += (Math.random() - 0.5) * 0.002;
            }
        }

        this.move(this.xd, this.yd, this.zd);

        // 減速処理
        this.xd *= 0.95D;
        this.yd *= 0.95D;
        this.zd *= 0.95D;

        // --- 2. ゴースト・トレイル (残像) ---
        // 本体かつ速度が出ている場合、確率で残像を残す
        if (!isTrail && this.age % 2 == 0) {
            double speed = Math.sqrt(this.xd*this.xd + this.yd*this.yd + this.zd*this.zd);
            if (speed > 0.05) {
                // 残像を生成 (自分の色を引き継ぐ)
                MagicGlowParticle trail = new MagicGlowParticle(this.level, this.x, this.y, this.z, this.rCol, this.gCol, this.bCol, this.sprites, true);
                trail.quadSize = this.quadSize * 0.7F; // 少し小さく
                Minecraft.getInstance().particleEngine.add(trail);
            }
        }

        // フェードアウト
        if (this.age > this.lifetime * 0.7) {
            this.alpha = 1.0F - ((float)(this.age - this.lifetime * 0.7) / (float)(this.lifetime * 0.3));
        }

        // アニメーション更新
        this.setSpriteFromAge(sprites);
    }

    // --- 3. 加算合成と発光 (Bloom) ---
    @Override
    public ParticleRenderType getRenderType() {
        // 半透明かつ加算合成的な描画 (PARTICLE_SHEET_TRANSLUCENT は深度書き込みなしでブレンドされる)
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        // 常に最大の明るさ (Bloom効果の基礎)
        return 240 | (240 << 16);
    }

    // --- Factory ---
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double r, double g, double b) {
            // 引数の r, g, b を色として使用
            return new MagicGlowParticle(level, x, y, z, r, g, b, sprites, false);
        }
    }
}