package com.example.chartdrawer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Column {
    public Column(String key, int ptsCount)
    {
        dataBoundBox = new BoundBox();
        this.isVisible = true;
        this.targetVisiblity = true;
        this.vertices = ByteBuffer.allocateDirect(ptsCount * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.vertices.position(0);
        this.key = key;
        this.hideT = 1;
    }

    public String key;
    public String name;
    public String type;
    public float cR;
    public float cG;
    public float cB;
    public float cA;
    public FloatBuffer vertices;
    public boolean isVisible;
    public boolean targetVisiblity;
    public float hideT;
    BoundBox dataBoundBox;
}
