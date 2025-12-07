//package com.COLLABOMOD.collabomod.magic;
//
//import com.COLLABOMOD.collabomod.util.MagicSpellType;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class SpellRegistry {
//    private static final Map<MagicSpellType, IMagicSpell> spells = new HashMap<>();
//
//    // 初期化（メインクラスで呼ぶ）
//    public static void init() {
//        register(MagicSpellType.AIR_BULLET, new SpellAirBullet());
//        // 他の魔法もここに追加していく
//        register(MagicSpellType.GRAM_DEMOLITION, new SpellGramDemolition());
//    }
//
//    private static void register(MagicSpellType type, IMagicSpell spell) {
//        spells.put(type, spell);
//    }
//
//    public static IMagicSpell getSpell(MagicSpellType type) {
//        return spells.get(type);
//    }
//}