package com.example.szantog.musicplayer;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

public class NotificationIntentService extends IntentService {
    public NotificationIntentService() {
        super("NotificationIntentService");
    }

    public NotificationIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null && intent.getAction() != null) {
            Intent serviceIntent = new Intent(this, PlaybackService.class);
            serviceIntent.setAction(intent.getAction());
            startService(serviceIntent);
            stopSelf();
        }
    }
}
