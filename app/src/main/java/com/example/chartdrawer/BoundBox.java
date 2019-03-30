package com.example.chartdrawer;

public class BoundBox {
    private float left;
    private float right;
    private float top;
    private float bottom;

    public BoundBox()
    {
        left = Float.MAX_VALUE;
        right = -Float.MAX_VALUE;
        top = -Float.MAX_VALUE;
        bottom = Float.MAX_VALUE;
    }

    public BoundBox(float left, float right, float bottom, float top)
    {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public BoundBox(BoundBox box)
    {
        this.setFrom(box);
    }

    public void setLeft(float value)
    {
        this.left = value;
    }

    public void setRight(float value)
    {
        this.right = value;
    }

    public void setTop(float value)
    {
        this.top = value;
    }

    public void setBottom(float value)
    {
        this.bottom = value;
    }

    public float getLeft() { return left; }

    public float getRight() { return right; }

    public float getTop() { return top; }

    public float getBottom() { return bottom; }

    public float getWidth() { return Math.abs(right - left); }

    public float getHeight() { return Math.abs(top - bottom); }

    public void translate(float dx, float dy)
    {
        left += dx;
        right += dx;
        top += dy;
        bottom += dy;
    }

    public BoundBox add(BoundBox box)
    {
        return new BoundBox(left + box.left, right + box.right, bottom + box.bottom, top + box.top);
    }

    public BoundBox sub(BoundBox box)
    {
        return new BoundBox(left - box.left, right - box.right, bottom - box.bottom, top - box.top);
    }

    public BoundBox mul(float scal)
    {
        return new BoundBox(left * scal, right * scal, bottom * scal, top * scal);
    }

    public BoundBox div(float scal)
    {
        return new BoundBox(left / scal, right / scal, bottom / scal, top / scal);
    }

    public BoundBox div(float scalx, float scaly)
    {
        return new BoundBox(left / scalx, right / scalx, bottom / scaly, top / scaly);
    }

    public void setFrom(BoundBox box)
    {
        this.left = box.left;
        this.top = box.top;
        this.right = box.right;
        this.bottom = box.bottom;
    }
}
