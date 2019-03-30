package com.example.chartdrawer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.preference.PreferenceManager;
import android.os.Bundle;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class MainActivity extends Activity {

    SharedPreferences mPrefs;

    static ArrayList<Chart> charts;
    static ArrayList<GLSurfaceView> oglSurfs;
    static ArrayList<View> vs;

    boolean loaded = false;

    Point screenSize = new Point();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPrefs =  PreferenceManager.getDefaultSharedPreferences(this);
        boolean isNightModeEnabled = mPrefs.getBoolean("NIGHT_MODE", false);
        setTheme(isNightModeEnabled ? R.style.AppThemeNight : R.style.AppTheme);
        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        display.getSize(screenSize);

        if(!supprtES2())
        {
            Toast.makeText(this, "OpenGL ES 2.0 is not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if(mPrefs.getBoolean("NIGHT_MODE", false))
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryNight));
        else
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));

        LinearLayout ll = findViewById(R.id.list_layout);

        ((ScrollView)findViewById(R.id.act_scroll)).getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            int cnt = 9;
            @Override
            public void onScrollChanged() {
                cnt++;
                if(cnt % 10 == 0)
                for(Object surf : oglSurfs) {
                    boolean visible = checkViewIsVisible((View)surf);
                    if (visible && ((GLChartView)surf).isPaused()) {
                        ((GLChartView) surf).onResume();
                    } else if (!visible && !((GLChartView)surf).isPaused()) {
                        ((GLChartView) surf).onPause();
                    }
                }
                if(cnt > 1e4) cnt = 0;
            }
        });

        if(charts == null)
            charts = new ArrayList<>();

        if(vs == null)
            vs = new ArrayList<>();

        if(oglSurfs == null)
            oglSurfs = new ArrayList<>();

        if(!loaded) {
            String jsonData = FileUtils.readTextFromRaw(this, R.raw.data);//extr.getString("JSON_INPUT");
            JSONArray jsonAr = null;
            Chart tmp = null;
            try {
                jsonAr = new JSONArray(jsonData);
                for (int i = 0; i < jsonAr.length(); ++i) {
                    JSONObject chart1 = jsonAr.getJSONObject(i);
                    tmp = new Chart(chart1);
                    tmp.targetViewBoundBox.setLeft(tmp.targetViewBoundBox.getLeft() + tmp.targetViewBoundBox.getWidth() * 0.7f);
                    charts.add(tmp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            for(int i = 0; i < charts.size(); ++i) {
                View ele = getLayoutInflater().inflate(R.layout.chart_layout, ll, false);

                ChartRenderer chartRenderer = new ChartRenderer(getApplicationContext(), supprtES32());
                chartRenderer.setNightMode(isNightModeEnabled);
                chartRenderer.setChart(charts.get(i));

                ((GLChartView) ele.findViewById(R.id.GLChartView)).setEGLConfigChooser(new GLSurfaceView.EGLConfigChooser() {
                    @Override
                    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                        int attribs[] = {
                                EGL10.EGL_LEVEL, 0,
                                EGL10.EGL_RENDERABLE_TYPE, 4,  // EGL_OPENGL_ES2_BIT
                                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                                EGL10.EGL_RED_SIZE, 8,
                                EGL10.EGL_GREEN_SIZE, 8,
                                EGL10.EGL_BLUE_SIZE, 8,
                                EGL10.EGL_DEPTH_SIZE, 16,
                                EGL10.EGL_SAMPLE_BUFFERS, 1,
                                EGL10.EGL_SAMPLES, 0,  // This is for 4x MSAA.
                                EGL10.EGL_NONE
                        };
                        EGLConfig[] configs = new EGLConfig[1];
                        int[] configCounts = new int[1];
                        egl.eglChooseConfig(display, attribs, configs, 1, configCounts);

                        if (configCounts[0] == 0) {
                            // Failed! Error handling.
                            return null;
                        } else {
                            return configs[0];
                        }
                    }
                });
                oglSurfs.add((GLSurfaceView)ele.findViewById(R.id.GLChartView));
                ViewGroup.LayoutParams layoutParams = ((GLChartView) ele.findViewById(R.id.GLChartView)).getLayoutParams();
                layoutParams.height = (int) (screenSize.x * 0.8);
                ((GLChartView) ele.findViewById(R.id.GLChartView)).setLayoutParams(layoutParams);
                ((GLChartView) ele.findViewById(R.id.GLChartView)).setRenderer(chartRenderer);
                ((GLChartView) ele.findViewById(R.id.GLChartView)).onPause();

                ChartPreviewRenderer chartPrvRenderer = new ChartPreviewRenderer(getApplicationContext());
                chartPrvRenderer.setChart(charts.get(i));
                chartPrvRenderer.setNightMode(isNightModeEnabled);
                oglSurfs.add((GLSurfaceView)ele.findViewById(R.id.GLChartPreView));
                ((GLChartView) ele.findViewById(R.id.GLChartPreView)).setRenderer(chartPrvRenderer);
                ((GLChartView) ele.findViewById(R.id.GLChartPreView)).onPause();

                ((TextView) ele.findViewById(R.id.title)).setText(String.format("Followers chart #%d", i + 1));

                SelectorListView colList = ele.findViewById(R.id.chart_selector);

                ColumnListAdapter ada = new ColumnListAdapter(charts.get(i).columns);
                ada.setVisiblityChangedListener(new VisiblityChangedListener(chartRenderer, chartPrvRenderer, charts.get(i)));
                colList.setAdapter(ada);
                colList.addFooterView(new View(this));

                vs.add(ele);
                //ll.addView(ele);
            }
            loaded = true;
        }


        for(View v : vs)
        {
            if(v.getParent() != null)
                ((LinearLayout) v.getParent()).removeView(v);
            ll.addView(v);
        }

/*
        chartRenderer = new ChartRenderer(getApplicationContext(), supprtES32());
        chartPreviewRenderer = new ChartPreviewRenderer(getApplicationContext());

        chartPreviewRenderer.setChart(rChart);
        chartRenderer.setChart(rChart);

        ((GLChartView)findViewById(R.id.GLChartView)).setRenderer(chartRenderer);
        ((GLChartView)findViewById(R.id.GLChartPreView)).setRenderer(chartPreviewRenderer);

        chartRenderer.setNightMode(isNightModeEnabled);
        chartPreviewRenderer.setNightMode(isNightModeEnabled);

        colList = findViewById(R.id.columns_list);

        ColumnListAdapter ada = new ColumnListAdapter(rChart.columns);
        ada.setVisiblityChangedListener(new VisiblityChangedListener() {
            @Override
            public void OnVisiblityChanged() {
                chartRenderer.doZoomInCurrentBound();
                rChart.updateDataBoundBox();
            }
        });
        colList.setAdapter(ada);
        colList.addFooterView(new View(this));*/
    }
/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_lay, menu);
        return true;
    }
