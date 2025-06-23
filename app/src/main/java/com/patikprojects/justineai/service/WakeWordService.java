package com.patikprojects.justineai.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.patikprojects.justineai.R;

public class WakeWordService extends Service {
    private static final String TAG = "WakeWordService";
    private static final String CHANNEL_ID = "WakeWordServiceChannel";
    private final IBinder binder = new LocalBinder();

    public static class LocalBinder extends Binder {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Justine AI")
                .setContentText("Listening for 'Hey Justine'")
                .setSmallIcon(R.drawable.ic_mic)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();

        startForeground(1, notification);
        Log.i(TAG, "WakeWordService started in foreground");
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Justine Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
