package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class SpellContext {
    public Level level;
    public LivingEntity caster;
    public LivingEntity target;
    public Vec3 origin;

    // スクリプト情報
    public List<String> script = new ArrayList<>();

    // ■ AI推論結果
    public VisualMetadata visuals = new VisualMetadata();
    public PhysicsMetadata physics = new PhysicsMetadata(); // 追加

    // 旧システム互換用 (徐々に廃止)
    public int cost = 0;
    public int castTime = 0;
    public boolean fromSequence = false;
    public ScienceContext science = new ScienceContext(); // 物理演算用コンテキスト

    public SpellContext() {}
}