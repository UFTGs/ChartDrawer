package com.example.chartdrawer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.support.v4.view.NestedScrollingChild;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class GLChartView extends GLSurfaceView implements NestedScrollingChild {
    private float previousX;
    private float previousY;
    private boolean blockScrolling = false;
    private boolean doScrolling = false;

    public GLChartView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
    }

    public GLChartView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setEGLContextClientVersion(2);
    }

    Renderer renderer;

    public boolean rendererIsSetted()
    {
        return renderer != null;
    }

    @Override
    public void setRenderer(Renderer rdr)
    {
        this.renderer = rdr;
        super.setRenderer(rdr);
    }

    boolean paused = false;

    public boolean isPaused()
    {
        return paused;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        paused = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        paused = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - previousX;
                float dy = y - previousY;

                if(dy > 25 && !doScrolling)
                    blockScrolling = true;
                if(dx > 25 && !blockScrolling)
                    doScrolling = true;

                if(!blockScrolling)
                    ((ITouchMovable)renderer).Move(dx, dy);

                        break;
            case MotionEvent.ACTION_DOWN:
                ((ITouchMovable)renderer).TouchDown(x, y);
                doScrolling = false;
                blockScrolling = false;
                break;
            case MotionEvent.ACTION_UP:
                ((ITouchMovable)renderer).TouchUp(x, y);
                doScrolling = false;
                blockScrolling = false;
                break;
        }

        previousX = x;
        previousY = y;
        return true;
    }
}
