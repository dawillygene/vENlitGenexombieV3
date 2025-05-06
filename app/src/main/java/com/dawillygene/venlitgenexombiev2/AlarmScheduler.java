package com.dawillygene.venlitgenexombiev2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmScheduler {
    private static final long NINE_HOURS_MS = 9 * 60 * 60 * 1000;
    private static final long ONE_DAY_MS = 24 * 60 * 60 * 1000;

    public static void scheduleDaily(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SmsReaderService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Cancel any existing alarms
        alarmManager.cancel(pendingIntent);

        // Calculate trigger time - 9 hours from now
        long triggerTime = System.currentTimeMillis() + NINE_HOURS_MS;

        // Schedule the alarm with appropriate method based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        // Store the next trigger time
        SharedPreferences prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("next_trigger_time", triggerTime).apply();

        // Create a repeating alarm for the following days
        PendingIntent dailyPendingIntent = PendingIntent.getService(
                context,
                1, // Different request code for the daily alarm
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // For the daily repeating alarm, use setRepeating which is less precise but more battery-efficient
        long dailyStartTime = triggerTime + ONE_DAY_MS; // Start daily cycle after the first 9-hour delay
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, dailyStartTime, ONE_DAY_MS, dailyPendingIntent);

        Toast.makeText(context, "SMS service scheduled to run after 9 hours and then daily", Toast.LENGTH_SHORT).show();
    }

    public static void rescheduleAfterReboot(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);
        long storedTriggerTime = prefs.getLong("next_trigger_time", 0);

        long currentTime = System.currentTimeMillis();

        // If stored time is in the past or not set, calculate a new time
        if (storedTriggerTime <= currentTime || storedTriggerTime == 0) {
            // Set to 9 hours from now
            scheduleDaily(context);
        } else {
            // We can still use the stored trigger time
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, SmsReaderService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule with the stored time
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, storedTriggerTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, storedTriggerTime, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, storedTriggerTime, pendingIntent);
            }

            // Also set up the daily repeating alarm
            PendingIntent dailyPendingIntent = PendingIntent.getService(
                    context,
                    1,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long dailyStartTime = storedTriggerTime + ONE_DAY_MS;
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, dailyStartTime, ONE_DAY_MS, dailyPendingIntent);
        }
    }
}
