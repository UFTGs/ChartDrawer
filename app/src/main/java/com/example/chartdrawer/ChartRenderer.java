package com.example.chartdrawer;

import android.content.Context;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import static android.opengl.GLES20.*;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ChartRenderer extends BaseChartRenderer {

    public ChartRenderer(Context context, boolean supportES32) {
        super(context, supportES32);
        charLineWidth = 2.8f;
        gridVerts = ByteBuffer.allocateDirect(22*2*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        markerVerts = ByteBuffer.allocateDirect(31 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        markerYValues = new ArrayList<>();
        dateList = new ArrayList<>();
    }

    ArrayList<Long> dateList;

    @Override
    public void setChart(Chart chart) {
        super.setChart(chart);
        long curTS = RoundTimestamp((long)rChart.dataBoundBox.getRight());
        dateList.clear();
    }

    private long RoundTimestamp(long ts)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        Date rounded = new Date(ts);
        try {
            rounded = dateFormat.parse(dateFormat.format(new Date(ts)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return  rounded.getTime();
    }

    TextWriter textWriter;

    FloatBuffer gridVerts;
    FloatBuffer markerVerts;

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        super.onSurfaceChanged(gl, width, height);
        markerVerts.position(0);
        markerVerts.put(0);
        markerVerts.put(0);
        markerVerts.put(0);
        markerVerts.put(viewportHeight);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        textWriter = new TextWriter(getContext());

        markerVerts.position(0);
        markerVerts.put(0);
        markerVerts.put(0);
        markerVerts.put(0);
        markerVerts.put(viewportHeight);

        markerVerts.put(0.5f);
        markerVerts.put(0.5f);

        markerVerts.put(0);
        markerVerts.put(0);
        markerVerts.put(1f);
        markerVerts.put(0);
        markerVerts.put(1f);
        markerVerts.put(1f);
        markerVerts.put(0);
        markerVerts.put(1f);
        markerVerts.put(0);
        markerVerts.put(0);

        markerVerts.put(0);
        markerVerts.put(0);
        float ang = 0;
        for (int i = 0; i < (16 + 1); ++i) {
            markerVerts.put((float) Math.cos(ang));
            markerVerts.put((float) Math.sin(ang));
            ang += Math.PI / 8;
        }
    }

    float gridStep = 0;

    long dateGridStep = 0;

    @Override
    protected void processScale(float dt) {
        super.processScale(dt);

        currentViewBound = currentChartViewBound;
        gridVerts.position(0);

        gridStep = Math.round((currentChartViewBound.getTop() - currentChartViewBound.getBottom()) / 6);

        double k = Math.pow(10, Math.floor(Math.log10(gridStep)));
        gridStep = (float)(Math.round(gridStep / k) * k);

        for(int i = 0; i <= 10; ++i) {
            float val = gridStep * i;
            gridVerts.put(currentChartViewBound.getLeft());
            gridVerts.put(val);
            gridVerts.put(currentChartViewBound.getRight());
            gridVerts.put(val);
        }

        float deli = 86400000 / currentChartViewBound.getWidth() * viewportWidth;
        double kk = Math.ceil(40 * dens / deli);
        kk = Math.pow(2, Math.ceil(Math.log(kk) / Math.log(2)));

        dateGridStep = (long)86400000 * (long)kk;
    }

    float curBloobX = 0;
    float curMarkerAl = 0;

    String FormatValue(float val, char c)
    {
        if(val == 0)
            return "0";
        int pw = (int)(Math.floor(Math.log10(val) / 3) * 3);
        String pf;
        if(pw < 3)
            return String.format("%.0f", val / Math.pow(10, pw));
        else if(pw < 6)
            pf  = "k";
        else if(pw < 9)
            pf = "M";
        else
            pf = "G";
        return String.format("%." + c + "f%s", val / Math.pow(10, pw), pf);
    }

    @Override
    protected void PostRender() {
        // Y-axis lables
        float[] matr = new float[16];
        Matrix.orthoM(mProjectionMatrix, 0, currentViewBound.getLeft(), currentViewBound.getRight(), currentViewBound.getBottom(), currentViewBound.getTop() == currentViewBound.getBottom() ? currentViewBound.getTop() + 1:currentViewBound.getTop(), -1, 1);
        Matrix.orthoM(screenMatrix, 0, 0, viewportWidth, 0, viewportHeight, -1, 1);
        for (int i = 0; i <= 10; ++i) {
            float val = gridStep * i;
            if ((val - currentChartViewBound.getBottom()) / currentChartViewBound.getHeight() * viewportHeight + 20 * dens > viewportHeight)
                break;

            textWriter.DrawText(FormatValue(val, '0'), 13.3f * dens, 0, (val - currentChartViewBound.getBottom()) / currentChartViewBound.getHeight() * viewportHeight + 20f * dens, screenMatrix, 0.588f, 0.635f, 0.667f, 1f);
        }
        // X-axis labels
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        SimpleDateFormat axisDateFormat = new SimpleDateFormat("MMM d", Locale.ENGLISH);
        long curTS = ((long) rChart.dataBoundBox.getRight());
        Date rounded = null;
        try {
            rounded = dateFormat.parse(dateFormat.format(new Date(curTS)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        curTS = rounded.getTime() - dateGridStep;

        int over = 0;
        float ha = Math.abs(40f*dens - dateGridStep / currentChartViewBound.getWidth() * viewportWidth) / 30f;
        ha = ha > 1 ? 1 : ha < 0 ? 0 : ha;
        while (curTS > rChart.dataBoundBox.getLeft()) {
            if (curTS <= currentChartViewBound.getRight() + 86400000 * 2 && curTS >= currentChartViewBound.getLeft() - 86400000 * 2) {
                Date date = new Date(curTS);
                float cx = (curTS - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth - 16.67f*dens;
                textWriter.DrawText(axisDateFormat.format(date), 13.3f * dens, cx, 0, screenMatrix, 0.588f, 0.635f, 0.667f, ha);
                ++over;
                if (over > 100)
                    break;
            }
            curTS -= dateGridStep * 2;
            if ((curTS - rChart.dataBoundBox.getLeft()) / dateGridStep > 1e4)
                break;
        }
        //
        curTS = rounded.getTime();

        over = 0;
        while (curTS > rChart.dataBoundBox.getLeft()) {
            if (curTS <= currentChartViewBound.getRight() + 86400000 * 2 && curTS >= currentChartViewBound.getLeft() - 86400000 * 2) {
                Date date = new Date(curTS);
                float cx = (curTS - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth - 16.67f*dens;
                textWriter.DrawText(axisDateFormat.format(date), 13.3f * dens, cx, 0, screenMatrix, 0.588f, 0.635f, 0.667f, 1);
                ++over;
                if (over > 100)
                    break;
            }
            curTS -= dateGridStep * 2;
            if ((curTS - rChart.dataBoundBox.getLeft()) / dateGridStep > 1e4)
                break;
        }

        if (markerIsVisible || curMarkerAl > 0.05) {
            float markerXValueSCR = (markerXValue - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth;
            curMarkerAl += ((markerIsVisible ? 1 : 0) - curMarkerAl) / 10;

            setLineWidth(1.6f);
            // Marker line
            Matrix.translateM(screenMatrix, 0, markerXValueSCR, 16.67f * dens, 0);
            simpleShaderProg.useProgram();
            simpleShaderProg.ApplyProjection(screenMatrix, null);
            if (nightMode)
                simpleShaderProg.SetColor(0.075f, 0.110f, 0.149f, 1f);
            else
                simpleShaderProg.SetColor(0.945f, 0.945f, 0.949f, 1f);
            markerVerts.position(0);
            simpleShaderProg.enableAttribs();
            glVertexAttribPointer(simpleShaderProg.getAttr("vertPos"), 2, GL_FLOAT, false, 0, markerVerts);
            glDrawArrays(GL_LINES, 0, 2);
            glEnable(GL_BLEND);
            // Marker points
            float[] markMat = screenMatrix.clone();
            for(int i = 0; i < markerYValues.size(); ++i)
            {
                MarkerValue val = markerYValues.get(i);
                Matrix.translateM(screenMatrix, 0, 0f, val.value / currentChartViewBound.getHeight() * viewportHeight, 0);
                Matrix.scaleM(screenMatrix, 0, 6.67f *dens, 6.67f *dens, 0);
                simpleShaderProg.ApplyProjection(screenMatrix, null);
                simpleShaderProg.SetColor(val.cR, val.cG, val.cB, curMarkerAl);
                glDrawArrays(GL_TRIANGLE_FAN, 8, (16+2));

                if(nightMode)
                    simpleShaderProg.SetColor(0.114f, 0.153f, 0.2f, curMarkerAl);
                else
                    simpleShaderProg.SetColor(1f, 1f, 1f, curMarkerAl);
                Matrix.scaleM(screenMatrix, 0, 0.68f, 0.68f, 0);
                simpleShaderProg.ApplyProjection(screenMatrix, null);
                glDrawArrays(GL_TRIANGLE_FAN, 8, (16+2));
                screenMatrix = markMat.clone();
            }

            screenMatrix = markMat.clone();

            // Precalc text size
            SimpleDateFormat markerDateFormat = new SimpleDateFormat("E, MMM d", Locale.ENGLISH);
            float cx = 0;
            for(int i = 0; i < markerYValues.size(); ++i) {
                MarkerValue val = markerYValues.get(i);
                cx += Math.max(textWriter.TextWidth(val.title, 13.3f*dens), textWriter.TextWidth(FormatValue(val.value, '2'), 16.67f*dens)) + 13.3f *dens;
            }
            cx = Math.max(cx, textWriter.TextWidth(markerDateFormat.format(new Date((long)markerXValue)), 13.3f * dens) + 13.3f * dens);

            float bloobX = 16.67f*dens;
            if((markerXValue - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth + cx + (16.67f) * dens > viewportWidth)
            {
                bloobX -= (16.67f*2) * dens + cx;
                if(bloobX < -(markerXValue - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth + 13.3f *dens)
                {
                    bloobX = -(markerXValue - currentChartViewBound.getLeft()) / currentChartViewBound.getWidth() * viewportWidth + 13.3f*dens;
                }
            }

            curBloobX += (bloobX - curBloobX) / 7;

            // Marker blob
            float tmp[] = new float[16];
            Matrix.translateM(screenMatrix, 0, curBloobX, viewportHeight - 83.33f *dens - 10, 0);
            tmp = screenMatrix.clone();
            Matrix.scaleM(screenMatrix, 0, cx, 66.67f*dens, 0);
            simpleShaderProg.ApplyProjection(screenMatrix, null);

            if(nightMode)
                simpleShaderProg.SetColor(0.114f, 0.153f, 0.2f, curMarkerAl);
            else
                simpleShaderProg.SetColor(1f, 1f, 1f, curMarkerAl);
            markerVerts.position(0);
            glDrawArrays(GL_TRIANGLE_FAN, 2, 6);

            if (nightMode)
                simpleShaderProg.SetColor(0.075f, 0.110f, 0.149f, curMarkerAl);
            else
                simpleShaderProg.SetColor(0.945f, 0.945f, 0.949f, curMarkerAl);
            markerVerts.position(0);
            glDrawArrays(GL_LINE_STRIP, 3, 6);

            // Marker blob text
            screenMatrix = tmp;
            Matrix.translateM(screenMatrix, 0, 6.67f*dens, 46.67f*dens, 0);
            simpleShaderProg.ApplyProjection(screenMatrix, null);

            if(nightMode)
                textWriter.DrawText(markerDateFormat.format(new Date((long)markerXValue)), 13.3f*dens, 0, 0, screenMatrix, 0.588f, 0.635f, 0.667f,curMarkerAl);
            else
                textWriter.DrawText(markerDateFormat.format(new Date((long)markerXValue)), 13.3f*dens, 0, 0, screenMatrix, 0,0,0,curMarkerAl);
            cx = 0;
            for(int i = 0; i < markerYValues.size(); ++i) {
                MarkerValue val = markerYValues.get(i);
                textWriter.DrawText(FormatValue(val.value, '2'), 16.67f*dens, cx, -20*dens, screenMatrix, val.cR, val.cG, val.cB, curMarkerAl);
                textWriter.DrawText(val.title, 13.3f*dens, cx, -33.3f*dens, screenMatrix, val.cR, val.cG, val.cB, curMarkerAl);
                cx += Math.max(textWriter.TextWidth(val.title, 13.3f*dens), textWriter.TextWidth(FormatValue(val.value, '2'), 16.67f*dens)) + 13.3f*dens;
            }

            if(markerXValueSCR > viewportWidth + 10 || markerXValueSCR < -10) {
                markerIsVisible = false;
            }
        } else {
            curBloobX = 0;
        }
    }

    @Override
    protected void PreRender() {
        Matrix.translateM(mProjectionMatrix, 0, 0, 16.67f * dens / viewportHeight * currentChartViewBound.getHeight() , 0);
        Matrix.orthoM(screenMatrix, 0, 0, viewportWidth, 0, viewportHeight, -1, 1);
        setLineWidth(1);
        simpleShaderProg.useProgram();
        if(nightMode)
            simpleShaderProg.SetColor(0.075f, 0.110f, 0.149f, 1f);
        else
            simpleShaderProg.SetColor(0.945f, 0.945f, 0.949f, 1f);
        gridVerts.position(0);
        glVertexAttribPointer(simpleShaderProg.getAttr("vertPos"), 2, GL_FLOAT, false, 0, gridVerts);
        simpleShaderProg.enableAttribs();

        simpleShaderProg.ApplyProjection(mProjectionMatrix, screenMatrix);
        gridVerts.position(0);
        glDrawArrays(GL_LINES, 0, 22);
    }

    boolean doMoving = false;

    @Override
    public void Move(float dx, float dy) {
        if(rChart == null || locked)
            return;

        dx /= viewportWidth;
        dx *= currentViewBound.getWidth() * 1.5f;

        if(Math.abs(dx) > currentChartViewBound.getWidth() * 0.01f)
            doMoving = true;

        rChart.targetViewBoundBox.translate(-dx, 0f);

        float overR = rChart.dataBoundBox.getRight() - rChart.targetViewBoundBox.getRight();
        float overL = rChart.dataBoundBox.getLeft() - rChart.targetViewBoundBox.getLeft();

        if(overR < 0)
        {
            rChart.targetViewBoundBox.translate(overR, 0f);
        }
        if(overL > 0)
        {
            rChart.targetViewBoundBox.translate(overL, 0f);
        }

        doZoomInCurrentBound();
    }

    boolean markerIsVisible = false;
    int markerIndex;
    float markerXValue = 0;
    ArrayList<MarkerValue> markerYValues;

    @Override
    public void TouchDown(float x, float y) {

    }

    @Override
    public void doZoomInCurrentBound()
    {
        super.doZoomInCurrentBound();
        if(markerIsVisible)
        {
            processMarker(markerXValue);
        }
    }

    void processMarker(float x)
    {
        int fndIndex = 0;
        if (!rChart.columns.values().isEmpty()) {
            Column col = rChart.columns.values().iterator().next();
            for (int i = 2; i < col.vertices.capacity(); i += 2) {
                if (col.vertices.get(i) >= x) {
                    fndIndex = i;
                    break;
                }
            }
            if(fndIndex == 0)
                return;
            if (Math.abs(col.vertices.get(fndIndex) - x) > Math.abs(col.vertices.get((fndIndex - 2)) - x)) {
                fndIndex -= 2;
            }
            markerIndex = fndIndex;

            markerYValues.clear();
            for (Iterator<Column> i = rChart.columns.values().iterator(); i.hasNext(); ) {
                Column ccol = i.next();
                if (ccol.isVisible) {
                    MarkerValue val = new MarkerValue();
                    val.value = ccol.vertices.get(markerIndex + 1);
                    val.title = ccol.name;
                    val.cR = ccol.cR;
                    val.cG = ccol.cG;
                    val.cB = ccol.cB;
                    markerYValues.add(val);
                }
            }

            markerXValue = col.vertices.get(fndIndex);

            markerIsVisible = true;
        }
    }

    @Override
    public void TouchUp(float x, float y) {
        if(locked) return;
        super.TouchUp(x, y);
        if (!doMoving && !locked) {
            x = (x / viewportWidth) * currentViewBound.getWidth() + currentViewBound.getLeft();
            processMarker(x);
        }
        doMoving = false;
    }

    private class MarkerValue
    {
        public float value;
        public String title;
        public float cR;
        public float cG;
        public float cB;
    }
}
