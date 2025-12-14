package com.COLLABOMOD.collabomod.magic;

import com.COLLABOMOD.collabomod.science.ScienceContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SpellContext {
    public final ScienceContext science = new ScienceContext();

    public List<String> script = new ArrayList<>();
    public boolean fromSequence = false;
    public boolean isSimulation = false;
    public int cost = 0;
    public int castTime = 0;
    public EnumAction action = EnumAction.NONE;
    public EnumAttribute attribute = EnumAttribute.NONE;

    public Level level;
    public LivingEntity caster;
    public Vec3 origin = Vec3.ZERO;
    public Vec3 direction = Vec3.ZERO;
    public float rotX = 0;
    public float rotY = 0;
    public LivingEntity target;

    public float power = 1.0F;
    public float range = 10.0F;
    public float speed = 1.0F;

    public SpellContext(Level level, LivingEntity caster) {
        this.level = level;
        this.caster = caster;
        if (caster != null) {
            this.origin = caster.position();
            this.direction = caster.getLookAngle();
            this.rotX = caster.getXRot();
            this.rotY = caster.getYRot();
        }
    }

    public SpellContext() {
        this.isSimulation = true; // デフォルトでtrueにしておく
    }

    public void setLocation(Vec3 pos, float pitch, float yaw) {
        this.origin = pos;
        this.rotX = pitch;
        this.rotY = yaw;
        float f = 0.017453292F;
        double x = -Math.sin(yaw * f) * Math.cos(pitch * f);
        double y = -Math.sin(pitch * f);
        double z = Math.cos(yaw * f) * Math.cos(pitch * f);
        this.direction = new Vec3(x, y, z).normalize();
    }

    public enum EnumAction {
        NONE, PROJECTILE, EXPLOSION, RESTORE,
        MOVE,   // 移動・加速
        DEFEND  // 防御・干渉
    }

    public enum EnumAttribute {
        NONE, AIR, OSCILLATION, DECOMPOSITION, MASS_ENERGY,
        ACCEL,  // 加速
        WEIGHT  // 加重
    }
}
