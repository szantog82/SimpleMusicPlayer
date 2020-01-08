package com.example.szantog.musicplayer;

import java.util.ArrayList;

public class FileItem {

    private String path;
    private short type;
    private boolean isChecked;

    public static final short IS_FILE = 0;
    public static final short IS_FOLDER = 1;

    public FileItem(String path, short type, boolean isChecked) {
        this.path = path;
        this.type = type;
        this.isChecked = isChecked;
    }

    public String getPath() {
        return path;
    }

    public String getStrippedPath() {
        int pos = path.lastIndexOf("/");
        return path.substring(pos + 1);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public static void sortArray(ArrayList<FileItem> items) {
        int i = 0;
        int j = items.size() - 1;
        while (i < j) {
            while (i < items.size() - 1 && items.get(i).getType() == IS_FOLDER) {
                i++;
            }
            while (items.get(j).getType() == IS_FILE && j > 0) {
                j--;
            }
            if (i < j) {
                FileItem temp = items.get(i);
                items.set(i, items.get(j));
                items.set(j, temp);
                i++;
                j--;
            }
        }
        combSort(IS_FILE, items);
        combSort(IS_FOLDER, items);
    }

    public static void sortStringArray(ArrayList<String> array) {
        int gap = array.size();
        while (gap >= 1) {
            for (int i = 0; i < array.size() - gap; i++) {
                if (array.get(i).toLowerCase().compareTo(array.get(i + gap).toLowerCase()) > 0) {
                    String temp = array.get(i);
                    array.set(i, array.get(i + gap));
                    array.set(i + gap, temp);
                }
            }
            gap = gap * 10 / 13;
        }
    }

    public static void sortFileItemByName(ArrayList<FileItem> array) {
        int gap = array.size();
        while (gap >= 1) {
            for (int i = 0; i < array.size() - gap; i++) {
                if (array.get(i).getPath().toLowerCase().compareTo(array.get(i + gap).getPath().toLowerCase()) > 0) {
                    FileItem temp = array.get(i);
                    array.set(i, array.get(i + gap));
                    array.set(i + gap, temp);
                }
            }
            gap = gap * 10 / 13;
        }
    }

    private static void combSort(short type, ArrayList<FileItem> items) {
        int size = items.size();
        int gap = size - 1;
        while (gap >= 1) {
            for (int i = 0; i < size - gap; i++) {
                if (items.get(i).getType() == type && items.get(i + gap).getType() == type &&
                        items.get(i).getPath().toLowerCase().compareTo(items.get(i + gap).getPath().toLowerCase()) > 0) {
                    FileItem temp = items.get(i);
                    items.set(i, items.get(i + gap));
                    items.set(i + gap, temp);
                }
            }
            gap = gap * 10 / 13;
        }
    }

    public static String getFilenameFromPath(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(index + 1);
    }

    public static String getFoldernameFromPath(String path) {
        int index = path.lastIndexOf("/");
        return path.substring(0, index);
    }
}
