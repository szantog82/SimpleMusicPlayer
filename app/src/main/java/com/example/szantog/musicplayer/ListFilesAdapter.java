package com.example.szantog.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class ListFilesAdapter extends BaseAdapter {

    public interface OnFileCheckedChangeListener {
        void onFileCheckedChanged(int index, boolean newValue);
    }

    private OnFileCheckedChangeListener listener;
    private Context context;
    private ArrayList<FileItem> items;

    public ListFilesAdapter(Context context, ArrayList<FileItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.folder_listview_elements, viewGroup, false);
        }
        ImageView imageView = view.findViewById(R.id.folder_listview_elements_img);
        TextView textView = view.findViewById(R.id.folder_listview_elements_name);
        CheckBox checkBox = view.findViewById(R.id.folder_listview_elements_checkbox);
        textView.setText(items.get(i).getStrippedPath());
        if (items.get(i).getType() == FileItem.IS_FILE) {
            imageView.setImageResource(android.R.drawable.ic_media_play);
            checkBox.setVisibility(View.VISIBLE);
        } else {
            imageView.setImageResource(R.drawable.folderup128x128);
            checkBox.setVisibility(View.GONE);
        }
        checkBox.setChecked(items.get(i).isChecked());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (listener != null) {
                    listener.onFileCheckedChanged(i, isChecked);
                }
            }
        });

        return view;
    }

    public void setOnFileCheckedChangedListener(OnFileCheckedChangeListener listener) {
        this.listener = listener;
    }
}
