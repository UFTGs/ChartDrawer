package com.example.chartdrawer;

public interface IShaderProg {
    void ApplyProjection(float[] projMat, float[] scrMat);
    void SetColor(float r, float g, float b, float a);
    void useProgram();
    void enableAttribs();
    void disableAttribs();
    int getAttr(String attr);
}
