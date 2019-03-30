package com.example.chartdrawer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.Map;

public class ColumnListAdapter extends BaseAdapter {
    private final ArrayList mData;

    public ColumnListAdapter(Map<String, Column> map) {
        mData = new ArrayList();
        mData.addAll(map.entrySet());
    }

    MainActivity.VisiblityChangedListener listener;

    public void setVisiblityChangedListener(MainActivity.VisiblityChangedListener listener)
    {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View result;

        if (convertView == null) {
            result = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkable_row, parent, false);
        } else {
            result = convertView;
        }

        //Map.Entry<String, Column> item = (Map.Entry<String, Column>) getItem(position);
        Column column = ((Map.Entry<String, Column>) getItem(position)).getValue();

        // TODO replace findViewById by ViewHolder
        CheckBox cb = result.findViewById(R.id.checkBox);
        cb.setText(column.name);
        cb.setButtonTintList(ColorStateList.valueOf(Color.rgb((int)(column.cR * 255), (int)(column.cG * 255), (int)(column.cB *255))));
        cb.setTextColor(ColorStateList.valueOf(Color.rgb((int)(column.cR * 255), (int)(column.cG * 255), (int)(column.cB *255))));
        cb.setTag(column);
        cb.setChecked(column.isVisible);
        //cb.setScrollBarStyle();
        //cb.setOnClickListener();
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((Column)buttonView.getTag()).isVisible = isChecked;
                if(listener != null)
                    listener.OnVisiblityChanged();
                //Toast.makeText(getApplicationContext(), String.format("%s is %s", buttonView.getText(), Boolean.toString(isChecked)), Toast.LENGTH_SHORT).show();
            }
        });
        //((TextView) result.findViewById(android.R.id.text2)).setText(item.getValue());

        return result;
    }
}
