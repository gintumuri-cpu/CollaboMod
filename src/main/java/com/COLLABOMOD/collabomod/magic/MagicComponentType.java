package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.science.PhenomenonType;
import com.COLLABOMOD.collabomod.science.ScienceContext;
import com.mojang.math.Vector3f;
import java.util.function.Consumer;

public enum MagicComponentType {

    // ■■■ 1. 作用（Action） ■■■
    ACT_SHOOT(
            ctx -> {
                ctx.action = SpellContext.EnumAction.PROJECTILE;
                ctx.science.type = PhenomenonType.POINT;
                ctx.science.velocity += 2.0F;
                ctx.science.energy += 10.0F;
                ctx.cost += 10;
                ctx.castTime += 5;
            },
            new VisualMetadata("default", 0, new Vector3f(1.0F, 1.0F, 1.0F), 1.0F),
            10 // 基本コスト
    ),

    ACT_EXPLODE(
            ctx -> {
                ctx.action = SpellContext.EnumAction.EXPLOSION;
                ctx.science.type = PhenomenonType.SPHERE_EXPANSION;
                ctx.science.radius = 5.0F;
                ctx.science.energy += 50.0F;
                ctx.cost += 20;
                ctx.castTime += 10;
            },
            new VisualMetadata("explosion_sphere", 10, new Vector3f(1.0F, 0.5F, 0.0F), 1.0F),
            20
    ),

    ACT_RESTORE(
            ctx -> {
                ctx.action = SpellContext.EnumAction.RESTORE;
                ctx.cost += 50;
                ctx.castTime += 20;
            },
            new VisualMetadata("default", 5, new Vector3f(0.5F, 1.0F, 0.5F), 1.0F),
            50
    ),


    // ■■■ 2. 属性（Attribute） ■■■
    ATTRIB_AIR(
            ctx -> {
                ctx.science.compWave += 0.2F;
                ctx.science.energy += 10.0F;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.9F, 0.9F, 1.0F), 1.0F),
            5
    ),

    ATTRIB_VIBRATION(
            ctx -> {
                ctx.science.compWave = 1.0F;
                ctx.science.energy += 30.0F;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.2F, 0.9F, 1.0F), 1.0F),
            5
    ),

    ATTRIB_DECOMPOSITION(
            ctx -> {
                ctx.science.compMatter = 1.0F;
                ctx.science.energy += 500.0F;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.1F, 0.1F, 0.8F), 1.0F),
            5
    ),


    // ■■■ 3. 強化（Modifier） ■■■
    MOD_POWER_UP(
            ctx -> {
                ctx.science.energy *= 1.5F; // 威力1.5倍
                ctx.cost += 10;             // コスト+10
            },
            new VisualMetadata("default", 0, new Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
            10
    ),

    MOD_STRATEGIC(
            ctx -> {
                ctx.attribute = SpellContext.EnumAttribute.MASS_ENERGY;
                ctx.science.energy *= 100.0F;
                ctx.science.radius = 50.0F;
                ctx.science.velocity = 0.2F;
                ctx.science.temperature = 5000.0F;
                ctx.science.compMatter = 1.0F;
                ctx.cost += 100;
                ctx.castTime += 80;
            },
            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F),
            100
    ),

    // 旧互換用
    PROJECTILE_AIR(
            ctx -> {
                ctx.action = SpellContext.EnumAction.PROJECTILE;
                ctx.science.type = PhenomenonType.POINT;
                ctx.science.compWave = 0.2F;
                ctx.cost += 20;
                ctx.castTime += 15;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.9F, 0.9F, 1.0F), 1.0F),
            20
    ),

    PROJECTILE_GRAM(
            ctx -> {
                ctx.action = SpellContext.EnumAction.PROJECTILE;
                ctx.science.compMatter = 1.0F;
                ctx.cost += 40;
                ctx.castTime += 0;
            },
            new VisualMetadata("magic_circle", 5, new Vector3f(0.2F, 0.9F, 1.0F), 1.0F),
            40
    ),

    MATERIAL_BURST(
            ctx -> {
                ctx.action = SpellContext.EnumAction.EXPLOSION;
                ctx.science.type = PhenomenonType.SPHERE_EXPANSION;
                ctx.science.energy = 10000.0F;
                ctx.science.radius = 50.0F;
                ctx.science.temperature = 5000.0F;
                ctx.science.compMatter = 1.0F;
                ctx.cost += 50;
                ctx.castTime += 100;
            },
            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F),
            50
    );


    // --- フィールド ---
    public final Consumer<SpellContext> applier;
    public final VisualMetadata visuals;
    public final int cost; // ■ 追加: これがないとエラーになります

    // コンストラクタ
    MagicComponentType(Consumer<SpellContext> applier, VisualMetadata visuals, int cost) {
        this.applier = applier;
        this.visuals = visuals;
        this.cost = cost;
    }

    public void apply(SpellContext context) {
        applier.accept(context);
    }


    // ■■■ 追加: GUI表示用のカテゴリ定義 ■■■
    public enum Category {
        ACTION("作用", 0xFFFF5555),      // 赤
        ATTRIBUTE("属性", 0xFF55FFFF),   // 水色
        MODIFIER("強化", 0xFFFFFF55),    // 黄色
        OTHER("その他", 0xFFAAAAAA);     // グレー

        public final String label;
        public final int color;
        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    // 自分のカテゴリを判定するメソッド
    public Category getCategory() {
        String n = this.name();
        if (n.startsWith("ACT_")) return Category.ACTION;
        if (n.startsWith("ATTRIB_")) return Category.ATTRIBUTE;
        if (n.startsWith("MOD_")) return Category.MODIFIER;
        return Category.OTHER;
    }

    // コード風の表示名を取得 (例: ACT_SHOOT -> Action.Shoot())
    public String getCodeName() {
        String n = this.name();
        if (n.startsWith("ACT_")) return "Action." + toPascalCase(n.substring(4)) + "()";
        if (n.startsWith("ATTRIB_")) return "Attrib." + toPascalCase(n.substring(7)) + "";
        if (n.startsWith("MOD_")) return "Mod." + toPascalCase(n.substring(4)) + "()";
        return n;
    }

    private String toPascalCase(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}