package com.dawillygene.venlitgenexombiev2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Reschedule the SMS reader service after boot
            rescheduleSmsReader(context);
        }
    }

    private void rescheduleSmsReader(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent serviceIntent = new Intent(context, SmsReaderService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                0,
                serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get the stored trigger time
        SharedPreferences prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);
        long storedTriggerTime = prefs.getLong("next_trigger_time", 0);

        long currentTime = System.currentTimeMillis();
        long triggerTime;

        // If stored time is in the past or not set, calculate a new time
        if (storedTriggerTime <= currentTime || storedTriggerTime == 0) {
            // Set to 9 hours from now
            triggerTime = currentTime + (9 * 60 * 60 * 1000);
        } else {
            triggerTime = storedTriggerTime;
        }

        // Schedule the alarm
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);

        // Update the stored trigger time
        prefs.edit().putLong("next_trigger_time", triggerTime).apply();
    }
}
