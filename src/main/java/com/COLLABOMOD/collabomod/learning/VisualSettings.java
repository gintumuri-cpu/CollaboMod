package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnimation;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.mojang.math.Vector3f;

public class VisualSettings {
    public Vector3f color;
    public EnumMagicShape shape;
    public EnumMagicAnimation animation;

    public VisualSettings(Vector3f color, EnumMagicShape shape, EnumMagicAnimation animation) {
        this.color = color;
        this.shape = shape;
        this.animation = animation;
    }
}