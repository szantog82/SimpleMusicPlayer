package com.example.szantog.musicplayer;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DrawerLayoutListAdapter2 extends BaseAdapter {

    private Context context;
    private ArrayList<FileItem> items;
    private ArrayList<Integer> currentPosition;

    public DrawerLayoutListAdapter2(Context context, ArrayList<FileItem> items, ArrayList<Integer> currentPosition) {
        this.context = context;
        this.items = items;
        this.currentPosition = currentPosition;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        }
        TextView textView = view.findViewById(android.R.id.text1);
        if (items.get(i).isChecked()) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(FileItem.getFilenameFromPath(items.get(i).getPath()));
        } else {
            textView.setVisibility(View.GONE);
        }
        if (currentPosition.get(0) == i) {
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.DrawerLayoutHighlighted));
        } else {
            textView.setBackgroundColor(ContextCompat.getColor(context, R.color.DrawerLayoutBackground));
        }
        return view;
    }
}