*/
    public boolean checkViewIsVisible(final View view) {
        if (view == null)
            return false;

        if (!view.isShown())
            return false;

        final Rect actualPosition = new Rect();
        view.getGlobalVisibleRect(actualPosition);

        final Rect screen = new Rect(0, 0, screenSize.x, screenSize.y);
        boolean res = actualPosition.intersect(screen);
        return actualPosition.intersect(screen);
    }

    public void NightDaySwitch(View view)
    {
        boolean nMode = !mPrefs.getBoolean("NIGHT_MODE", false);

        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putBoolean("NIGHT_MODE", nMode);
        ed.commit();

        for(GLSurfaceView suf : oglSurfs)
        {
            ((BaseChartRenderer)(((GLChartView)suf).renderer)).setNightMode(mPrefs.getBoolean("NIGHT_MODE", false));
        }

        if(nMode) {
            ColorDrawable[] colorTr = {new ColorDrawable(getResources().getColor(R.color.colorActivityBackground)), new ColorDrawable(getResources().getColor(R.color.colorActivityBackgroundNight))};
            ColorDrawable[] colorBarTr = {new ColorDrawable(getResources().getColor(R.color.colorPrimary)), new ColorDrawable(getResources().getColor(R.color.colorPrimaryNight))};
            TransitionDrawable trans = new TransitionDrawable(colorTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
            TransitionDrawable transBar = new TransitionDrawable(colorBarTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
            //((View)getWindow().findViewById(android.R.id.title).getParent()).setBackground(trans);
            //getActionBar().setBackgroundDrawable(trans);
            findViewById(R.id.mainact_lay).setBackground(trans);//(getResources().getColor(R.color.colorActivityBackgroundNight));
            findViewById(R.id.linearLayout).setBackground(transBar);
            trans.startTransition(200);
            transBar.startTransition(200);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDarkNight));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryNight));
            ((ImageButton)findViewById(R.id.nightDayButton)).setImageResource(R.drawable.ic_action_daymode);
        } else {
            ColorDrawable[] colorTr = {new ColorDrawable(getResources().getColor(R.color.colorActivityBackgroundNight)), new ColorDrawable(getResources().getColor(R.color.colorActivityBackground))};
            ColorDrawable[] colorBarTr = {new ColorDrawable(getResources().getColor(R.color.colorPrimaryNight)), new ColorDrawable(getResources().getColor(R.color.colorPrimary))};
            TransitionDrawable trans = new TransitionDrawable(colorTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
            TransitionDrawable transBar = new TransitionDrawable(colorBarTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
            //((View)getWindow().findViewById(android.R.id.title).getParent()).setBackground(trans);
            findViewById(R.id.mainact_lay).setBackground(trans);//(getResources().getColor(R.color.colorActivityBackgroundNight));
            findViewById(R.id.linearLayout).setBackground(transBar);
            //getActionBar().setBackgroundDrawable(trans);
            trans.startTransition(200);
            transBar.startTransition(200);
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));
            ((ImageButton)findViewById(R.id.nightDayButton)).setImageResource(R.drawable.ic_action_nightmode);
            //findViewById(R.id.mainact_lay).setBackgroundColor(getResources().getColor(R.color.colorActivityBackground));
        }
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_maintain:
                boolean nMode = !mPrefs.getBoolean("NIGHT_MODE", false);
                onPause();

                SharedPreferences.Editor ed = mPrefs.edit();
                ed.putBoolean("NIGHT_MODE", nMode);
                ed.commit();

                for(GLSurfaceView suf : oglSurfs)
                {
//                    suf.onPause();
                    ((BaseChartRenderer)(((GLChartView)suf).renderer)).setNightMode(mPrefs.getBoolean("NIGHT_MODE", false));
                }

                for(GLSurfaceView suf : oglSurfs)
                {
  //                  suf.onResume();
                }

                if(nMode) {
                    ColorDrawable[] colorTr = {new ColorDrawable(getResources().getColor(R.color.colorActivityBackground)), new ColorDrawable(getResources().getColor(R.color.colorActivityBackgroundNight))};
                    TransitionDrawable trans = new TransitionDrawable(colorTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
                    //((View)getWindow().findViewById(android.R.id.title).getParent()).setBackground(trans);
                    getActionBar().setBackgroundDrawable(trans);
                    findViewById(R.id.mainact_lay).setBackground(trans);//(getResources().getColor(R.color.colorActivityBackgroundNight));
                    trans.startTransition(200);
                    getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDarkNight));
                    getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryNight));
                } else {
                    ColorDrawable[] colorTr = {new ColorDrawable(getResources().getColor(R.color.colorActivityBackgroundNight)), new ColorDrawable(getResources().getColor(R.color.colorActivityBackground))};
                    TransitionDrawable trans = new TransitionDrawable(colorTr);//(TransitionDrawable)findViewById(R.id.mainact_lay).getBackground();
                    //((View)getWindow().findViewById(android.R.id.title).getParent()).setBackground(trans);
                    findViewById(R.id.mainact_lay).setBackground(trans);//(getResources().getColor(R.color.colorActivityBackgroundNight));
                    getActionBar().setBackgroundDrawable(trans);
                    trans.startTransition(200);
                    getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
                    getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimary));
                    //findViewById(R.id.mainact_lay).setBackgroundColor(getResources().getColor(R.color.colorActivityBackground));
                }
                onResume();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    public class VisiblityChangedListener {
        public VisiblityChangedListener(ChartRenderer renderer, ChartPreviewRenderer rendererPrev, Chart chart)
        {
            this.renderer = renderer;
            this.rendererPrev = rendererPrev;
            this.chart = chart;
        }

        ChartRenderer renderer;
        ChartPreviewRenderer rendererPrev;
        Chart chart;

        public void OnVisiblityChanged() {
            for(Column col : chart.columns.values())
            {
                if(col.isVisible)
                {
                    renderer.doZoomInCurrentBound();
                    chart.updateDataBoundBox();
                    renderer.setLocked(false);
                    rendererPrev.setLocked(false);
                    return;
                }
            }
            renderer.setLocked(true);
            rendererPrev.setLocked(true);
            renderer.markerIsVisible = false;
        }
    }

    //ChartRenderer chartRenderer;
    //ChartPreviewRenderer chartPreviewRenderer;

    @Override
    protected void onPause()
    {
        super.onPause();
        for(Object surf : oglSurfs)
            ((GLSurfaceView)surf).onPause();
       // ((GLSurfaceView)findViewById(R.id.GLChartView)).onPause();
       // ((GLSurfaceView)findViewById(R.id.GLChartPreView)).onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        for(Object surf : oglSurfs)
            ((GLSurfaceView)surf).onResume();
       // ((GLSurfaceView)findViewById(R.id.GLChartView)).onResume();
       // ((GLSurfaceView)findViewById(R.id.GLChartPreView)).onResume();
    }

    private boolean supprtES2()
    {
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return  (configurationInfo.reqGlEsVersion >= 0x20000);
    }

    private boolean supprtES32()
    {
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return  (configurationInfo.reqGlEsVersion >= 0x30002);
    }
}
