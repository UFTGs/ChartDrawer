package com.example.chartdrawer;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Chart {
    public Map<String, Column> columns;
    private FloatBuffer xValues;
    private Map<String, FloatBuffer> yValues;

    BoundBox dataBoundBox;
    BoundBox targetViewBoundBox;

    public Chart(JSONObject obj)
    {
        dataBoundBox = new BoundBox();
        targetViewBoundBox = new BoundBox();

        columns = new HashMap<String, Column>();
        yValues = new HashMap<String, FloatBuffer>();
        try {
            JSONArray columnsArray = obj.getJSONArray("columns");
            float tmp = 0;
            for(int i = 0; i < columnsArray.length(); ++i)
            {
                JSONArray columnArray = columnsArray.getJSONArray(i);
                String key = columnArray.getString(0);
                if(key.equals("x"))
                {
                    int ptc = columnArray.length() - 1;
                    xValues = ByteBuffer.allocateDirect(ptc * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
                    xValues.position(0);
                    for(int j = 1; j <= ptc; ++j)
                    {
                        tmp = (float)columnArray.getDouble(j);
                        xValues.put(tmp);
                    }
                } else {
                    int ptc = columnArray.length() - 1;
                    yValues.put(key, ByteBuffer.allocateDirect(ptc * 4).order(ByteOrder.nativeOrder()).asFloatBuffer());
                    yValues.get(key).position(0);
                    for(int j = 1; j <= ptc; ++j)
                    {
                        tmp = (float)columnArray.getDouble(j);
                        yValues.get(key).put(tmp);
                    }
                }
            }
            int frameSize = (int)Math.ceil(xValues.capacity() / 5000.0 + 0.5);
            double localMean = 0, localMeanX = 0;
            for(Map.Entry<String, FloatBuffer> pair : yValues.entrySet())
            {
                Column newColumn = new Column(pair.getKey(), (int)(Math.ceil(xValues.capacity() / (1.0f * frameSize)))*1+2);

                xValues.position(0);
                pair.getValue().position(0);

                int p = 0;
                for (int i = 0; i < xValues.capacity(); i += frameSize)
                {
                    p = xValues.capacity() - i > frameSize ? frameSize : xValues.capacity() - i;

                    localMean = 0;
                    localMeanX = 0;

                    for(int j = 0; j < p; ++j) {
                        localMeanX += xValues.get(i+j);
                        localMean += pair.getValue().get(i+j);
                    }
                    localMeanX /= p;
                    localMean /= p;
                    if(localMeanX > newColumn.dataBoundBox.getRight()) newColumn.dataBoundBox.setRight((float)localMeanX);
                    if(localMeanX < newColumn.dataBoundBox.getLeft()) newColumn.dataBoundBox.setLeft((float)localMeanX);
                    if(localMean > newColumn.dataBoundBox.getTop()) newColumn.dataBoundBox.setTop((float)localMean);
                    if(localMean < newColumn.dataBoundBox.getBottom()) newColumn.dataBoundBox.setBottom((float)localMean);
                    newColumn.vertices.put((float)localMeanX);
                    newColumn.vertices.put((float)localMean);

                    if(localMeanX > dataBoundBox.getRight()) dataBoundBox.setRight((float)localMeanX);
                    if(localMeanX < dataBoundBox.getLeft()) dataBoundBox.setLeft((float)localMeanX);
                    if(localMean > dataBoundBox.getTop()) dataBoundBox.setTop((float)localMean);
                    if(localMean < dataBoundBox.getBottom()) dataBoundBox.setBottom((float)localMean);

                    if(i == 0)
                    {
                        newColumn.vertices.put((float)localMeanX);
                        newColumn.vertices.put((float)localMean);
                    }
                }
                newColumn.vertices.put((float)localMeanX);
                newColumn.vertices.put((float)localMean);
                columns.put(newColumn.key, newColumn);
                pair.getValue().clear();
            }

            JSONObject typesObj = obj.getJSONObject("types");
            Iterator<String> it = typesObj.keys();
            while(it.hasNext()) {
                String k = it.next();
                if(typesObj.getString(k).equals("line"))
                {
                    columns.get(k).type = "line";
                }
            }

            JSONObject namesObj = obj.getJSONObject("names");
            it = namesObj.keys();
            while(it.hasNext()) {
                String k = it.next();
                columns.get(k).name = namesObj.getString(k);
            }

            JSONObject colorsObj = obj.getJSONObject("colors");
            it = colorsObj.keys();
            while(it.hasNext()) {
                String k = it.next();
                String st = colorsObj.getString(k);
                int color = Color.parseColor(st);
                columns.get(k).cR = Color.red(color) / 255f;
                columns.get(k).cG = Color.green(color) / 255f;
                columns.get(k).cB = Color.blue(color) / 255f;
                columns.get(k).cA = 1.0f;
            }
            xValues.clear();
            yValues.clear();

            dataBoundBox.setBottom(0);
            dataBoundBox.setTop(dataBoundBox.getTop() * 1.1f);
            targetViewBoundBox.setFrom(dataBoundBox);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void updateDataBoundBox()
    {
        dataBoundBox = new BoundBox(Float.MAX_VALUE, -Float.MAX_VALUE, 0, 1);
        for(Map.Entry<String, Column> p : columns.entrySet())
        {
            Column col = p.getValue();
            if(col.isVisible)
            {
                if(col.dataBoundBox.getTop() > dataBoundBox.getTop()) dataBoundBox.setTop(col.dataBoundBox.getTop());
                if(col.dataBoundBox.getRight() > dataBoundBox.getRight()) dataBoundBox.setRight(col.dataBoundBox.getRight());
                if(col.dataBoundBox.getLeft() < dataBoundBox.getLeft()) dataBoundBox.setLeft(col.dataBoundBox.getLeft());
            }
        }
        dataBoundBox.setTop(dataBoundBox.getTop() * 1.1f);
    }
}
