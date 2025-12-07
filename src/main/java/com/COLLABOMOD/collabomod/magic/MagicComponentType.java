package com.COLLABOMOD.collabomod.magic;

import com.mojang.math.Vector3f;
import java.util.function.Consumer;

public enum MagicComponentType {

    // ■ 1. 何をするか (Action)
    ACT_SHOOT(
            ctx -> {
                ctx.action = SpellContext.EnumAction.PROJECTILE;
                ctx.cost += 20;
                ctx.castTime += 10;
            },
            new VisualMetadata("default", 0, new Vector3f(1.0F, 1.0F, 1.0F), 1.0F)
    ),

    ACT_EXPLODE(
            ctx -> {
                ctx.action = SpellContext.EnumAction.EXPLOSION;
                ctx.cost += 30;
                ctx.castTime += 20;
            },
            new VisualMetadata("explosion_sphere", 10, new Vector3f(1.0F, 0.5F, 0.0F), 1.0F)
    ),

    ACT_RESTORE(
            ctx -> {
                ctx.action = SpellContext.EnumAction.RESTORE;
                ctx.cost += 50;
                ctx.castTime += 40;
            },
            new VisualMetadata("default", 5, new Vector3f(0.5F, 1.0F, 0.5F), 1.0F)
    ),

    // ■ 2. 何でやるか (Attribute)
    ATTRIB_AIR(
            ctx -> {
                ctx.attribute = SpellContext.EnumAttribute.AIR;
                ctx.power += 5.0F;
                ctx.speed += 0.5F;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.9F, 0.9F, 1.0F), 1.0F)
    ),

    ATTRIB_VIBRATION( // 振動 (フォノン・メーザー用)
            ctx -> {
                ctx.attribute = SpellContext.EnumAttribute.OSCILLATION;
                ctx.power += 8.0F;
                ctx.speed += 2.0F; // 高速
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(1.0F, 0.6F, 0.0F), 1.0F)
    ),

    ATTRIB_DECOMPOSITION(
            ctx -> {
                ctx.attribute = SpellContext.EnumAttribute.DECOMPOSITION;
                ctx.power += 999.0F;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.1F, 0.1F, 0.8F), 1.0F)
    ),

    // ■ 3. どう強化するか (Modifier)
    MOD_POWER(
            ctx -> {
                ctx.power *= 1.5F;
                ctx.cost += 10;
            },
            new VisualMetadata("default", 0, new Vector3f(1.0F, 0.0F, 0.0F), 1.0F)
    ),

    MOD_RANGE(
            ctx -> {
                ctx.range *= 2.0F;
                ctx.speed *= 1.5F;
                ctx.cost += 10;
            },
            new VisualMetadata("default", 0, new Vector3f(0.0F, 1.0F, 0.0F), 1.0F)
    ),

    MOD_STRATEGIC( // 戦略級化
            ctx -> {
                ctx.attribute = SpellContext.EnumAttribute.MASS_ENERGY; // 強制的に質量変換
                ctx.power *= 50.0F;
                ctx.range = 50.0F; // 爆発半径
                ctx.cost += 100;
                ctx.castTime += 80;
            },
            // 優先度100でマテリアルバーストの見た目を適用
            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F)
    );

    // --- フィールド ---
    public final Consumer<SpellContext> applier;
    public final VisualMetadata visuals;
    public final int cost; // 基本コスト（表示用）

    MagicComponentType(Consumer<SpellContext> applier, VisualMetadata visuals) {
        this.applier = applier;
        this.visuals = visuals;
        this.cost = 0; // 表示用コストは簡易的に0、実際はapplierで加算
    }

    public void apply(SpellContext context) {
        applier.accept(context);
    }
}