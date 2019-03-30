package com.example.chartdrawer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class VerticalScrollview extends ScrollView {
    public VerticalScrollview(Context context) {
        super(context);
    }
    public VerticalScrollview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public VerticalScrollview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private float previousX = 0;
    private float previousY = 0;
    private boolean blockScrolling = false;
    private boolean doScrolling = false;

    @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        final int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                doScrolling = false;
                blockScrolling = false;
                super.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;

                if(dx > 25 && !doScrolling)
                    blockScrolling = true;
                if(dy > 25 && !blockScrolling)
                    doScrolling = true;

                if(!blockScrolling)
                    super.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_UP:
                if(!blockScrolling)
                    super.onTouchEvent(ev);
                doScrolling = false;
                blockScrolling = false;
                return false;
            default:
                break;
        }
        previousX = x;
        previousY = y;
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        Log.i("VerticalScrollview", "onTouchEvent. action: " + ev.getAction() );
        return true;
    }
}
