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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SmsReaderService", "Reading SMS from inbox");

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        SmsReader.readSms(this); // Read SMS from the inbox

        // Stop the service when done
        stopForeground(true);
        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "SMS Reader Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Reader")
                .setContentText("Reading SMS messages...")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }
}