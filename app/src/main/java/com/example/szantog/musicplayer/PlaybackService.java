package com.example.szantog.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    public interface PlaybackListener {
        void onSeekUpdate(String filename, int currentPosition, int duration);

        void onPlaybackStateChanged(boolean isPlaying);

        void onCurrentFilePositionChanged(int newPosition);
    }

    class MediaFile {
        private String path;
        private boolean toBePlayed;

        public MediaFile(String path, boolean toBePlayed) {
            this.path = path;
            this.toBePlayed = toBePlayed;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isToBePlayed() {
            return toBePlayed;
        }

        public void setToBePlayed(boolean toBePlayed) {
            this.toBePlayed = toBePlayed;
        }
    }


    private PlaybackListener listener;
    private final IBinder binder = new LocalBinder();
    private PowerManager.WakeLock wakeLock;
    private final String WAKELOCK_TAG = "wakelock_tag123";

    private TelephonyManager telephonyManager;
    private BroadcastReceiver telephonyStateManager;
    private NotificationManager notificationManager;
    private RemoteViews notificationView;
    private Notification.Builder notification;
    private int NOTIFICATION_ID = 123;

    private MediaPlayer mediaPlayer;
    private Handler handler;
    private ArrayList<String> filesToPlay;
    private ArrayList<MediaFile> filesToPlay2 = new ArrayList<>();
    private int currentFilePosition;
    private boolean repeatAll = true;

    public class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notification = new Notification.Builder(this);
        notificationView = new RemoteViews(getApplication().getPackageName(), R.layout.notification_layout);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("név", "leírás", NotificationManager.IMPORTANCE_DEFAULT);
            Log.e("channel", "notifchannel");
            channel.setDescription("description");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setVibrationPattern(new long[]{0});
            channel.setSound(null, null);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
            notification.setChannelId(channel.getId());
        }
        notification.setContent(notificationView);
        notification.setSmallIcon(android.R.drawable.ic_media_pause);

        //Intent intent = new Intent(getApplication(), MainActivity.class);
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Intent openactivityIntent = new Intent(this, MainActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(this, 0, openactivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationView.setOnClickPendingIntent(R.id.notification_leftmost_icon, openPendingIntent);

        Intent pauseIntent = new Intent(this, NotificationIntentService.class);
        pauseIntent.setAction(getString(R.string.NOTIFICATION_ACTION_PAUSEPLAY));
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notification_pause, pausePendingIntent);

        Intent rewindIntent = new Intent(this, NotificationIntentService.class);
        rewindIntent.setAction(getString(R.string.NOTIFICATION_ACTION_REWIND));
        PendingIntent rewindPendingIntent = PendingIntent.getService(this, 0, rewindIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notification_rewind, rewindPendingIntent);

        Intent ffIntent = new Intent(this, NotificationIntentService.class);
        ffIntent.setAction(getString(R.string.NOTIFICATION_ACTION_FF));
        PendingIntent ffPendingIntent = PendingIntent.getService(this, 0, ffIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notif_ff, ffPendingIntent);

        Intent exitIntent = new Intent(this, NotificationIntentService.class);
        exitIntent.setAction(getString(R.string.NOTIFICATION_ACTION_QUIT));
        //PendingIntent exitPendingIntent = PendingIntent.getActivity(this, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //notificationView.setOnClickPendingIntent(R.id.notif_exit, exitPendingIntent);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0, exitIntent, 0);
        notificationView.setOnClickPendingIntent(R.id.notif_exit, exitPendingIntent);

        startForeground(NOTIFICATION_ID, notification.build());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
        wakeLock.acquire();

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        transferPlaybackState(false);
                    }
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);

        telephonyStateManager = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL) || intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)
                        || intent.getAction().equals(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)) {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        transferPlaybackState(false);
                    }
                }
            }
        };

        IntentFilter telephonyStateIntents = new IntentFilter();
        telephonyStateIntents.addAction(Intent.ACTION_HEADSET_PLUG);
        telephonyStateIntents.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        telephonyStateIntents.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(telephonyStateManager, telephonyStateIntents);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(getString(R.string.NORMAL_START_INTENT_ACTION))) {
            filesToPlay = intent.getStringArrayListExtra(getString(R.string.FILES_TO_PLAY));
            for (String str : intent.getStringArrayListExtra(getString(R.string.FILES_TO_PLAY))) {
                filesToPlay2.add(new MediaFile(str, true));
            }
            currentFilePosition = 0;
            playMusic();
        }
        if (notification != null) {
            startForeground(NOTIFICATION_ID, notification.build());
        }
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_PAUSEPLAY))) {
                this.playPausePlayback();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_FF))) {
                this.fastForwardSong();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_REWIND))) {
                this.rewindSong();
            } else if (intent.getAction().equals(getString(R.string.NOTIFICATION_ACTION_QUIT))) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        notificationManager.cancelAll();
        if (handler != null) {
            handler.removeCallbacks(updateInfoRunnable);
        }
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        mediaPlayer = null;
        unregisterReceiver(telephonyStateManager);
    }

    private void playMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        if (listener != null) {
            listener.onCurrentFilePositionChanged(currentFilePosition);
        }
        notification.setSmallIcon(android.R.drawable.ic_media_play);
        notificationView.setTextViewText(R.id.notification_text, FileItem.getFilenameFromPath(filesToPlay.get(currentFilePosition)));
        //notificationView.setTextViewText(R.id.notification_text, FileItem.getFilenameFromPath(filesToPlay2.get(currentFilePosition).getPath()));
        notification.setContent(notificationView);
        notificationManager.notify(NOTIFICATION_ID, notification.build());
        mediaPlayer = null;
        mediaPlayer = MediaPlayer.create(this, Uri.parse(filesToPlay.get(currentFilePosition)));
        //mediaPlayer = MediaPlayer.create(this, Uri.parse(filesToPlay2.get(currentFilePosition).getPath()));
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.start();
        transferPlaybackState(true);
        String filename = filesToPlay.get(currentFilePosition).substring(filesToPlay.get(currentFilePosition).lastIndexOf("/") + 1);
        //String filename = filesToPlay2.get(currentFilePosition).getPath().substring(filesToPlay2.get(currentFilePosition).getPath().lastIndexOf("/") + 1);
        Toast.makeText(this, filename, Toast.LENGTH_SHORT).show();
        handler = new Handler();
        handler.post(updateInfoRunnable);
    }

    private void transferPlaybackState(boolean isPlaying) {
        if (listener != null) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    public void playPausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            handler.removeCallbacks(updateInfoRunnable);
            transferPlaybackState(false);
            notification.setSmallIcon(android.R.drawable.ic_media_pause);
            notificationView.setImageViewResource(R.id.notification_pause, android.R.drawable.ic_media_play);
            notificationManager.notify(NOTIFICATION_ID, notification.build());
        } else if (mediaPlayer != null) {
            mediaPlayer.start();
            handler.post(updateInfoRunnable);
            transferPlaybackState(true);
            notificationView.setImageViewResource(R.id.notification_pause, android.R.drawable.ic_media_pause);
            notificationManager.notify(NOTIFICATION_ID, notification.build());
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
            jumpToSong(sharedPreferences.getInt(getString(R.string.SHARED_LAST_SONGS_INDEX), 0));
            startService(new Intent(this, PlaybackService.class));
            changeRepeatState(!sharedPreferences.getBoolean(getString(R.string.SHARED_IS_ONE_SONG_REPEATING), false));
        }
    }

    public void rewindSong() {
        if (filesToPlay != null) {
            //if (filesToPlay2!=null && filesToPlay2.size()>0)
            currentFilePosition--;
            if (currentFilePosition < 0) {
                currentFilePosition = filesToPlay.size() - 1;
                //currentFilePosition = filesToPlay2.size() - 1;
            }
            playMusic();
        }
    }

    public void fastForwardSong() {
        if (filesToPlay != null) {
            //if (filesToPlay2!=null && filesToPlay2.size()>0)
            currentFilePosition++;
            if (currentFilePosition > filesToPlay.size() - 1) {
                //if (currentFilePosition > filesToPlay2.size() - 1) {
                currentFilePosition = 0;
            }
            playMusic();
        }
    }

    public void seekSong(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position * mediaPlayer.getDuration() / 100);
        }
    }

    public void jumpToSong(int index) {
        if (filesToPlay == null) {
            //if (filesToPlay2==null || filesToPlay2.size()==0)
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.SHARED_MAINKEY), 0);
            filesToPlay = new ArrayList<>(Arrays.asList(sharedPreferences.getString(getString(R.string.SHARED_LAST_SONGS_ARRAY), "").split(getString(R.string.DIVIDER))));
            for (String str : sharedPreferences.getString(getString(R.string.SHARED_LAST_SONGS_ARRAY), "").split(getString(R.string.DIVIDER))) {
                filesToPlay2.add(new MediaFile(str, true));
            }
        }
        currentFilePosition = index;
        playMusic();
    }

    public void changeRepeatState(boolean newState) {
        repeatAll = newState;
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    public void changeFilesToPlay(int index, boolean newValue) {
        if (!newValue && filesToPlay != null && filesToPlay.size() > 1) {
            filesToPlay.remove(index);
        }
      /*  if (filesToPlay2 != null && filesToPlay2.size() > 1) {
            if (filesToPlay2.get(index).toBePlayed) {
                filesToPlay2.set(index, new MediaFile(filesToPlay2.get(index).getPath(), false));
            } else {
                filesToPlay2.set(index, new MediaFile(filesToPlay2.get(index).getPath(), true));
            }
        }*/
    }

    public void changesPlayListByCheckbox(ArrayList<FileItem> items) {
        String currentSongToSave = filesToPlay.get(currentFilePosition);
        filesToPlay.clear();
        for (FileItem item : items) {
            if (item.isChecked()) {
                filesToPlay.add(item.getPath());
            }
        }
        if (filesToPlay.size() == 0) {
            filesToPlay.add(currentSongToSave);
        }

    }

    private Runnable updateInfoRunnable = new Runnable() {
        @Override
        public void run() {
            if (listener != null && mediaPlayer != null) {
                listener.onSeekUpdate(filesToPlay.get(currentFilePosition), mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
            }
            handler.postDelayed(updateInfoRunnable, 1000);
        }
    };

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        handler.removeCallbacks(updateInfoRunnable);
        if (repeatAll) {
            currentFilePosition++;
            if (currentFilePosition > filesToPlay.size() - 1) {
                currentFilePosition = 0;
            }
        }
        playMusic();
    }
}
