package com.example.szantog.musicplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity_withFileItems extends AppCompatActivity implements PlaybackService.PlaybackListener, AdapterView.OnItemClickListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener, ListFilesAdapter.OnFileCheckedChangeListener {

    private PlaybackService service;
    private Intent serviceIntent;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedEditor;

    private String currentPath = "/";
    private ListFilesAdapter adapter;

    private DrawerLayoutListAdapter2 drawerLayoutListAdapter2;
    private ArrayList<Integer> currentPosition = new ArrayList<>();
    private ArrayList<FileItem> songCollectionPathsToSend = new ArrayList<>();
    private boolean isRepeatAll = true;
    private TextView songnameTextView;
    private TextView durationPosTextView;
    private ImageView playPauseButton;
    private SeekBar seekBar;

    private ArrayList<FileItem> currentDirItems = new ArrayList<>();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mainactivity_menu, menu);
        if (sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false)) {
            menu.findItem(R.id.mainmenu_menuitem_repeat).setIcon(R.drawable.repeat_one);
        } else {
            menu.findItem(R.id.mainmenu_menuitem_repeat).setIcon(R.drawable.repeat_all);
        }
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mainmenu_menuitem_repeat) {
            if (service != null) {
                isRepeatAll = !isRepeatAll;
                if (isRepeatAll) {
                    item.setIcon(R.drawable.repeat_all);
                    sharedEditor.putBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false);
                } else {
                    item.setIcon(R.drawable.repeat_one);
                    sharedEditor.putBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), true);
                }
                sharedEditor.apply();
                service.changeRepeatState(isRepeatAll);
            }
        } else if (item.getItemId() == R.id.mainmenu_menuitem_quit) {
            stopService(serviceIntent);
            finish();
        } else if (item.getItemId() == R.id.mainmenu_menuitem_favourite) {
            Set<String> set;
            if (sharedPreferences.getStringSet(getString(R.string.SHARED_FAV_DIRS), null) == null) {
                set = new HashSet<>();
            } else {
                set = sharedPreferences.getStringSet(getString(R.string.SHARED_FAV_DIRS), null);
            }
            set.add(currentPath);
            sharedEditor.putStringSet(getString(R.string.SHARED_FAV_DIRS), set);
            sharedEditor.apply();
            updateFavoriteFolders();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!currentPath.equals("/")) {
            currentPath = FileItem.getFoldernameFromPath(currentPath);
            if (currentPath.length() == 0) {
                currentPath = "/";
            }
            listDir();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_QUIT))) {
                if (serviceIntent == null) {
                    serviceIntent = new Intent(this, PlaybackService.class);
                }
                stopService(serviceIntent);
                finish();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_PAUSEPLAY))) {
                if (service != null)
                    service.playPausePlayback();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_REWIND))) {
                if (service != null)
                    service.rewindSong();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_FF))) {
                if (service != null)
                    service.fastForwardSong();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, PlaybackService.class);

        sharedPreferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
        sharedEditor = sharedPreferences.edit();
        if (sharedPreferences.getString(getString(R.string.SHARED_LAST_DIR), null) != null) {
            currentPath = sharedPreferences.getString(getString(R.string.SHARED_LAST_DIR), null);
        }

        final DrawerLayout drawerLayout = findViewById(R.id.drawerlayout);
        drawerLayout.bringToFront();
        final ListView navListView = findViewById(R.id.navList);
        currentPosition.add(0);
        drawerLayoutListAdapter2 = new DrawerLayoutListAdapter2(this, songCollectionPathsToSend, currentPosition);
        navListView.setAdapter(drawerLayoutListAdapter2);
        navListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                if (service != null) {
                    service.jumpToSong(pos);
                    startService(serviceIntent);
                    service.changeRepeatState(!sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false));
                }
                drawerLayout.closeDrawer(Gravity.START);
            }
        });

        songnameTextView = findViewById(R.id.mainactivity_songname_textview);
        durationPosTextView = findViewById(R.id.mainactivity_duration_pos_textview);
        seekBar = findViewById(R.id.mainactivity_seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        ImageView rewindButton = findViewById(R.id.mainactivity_rew_button);
        playPauseButton = findViewById(R.id.mainactivity_play_button);
        ImageView ffButton = findViewById(R.id.mainactivity_ff_button);
        rewindButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        ffButton.setOnClickListener(this);

        updateFavoriteFolders();

        ListView listView = findViewById(R.id.listview);
        adapter = new ListFilesAdapter(this, currentDirItems);
        adapter.setOnFileCheckedChangedListener(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listDir();

        if (sharedPreferences.getString(getString(R.string.SHARED_LAST_SONGS_ARRAY), null) != null) {
            songCollectionPathsToSend.clear();
            ArrayList<String> savedPaths = new ArrayList<>(Arrays.asList(sharedPreferences.getString(getString(R.string.SHARED_LAST_SONGS_ARRAY), null).split(getString(R.string.DIVIDER))));
            for (String path : savedPaths) {
                songCollectionPathsToSend.add(new FileItem(path, FileItem.IS_FILE, true));
            }
        }
        currentPosition.clear();
        currentPosition.add(sharedPreferences.getInt(getString(R.string.SHARED_LAST_SONGS_INDEX), 0));
        drawerLayoutListAdapter2.notifyDataSetChanged();
    }

    private void updateFavoriteFolders() {
        LinearLayout favButtonsContainer = findViewById(R.id.fav_buttons_container);
        final Set<String> set = sharedPreferences.getStringSet(getString(R.string.SHARED_FAV_DIRS), null);
        if (set == null || set.isEmpty()) {
            favButtonsContainer.setVisibility(View.GONE);
        } else {
            favButtonsContainer.setVisibility(View.VISIBLE);
            favButtonsContainer.removeAllViews();
            for (String str : set) {
                Button btn = new Button(this);
                btn.setText(FileItem.getFilenameFromPath(str));
                btn.setTag(str);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        currentPath = view.getTag().toString();
                        listDir();
                    }
                });
                btn.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        set.remove(view.getTag());
                        sharedEditor.putStringSet(getString(R.string.SHARED_FAV_DIRS), set);
                        sharedEditor.apply();
                        updateFavoriteFolders();
                        return true;
                    }
                });
                favButtonsContainer.addView(btn);
            }
        }
    }

    private void listDir() {
        currentDirItems.clear();
        File dir = new File(currentPath);
        File[] files = dir.listFiles(fileFilter);
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    currentDirItems.add(new FileItem(file.getPath(), FileItem.IS_FOLDER, true));
                } else if (file.isFile()) {
                    currentDirItems.add(new FileItem(file.getPath(), FileItem.IS_FILE, true));
                }
            }
        }
        FileItem.sortArray(currentDirItems);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
        sharedEditor.putString(getString(R.string.SHARED_LAST_DIR), currentPath);
        String strToSave = "";
        for (int cnt = 0; cnt < songCollectionPathsToSend.size(); cnt++) {
            strToSave += songCollectionPathsToSend.get(cnt) + getString(R.string.DIVIDER);
        }
        sharedEditor.putString(getString(R.string.SHARED_LAST_SONGS_ARRAY), strToSave);
        sharedEditor.putInt(getString(R.string.SHARED_LAST_SONGS_INDEX), currentPosition.get(0));
        sharedEditor.apply();
    }

    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) iBinder;
            service = binder.getService();
            if (service != null) {
                service.setPlaybackListener(MainActivity_withFileItems.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void onSeekUpdate(String filename, int currentPosition, int duration) {
        songnameTextView.setText(FileItem.getFilenameFromPath(filename));
        seekBar.setProgress(currentPosition * 100 / duration);
        durationPosTextView.setText(formatDuration(currentPosition) + "/" + formatDuration(duration));
    }

    @NonNull
    private String formatDuration(int num) {
        int minute = num / 1000 / 60;
        int second = (num - minute * 60 * 1000) / 1000;
        if (second < 10) {
            return String.valueOf(minute) + ":0" + String.valueOf(second);
        } else {
            return String.valueOf(minute) + ":" + String.valueOf(second);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    @Override
    public void onCurrentFilePositionChanged(int newPosition) {
        currentPosition.clear();
        currentPosition.add(newPosition);
        drawerLayoutListAdapter2.notifyDataSetChanged();
    }

    private FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            String[] extensions = {".mp3", ".ogg"};
            for (String ext : extensions) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(ext)) {
                    return true;
                }
            }
            if (file.isDirectory()) {
                return true;
            }
            return false;
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        File file = new File(currentDirItems.get(i).getPath());
        if (file.isDirectory()) {
            currentPath = currentDirItems.get(i).getPath();
            listDir();
        } else if (file.isFile()) {
            File currentDir = new File(FileItem.getFoldernameFromPath(file.getPath()));
            File[] files = currentDir.listFiles(fileFilter);
            songCollectionPathsToSend.clear();
            for (File f : files) {
                if (f.isFile()) {
                    songCollectionPathsToSend.add(new FileItem(f.getPath(), FileItem.IS_FILE, true));
                }
            }
            FileItem.sortFileItemByName(songCollectionPathsToSend);
            int index = 0;
            while (!songCollectionPathsToSend.get(index).getPath().equals(currentDirItems.get(i).getPath())) {
                index++;
            }
            int length = songCollectionPathsToSend.size();
            if (index > length / 2) {
                for (int k = 0; k < length / 2; k++) {
                    if (k + length / 2 < length) {
                        FileItem temp = songCollectionPathsToSend.get(k);
                        songCollectionPathsToSend.set(k, songCollectionPathsToSend.get(length / 2 + k));
                        songCollectionPathsToSend.set(length / 2 + k, temp);
                    }
                }
                index = index - length / 2;
            }
            for (int k = 0; k < length; k++) {
                if (k + index < length) {
                    FileItem temp = songCollectionPathsToSend.get(k);
                    songCollectionPathsToSend.set(k, songCollectionPathsToSend.get(index + k));
                    songCollectionPathsToSend.set(index + k, temp);
                }
            }
            drawerLayoutListAdapter2.notifyDataSetChanged();
            ArrayList<String> stringPathsToSend = new ArrayList<>();
            for (FileItem item : songCollectionPathsToSend) {
                stringPathsToSend.add(item.getPath());
            }
            serviceIntent.putStringArrayListExtra(getString(R.string.FILES_TO_PLAY), stringPathsToSend);
            serviceIntent.setAction(getString(R.string.NORMAL_START_INTENT_ACTION));
            startService(serviceIntent);
            service.changeRepeatState(!sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false));
            serviceIntent.setAction("");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.mainactivity_rew_button:
                if (service != null)
                    service.rewindSong();
                break;
            case R.id.mainactivity_play_button:
                if (service != null)
                    service.playPausePlayback();
                break;
            case R.id.mainactivity_ff_button:
                if (service != null)
                    service.fastForwardSong();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (service != null)
                service.seekSong(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onFileCheckedChanged(int index, boolean newValue) {
        if (service != null) {
            songCollectionPathsToSend.set(index, new FileItem(songCollectionPathsToSend.get(index).getPath(),
                    songCollectionPathsToSend.get(index).getType(), newValue));
            drawerLayoutListAdapter2.notifyDataSetChanged();
            int currentDirItemIndex = 0;
            Log.e("index", String.valueOf(index));
            Log.e("filename", songCollectionPathsToSend.get(index).getPath().toString());
            while (!currentDirItems.get(currentDirItemIndex).getPath().equals(songCollectionPathsToSend.get(index).getPath())) {
                currentDirItemIndex++;
            }
            FileItem changedItem = currentDirItems.get(currentDirItemIndex);
            changedItem.setChecked(newValue);
            currentDirItems.set(currentDirItemIndex, changedItem);
            adapter.notifyDataSetChanged();
            service.changesPlayListByCheckbox(songCollectionPathsToSend);
        }
    }
}