package com.dawillygene.venlitgenexombiev2;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dawillygene.venlitgenexombiev2.Poem.PomeMainActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private SessionManager sessionManager;
    private Button btnAddPoem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize session manager
        sessionManager = new SessionManager(this);
        
        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        
        setContentView(R.layout.activity_main);

        // Initialize views
        btnAddPoem = findViewById(R.id.btn_add_poem);
        
        // Set up click listeners
        btnAddPoem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPoemActivity();
            }
        });

        Intent intent = new Intent(this, SmsReaderService.class);
        startService(intent);

        if (checkAndRequestPermissions()) {
            scheduleSmsReader();
        }
    }
    
    private void openPoemActivity() {
        Intent intent = new Intent(MainActivity.this, PomeMainActivity.class);
        startActivity(intent);
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void logout() {
        sessionManager.logoutUser();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private boolean checkAndRequestPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        // Check SCHEDULE_EXACT_ALARM permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SCHEDULE_EXACT_ALARM},
                        PERMISSION_REQUEST_CODE + 1);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleSmsReader();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE + 1) {
            // Handle SCHEDULE_EXACT_ALARM permission result
        }
    }

    private void scheduleSmsReader() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, SmsReaderService.class);
        PendingIntent pendingIntent = PendingIntent.getService(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set interval to 2 minutes as requested by user
        long interval = 2 * 60 * 1000; // 2 minutes
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent);
    }


    public void openInChrome(View view) {
        String androidPageUrl = "https://developer.android.com/";
        Uri webpage = Uri.parse(androidPageUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No browser available", Toast.LENGTH_SHORT).show();
        }
    }


}