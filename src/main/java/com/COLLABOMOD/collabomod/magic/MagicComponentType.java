//package com.COLLABOMOD.collabomod.magic;
//
//import com.COLLABOMOD.collabomod.science.PhenomenonType;
//import com.COLLABOMOD.collabomod.science.ScienceContext;
//import com.mojang.math.Vector3f;
//import java.util.function.Consumer;
//
//public enum MagicComponentType {
//
//    // --- Action ---
//    ACT_SHOOT(
//            ctx -> {
//                ctx.action = SpellContext.EnumAction.PROJECTILE;
//                ctx.science.type = PhenomenonType.POINT;
//                ctx.science.velocity += 2.0F;
//                ctx.science.energy += 10.0F;
//                ctx.cost += 10;
//                ctx.castTime += 20; // 魔法陣が見えるように時間を確保
//            },
//            new VisualMetadata("default", 0, new Vector3f(1.0F, 1.0F, 1.0F), 1.0F),
//            10
//    ),
//
//    ACT_EXPLODE(
//            ctx -> {
//                ctx.action = SpellContext.EnumAction.EXPLOSION;
//                ctx.science.type = PhenomenonType.SPHERE_EXPANSION;
//                ctx.science.radius = 5.0F;
//                ctx.science.energy += 50.0F;
//                ctx.cost += 20;
//                ctx.castTime += 30;
//                ctx.science.visuals.isSpiky = true; // 爆発系はトゲトゲに
//            },
//            new VisualMetadata("explosion_sphere", 10, new Vector3f(1.0F, 0.5F, 0.0F), 1.0F),
//            20
//    ),
//
//    ACT_RESTORE(
//            ctx -> {
//                ctx.action = SpellContext.EnumAction.RESTORE;
//                ctx.cost += 50;
//                ctx.castTime += 40;
//            },
//            new VisualMetadata("default", 5, new Vector3f(0.5F, 1.0F, 0.5F), 1.0F),
//            50
//    ),
//
//    ACT_MOVE(
//            ctx -> {
//                ctx.action = SpellContext.EnumAction.MOVE;
//                ctx.science.type = PhenomenonType.BEAM;
//                ctx.cost += 15;
//                ctx.castTime += 10;
//            },
//            new VisualMetadata("magic_circle", 5, new Vector3f(0.5F, 1.0F, 1.0F), 1.0F), 15
//    ),
//    ACT_DEFEND(
//            ctx -> {
//                ctx.action = SpellContext.EnumAction.DEFEND;
//                ctx.science.type = PhenomenonType.SPHERE_EXPANSION;
//                ctx.science.radius = 3.0F;
//                ctx.science.compShield = 1.0F;
//                ctx.cost += 30;
//                ctx.castTime += 10;
//                ctx.science.visuals.shape = EnumMagicShape.SPHERE; // 防御は球体
//            },
//            new VisualMetadata("magic_circle", 5, new Vector3f(1.0F, 0.8F, 0.2F), 1.5F), 30
//    ),
//
//    // --- Attribute ---
//    ATTRIB_AIR(
//            ctx -> {
//                ctx.science.compWave += 0.2F;
//                ctx.science.energy += 10.0F;
//                ctx.cost += 5;
//            },
//            new VisualMetadata("magic_circle", 5, new Vector3f(0.9F, 0.9F, 1.0F), 1.0F),
//            5
//    ),
//
//    ATTRIB_VIBRATION(
//            ctx -> {
//                ctx.science.compWave = 1.0F;
//                ctx.science.energy += 30.0F;
//                // ■ 追加: 振動属性なら魔法陣を波打たせる
//                ctx.science.visuals.isWavy = true;
//            },
//            new VisualMetadata("magic_circle", 5, new Vector3f(0.2F, 0.9F, 1.0F), 1.0F),
//            5
//    ),
//
//    ATTRIB_DECOMPOSITION(
//            ctx -> {
//                ctx.science.compMatter = 1.0F;
//                ctx.science.energy += 500.0F;
//            },
//            new VisualMetadata("magic_circle", 5, new Vector3f(0.1F, 0.1F, 0.8F), 1.0F),
//            5
//    ),
//
//    ATTRIB_FIRE(
//            ctx -> {
//                ctx.science.temperature += 1500.0F;
//                ctx.science.energy += 20.0F;
//                ctx.cost += 10;
//            },
//            new VisualMetadata("default", 5, new Vector3f(1.0F, 0.4F, 0.0F), 1.0F), 10
//    ),
//
//    ATTRIB_ICE(
//            ctx -> {
//                ctx.science.temperature = 100.0F;
//                ctx.science.energy += 10.0F;
//                ctx.cost += 10;
//            },
//            new VisualMetadata("default", 5, new Vector3f(0.5F, 0.8F, 1.0F), 1.0F), 10
//    ),
//
//    ATTRIB_ACCEL(
//            ctx -> {
//                ctx.science.velocity += 5.0F;
//            },
//            new VisualMetadata("default", 5, new Vector3f(0.5F, 1.0F, 0.5F), 1.0F),
//            10
//    ),
//
//    ATTRIB_WEIGHT(
//            ctx -> {
//                ctx.science.mass += 100.0F;
//            },
//            new VisualMetadata("default", 5, new Vector3f(0.5F, 0.0F, 0.5F), 1.0F),
//            10
//    ),
//
//
//    // --- Modifier ---
//    MOD_POWER(
//            ctx -> {
//                ctx.science.energy *= 1.5F;
//                ctx.cost += 10;
//                // ■ 追加: パワー強化で魔法陣の層を増やす
//                ctx.science.visuals.layerCount++;
//            },
//            new VisualMetadata("default", 0, new Vector3f(1.0F, 0.0F, 0.0F), 1.0F),
//            10
//    ),
//
//    MOD_RANGE(
//            ctx -> {
//                ctx.science.radius *= 2.0F;
//                ctx.science.velocity *= 1.5F;
//                ctx.cost += 10;
//                // 範囲拡大で魔法陣も大きく
//                ctx.science.visuals.scale *= 1.5F;
//            },
//            new VisualMetadata("default", 0, new Vector3f(0.0F, 1.0F, 0.0F), 1.0F),
//            10
//    ),
//
//    MOD_STRATEGIC(
//            ctx -> {
//                ctx.attribute = SpellContext.EnumAttribute.MASS_ENERGY;
//                ctx.science.energy *= 100.0F;
//                ctx.science.radius = 50.0F;
//                ctx.science.velocity = 0.2F;
//                ctx.science.temperature = 5000.0F;
//                ctx.science.compMatter = 1.0F;
//                ctx.cost += 100;
//                ctx.castTime += 80;
//                ctx.science.visuals.layerCount = 5; // 戦略級は多重展開
//                ctx.science.visuals.hasLightning = true;
//            },
//            new VisualMetadata("material_burst", 100, new Vector3f(0.2F, 0.9F, 1.0F), 2.0F),
//            100
//    ),
//
//    // 互換用ダミー
//    PROJECTILE_AIR(ctx->{}, new VisualMetadata("default", 0, new Vector3f(1,1,1), 1), 0),
//    PROJECTILE_GRAM(ctx->{}, new VisualMetadata("default", 0, new Vector3f(1,1,1), 1), 0),
//    MATERIAL_BURST(ctx->{}, new VisualMetadata("default", 0, new Vector3f(1,1,1), 1), 0);
//
//    public final Consumer<SpellContext> applier;
//    public final VisualMetadata visuals;
//    public final int cost;
//
//    MagicComponentType(Consumer<SpellContext> applier, VisualMetadata visuals, int cost) {
//        this.applier = applier;
//        this.visuals = visuals;
//        this.cost = cost;
//    }
//
//    public void apply(SpellContext context) {
//        applier.accept(context);
//    }
//
//    public enum Category { ACTION, ATTRIBUTE, MODIFIER, OTHER }
//    public Category getCategory() { return Category.OTHER; } // 簡易実装
//    public String getCodeName() { return this.name(); }
//}