package com.example.chartdrawer;

public interface ITouchMovable {
    void Move(float dx, float dy);
    void TouchDown(float x, float y);
    void TouchUp(float x, float y);
}
