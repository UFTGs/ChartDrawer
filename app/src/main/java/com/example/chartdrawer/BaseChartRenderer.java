package com.example.chartdrawer;

import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.DisplayMetrics;

import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFlush;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.GLES32.GL_LINE_STRIP_ADJACENCY;

public abstract class BaseChartRenderer implements Renderer, ITouchMovable {

    public float[] mProjectionMatrix = new float[16];
    public float[] screenMatrix = new float[16];

    private Context context;

    private boolean supportES32;

    protected Context getContext()
    {
        return  context;
    }

    protected float dens = 1;

    float curBgR = 0;
    float curBgG = 0;
    float curBgB = 0;

    public void setLineWidth(float wd)
    {
        wd *= dens;
        if(wd < 1)  wd = 1;
        glLineWidth(wd);
    }

    float t = 0;

    public BaseChartRenderer(Context context, boolean supportES32)
    {
        this.context = context;
        currentViewBound = new BoundBox();
        currentChartViewBound = new BoundBox();
        this.supportES32 = supportES32;

        DisplayMetrics dm = new DisplayMetrics();
        dm = context.getResources().getDisplayMetrics();
        dens = dm.density;
    }

    protected Chart rChart;

    protected ShaderProg20 simpleShaderProg;
    protected ShaderProg32 goodLinesShaderProg;
    protected IShaderProg currentShader;

    protected BoundBox currentViewBound;
    protected BoundBox currentChartViewBound;

    protected boolean nightMode;

    public void setChart(Chart chart) {
        this.rChart = chart;
        currentChartViewBound.setFrom(rChart.targetViewBoundBox);
        currentViewBound.setFrom(rChart.dataBoundBox);

        for(Column col : rChart.columns.values())
        {
            if(col.isVisible)
            {
                setLocked(false);
                return;
            }
        }
    }

    public void setNightMode(boolean nightMode)
    {
        this.nightMode = nightMode;
        t = 0;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        t = 1;
        CalcBackGround(0);

        simpleShaderProg = new ShaderProg20(FileUtils.readTextFromRaw(context, R.raw.v_shader), FileUtils.readTextFromRaw(context, R.raw.f_shader));

        currentShader = simpleShaderProg;

        if(supportES32) {
            goodLinesShaderProg = new ShaderProg32(FileUtils.readTextFromRaw(context, R.raw.v_shader_32), FileUtils.readTextFromRaw(context, R.raw.f_shader_32), FileUtils.readTextFromRaw(context, R.raw.g_shader_32));
            currentShader = goodLinesShaderProg;
        }

        if(rChart != null)
            currentChartViewBound.setFrom(rChart.targetViewBoundBox);
    }

    protected int viewportWidth = 100;
    protected int viewportHeight = 100;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        glViewport(0, 0, width, height);
    }

    float animLin(float t, float from, float to)
    {
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        return (to - from) * t + from;
    }

    float anim(float t)
    {
        if(t < 0)
            return  0;
        else if(t < 1)
            return 10*t*t*t - 15*t*t*t*t + 6*t*t*t*t*t;
        return  1;
    }

    boolean locked = true;
    public void setLocked(boolean val)
    {
        locked = val;
    }

    protected void processScale(float dt)
    {
        if (rChart != null && !locked) {
            currentChartViewBound = currentChartViewBound.add(rChart.targetViewBoundBox.sub(currentChartViewBound).div(2f, 8f));
        }
    }

    private long lastTime = 0;
    private long curTime = 0;
    private float dt = 0;

    protected void PreRender()
    {

    }

    protected void PostRender()
    {

    }

    private void CalcBackGround(float dt)
    {
        t += dt;
        if(t <= 1) {
            if (nightMode) {
                curBgR = animLin(t/0.23f, 1.0f, 0.114f);
                curBgG = animLin(t/0.23f, 1.0f, 0.153f);
                curBgB = animLin(t/0.23f, 1.0f, 0.2f);
            } else {
                curBgR = animLin(t/0.23f, 0.114f, 1.0f);
                curBgG = animLin(t/0.23f, 0.153f, 1.0f);
                curBgB = animLin(t/0.23f, 0.2f, 1.0f);
            }
        }
    }

    protected float charLineWidth = 1.4f;

    @Override
    public void onDrawFrame(GL10 gl) {
        curTime = System.nanoTime();
        if(lastTime == 0)
            dt = 0;
        else
            dt = (curTime - lastTime) / 1e9f;
        lastTime = curTime;

        CalcBackGround(dt);

        glClearColor(curBgR, curBgG, curBgB, 1.0f);

        processScale(dt);

        glClear(GL_COLOR_BUFFER_BIT);

        Matrix.orthoM(mProjectionMatrix, 0, currentViewBound.getLeft(), currentViewBound.getRight(), currentViewBound.getBottom(), currentViewBound.getTop() == currentViewBound.getBottom() ? currentViewBound.getTop() + 1:currentViewBound.getTop(), -1, 1);

        PreRender();

        Matrix.orthoM(screenMatrix, 0, 0, viewportWidth, 0, viewportHeight, -1, 1);

        currentShader.useProgram();
        currentShader.ApplyProjection(mProjectionMatrix, screenMatrix);
        currentShader.enableAttribs();

        setLineWidth(charLineWidth);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if(rChart != null) {
            for (Map.Entry<String, Column> column : rChart.columns.entrySet()) {
                Column col = column.getValue();
                if(col.isVisible)
                {
                    col.hideT += dt;
                    if(col.hideT > 1) col.hideT = 1;
                } else {
                    col.hideT -= dt;
                    if(col.hideT < 0) col.hideT = 0;
                }

                currentShader.SetColor(col.cR, col.cG, col.cB, anim(col.hideT));

                col.vertices.position(0);
                glVertexAttribPointer(currentShader.getAttr("vertPos"), 2, GL_FLOAT, false, 0, col.vertices);
                if(supportES32)
                    glDrawArrays(GL_LINE_STRIP_ADJACENCY, 0, col.vertices.capacity() / 2);
                else
                    glDrawArrays(GL_LINE_STRIP, 0, col.vertices.capacity() / 2);
            }
        }

        glDisable(GL_BLEND);
        currentShader.disableAttribs();
        glUseProgram(0);

        PostRender();

        glFlush();
    }

    public void doZoomInCurrentBound()
    {
        float max = 0;
        for(Map.Entry<String, Column> p : rChart.columns.entrySet())
        {
            Column col = p.getValue();
            if(!col.isVisible)  continue;
            for (int i = 0; i < col.vertices.capacity(); i += 2)
            {
                if(col.vertices.get(i) >= rChart.targetViewBoundBox.getLeft() && col.vertices.get(i) <= rChart.targetViewBoundBox.getRight() && col.vertices.get(i + 1) > max)
                    max = col.vertices.get(i + 1);
                else if(col.vertices.get(i) >= rChart.targetViewBoundBox.getRight())
                    break;
            }
        }
        rChart.targetViewBoundBox.setTop(max * 1.10f);
    }

    @Override
    public void TouchUp(float x, float y)
    {
        doZoomInCurrentBound();
    }
}
