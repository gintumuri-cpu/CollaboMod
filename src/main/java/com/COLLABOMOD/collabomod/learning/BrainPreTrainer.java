package com.COLLABOMOD.collabomod.learning;

import com.COLLABOMOD.collabomod.magic.EnumMagicAnimation;
import com.COLLABOMOD.collabomod.magic.EnumMagicShape;
import com.COLLABOMOD.collabomod.magic.VisualMetadata;
import com.mojang.math.Vector3f;

import java.util.Random;

public class BrainPreTrainer {

    // 脳に「一般常識」を注入する
    public static void preTrain(AttributePreference brain) {
        System.out.println("[Cardinal AI] Starting Pre-training...");

        // 火属性のトレーニング (1000回試行相当)
        trainConcept(brain, new float[]{1,0,0,0,0,0}, new Vector3f(1.0f, 0.2f, 0.0f), EnumMagicShape.SPHERE, EnumMagicAnimation.EXPAND_FADE);

        // 氷属性のトレーニング
        trainConcept(brain, new float[]{0,1,0,0,0,0}, new Vector3f(0.4f, 0.8f, 1.0f), EnumMagicShape.CYLINDER, EnumMagicAnimation.SUSTAIN_SPIN);

        // 風属性のトレーニング
        trainConcept(brain, new float[]{0,0,1,0,0,0}, new Vector3f(0.2f, 1.0f, 0.6f), EnumMagicShape.RING, EnumMagicAnimation.SUSTAIN_SPIN);

        // 土属性のトレーニング
        trainConcept(brain, new float[]{0,0,0,1,0,0}, new Vector3f(1.0f, 0.6f, 0.2f), EnumMagicShape.SPHERE, EnumMagicAnimation.PULSE);

        // 力属性（無属性）のトレーニング
        trainConcept(brain, new float[]{0,0,0,0,1,0}, new Vector3f(0.0f, 0.5f, 1.0f), EnumMagicShape.SPHERE, EnumMagicAnimation.BEAM_EXTEND);

        // 命属性のトレーニング
        trainConcept(brain, new float[]{0,0,0,0,0,1}, new Vector3f(1.0f, 0.4f, 0.8f), EnumMagicShape.RING, EnumMagicAnimation.PULSE);

        System.out.println("[Cardinal AI] Pre-training complete.");
    }

    private static void trainConcept(AttributePreference brain, float[] attrs, Vector3f color, EnumMagicShape shape, EnumMagicAnimation anim) {
        VisualSettings settings = new VisualSettings(color, shape, anim);

        // 脳に「これが正解だ」と教え込む (強烈に: 10点満点評価を50回繰り返す効果)
        for(int i=0; i<50; i++) {
            brain.train(attrs, settings, 1.0f); // 1.0 = Max Score
        }
    }
}