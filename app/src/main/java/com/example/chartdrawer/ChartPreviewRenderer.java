package com.example.chartdrawer;

import android.content.Context;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glVertexAttribPointer;

public class ChartPreviewRenderer extends BaseChartRenderer {

    FloatBuffer frameVerts;

    public ChartPreviewRenderer(Context context) {
        super(context, false);
        borderWidth = 6.67f * dens;
        frameVerts = ByteBuffer.allocateDirect(24 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    final float borderWidth;

    @Override
    protected void processScale(float dt) {
        super.processScale(dt);

        if(rChart == null)
            return;

        currentViewBound.setTop(currentViewBound.getTop() + (rChart.dataBoundBox.getTop() - currentViewBound.getTop()) / 8);
        currentViewBound.setBottom(currentViewBound.getBottom() + (rChart.dataBoundBox.getBottom() - currentViewBound.getBottom()) / 8);
        currentViewBound.setLeft(rChart.dataBoundBox.getLeft());
        currentViewBound.setRight(rChart.dataBoundBox.getRight());

        frameVerts.clear();
        frameVerts.position(0);

        frameVerts.put(currentChartViewBound.getLeft());
        frameVerts.put(currentViewBound.getBottom());
        frameVerts.put(currentChartViewBound.getLeft());
        frameVerts.put(currentViewBound.getTop());
        frameVerts.put(currentViewBound.getLeft());
        frameVerts.put(currentViewBound.getBottom());
        frameVerts.put(currentViewBound.getLeft());
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getRight());
        frameVerts.put(currentViewBound.getBottom());
        frameVerts.put(currentChartViewBound.getRight());
        frameVerts.put(currentViewBound.getTop());
        frameVerts.put(currentViewBound.getRight());
        frameVerts.put(currentViewBound.getBottom());
        frameVerts.put(currentViewBound.getRight());
        frameVerts.put(currentViewBound.getTop());

        float bordSW = borderWidth / viewportWidth * currentViewBound.getWidth();
        float bordTW = borderWidth / 4f / viewportHeight * currentViewBound.getHeight();

        frameVerts.put(currentChartViewBound.getLeft());
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getLeft());
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getLeft() + bordSW);
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getLeft() + bordSW);
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getLeft() + bordSW);
        frameVerts.put(currentViewBound.getBottom() + bordTW);

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getBottom() + bordTW);

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getRight());
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getRight());
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getRight());
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getBottom());

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getRight() - bordSW);
        frameVerts.put(currentViewBound.getTop() - bordTW);

        frameVerts.put(currentChartViewBound.getLeft() + bordSW);
        frameVerts.put(currentViewBound.getTop());

        frameVerts.put(currentChartViewBound.getLeft() + bordSW);
        frameVerts.put(currentViewBound.getTop() - bordTW);
    }

    @Override
    protected void PreRender() {
        Matrix.orthoM(mProjectionMatrix, 0, currentViewBound.getLeft(), currentViewBound.getRight(), currentViewBound.getBottom(), currentViewBound.getTop() == currentViewBound.getBottom() ? currentViewBound.getTop() + 1:currentViewBound.getTop(), -1, 1);
        simpleShaderProg.useProgram();
        simpleShaderProg.ApplyProjection(mProjectionMatrix, null);
        //glLineWidth(16);
        if(nightMode)
            simpleShaderProg.SetColor(.168f, 0.259f, 0.337f, 1);
        else
            simpleShaderProg.SetColor(.859f, 0.906f, 0.941f, 1);
        frameVerts.position(0);
        glVertexAttribPointer(simpleShaderProg.getAttr("vertPos"), 2, GL_FLOAT, false, 0, frameVerts);
        simpleShaderProg.enableAttribs();
        glDrawArrays(GL_TRIANGLE_STRIP, 8, 16);
        simpleShaderProg.disableAttribs();
    }

    @Override
    protected void PostRender() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        simpleShaderProg.useProgram();
        if(nightMode)
            simpleShaderProg.SetColor(0.075f, 0.110f, 0.149f, 0.65f);
        else
            simpleShaderProg.SetColor(.949f, 0.9765f, 0.992f, 0.72f);
        frameVerts.position(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, frameVerts);
        simpleShaderProg.enableAttribs();
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        glDrawArrays(GL_TRIANGLE_STRIP, 4, 4);
        glDisable(GL_BLEND);
        simpleShaderProg.disableAttribs();
    }

    boolean doMoving = false;
    boolean doScaleLeft = false;
    boolean doScaleRight = false;

    float bw = 0;

    @Override
    public void Move(float dx, float dy) {
        if(rChart == null || locked)
            return;

        dx /= viewportWidth;
        dx *= currentViewBound.getWidth();

        if(doMoving) {
            rChart.targetViewBoundBox.translate(dx, 0f);

            float overR = rChart.dataBoundBox.getRight() - rChart.targetViewBoundBox.getRight();
            float overL = rChart.dataBoundBox.getLeft() - rChart.targetViewBoundBox.getLeft();

            if (overR < 0) {
                rChart.targetViewBoundBox.translate(overR, 0f);
            }
            if (overL > 0) {
                rChart.targetViewBoundBox.translate(overL, 0f);
            }
            doZoomInCurrentBound();
        } else if(doScaleLeft) {
            rChart.targetViewBoundBox.setLeft(rChart.targetViewBoundBox.getLeft() + dx);

            float overL = rChart.dataBoundBox.getLeft() - rChart.targetViewBoundBox.getLeft();

            if (overL > 0) {
                rChart.targetViewBoundBox.setLeft(rChart.targetViewBoundBox.getLeft() + overL);
            }

            bw = borderWidth / viewportWidth * currentViewBound.getWidth();

            if(rChart.targetViewBoundBox.getRight() - rChart.targetViewBoundBox.getLeft() - bw * 3 < 0) {
                rChart.targetViewBoundBox.setLeft(rChart.targetViewBoundBox.getRight() - bw * 3);
            }
            doZoomInCurrentBound();
        } else if(doScaleRight) {
            rChart.targetViewBoundBox.setRight(rChart.targetViewBoundBox.getRight() + dx);

            float overR = rChart.dataBoundBox.getRight() - rChart.targetViewBoundBox.getRight();

            if (overR < 0) {
                rChart.targetViewBoundBox.setRight(rChart.targetViewBoundBox.getRight() + overR);
            }

            bw = borderWidth / viewportWidth * currentViewBound.getWidth();

            if(rChart.targetViewBoundBox.getRight() - rChart.targetViewBoundBox.getLeft() - bw * 3 < 0) {
                rChart.targetViewBoundBox.setRight(rChart.targetViewBoundBox.getLeft() + bw * 3);
            }
            doZoomInCurrentBound();
        }
    }

    @Override
    public void TouchDown(float x, float y) {
        if(locked) return;
        x = (x / viewportWidth) * currentViewBound.getWidth() + currentViewBound.getLeft();
        float bordSW = borderWidth / viewportWidth * rChart.dataBoundBox.getWidth();
        if(x >= currentChartViewBound.getLeft() + bordSW && x <= currentChartViewBound.getRight() - bordSW)
        {
            doMoving = true;
        } else if(x < currentChartViewBound.getLeft() + bordSW && x >= currentChartViewBound.getLeft() - bordSW) {
            doScaleLeft = true;
        } else if(x > currentChartViewBound.getRight() - bordSW && x <= currentChartViewBound.getRight() + bordSW) {
            doScaleRight = true;
        }
    }

    @Override
    public void TouchUp(float x, float y) {
        if(locked) return;
        doMoving = false;
        doScaleRight = false;
        doScaleLeft = false;
        super.TouchUp(x, y);
    }
}
