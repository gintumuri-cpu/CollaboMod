package com.COLLABOMOD.collabomod.learning;

public class LearningData {
    public float[] attributeVector;
    public VisualSettings visualSettings;
    public float developerScore; // プレイヤーによる評価 (0.0 - 1.0)
    public float consistencyScore; // AIによる自己評価 (-1.0 - 1.0)

    public LearningData() {
    }

    public LearningData(float[] attributeVector, VisualSettings visualSettings) {
        this.attributeVector = attributeVector;
        this.visualSettings = visualSettings;
    }
}