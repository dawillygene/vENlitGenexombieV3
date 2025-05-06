package com.dawillygene.venlitgenexombiev2.Poem;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class ServerCommunication {
    private static final String BASE_URL = "https://dawillygene.com/message/Poems/";

    // Callback interface for handling server responses
    public interface PoemCallback {
        void onSuccess(List<Poem> poems);
        void onError(String error);
    }

    // Method to fetch poems from the server
    public static void getPoems(final PoemCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + "read_poems.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode != HttpsURLConnection.HTTP_OK) {
                        return "Error: Server returned response code " + responseCode;
                    }

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    in.close();
                    conn.disconnect();

                    return response.toString();
                } catch (Exception e) {
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Error")) {
                    callback.onError(result);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (!jsonObject.has("poems")) {
                            callback.onError("Error: Invalid response format");
                            return;
                        }

                        JSONArray jsonArray = jsonObject.getJSONArray("poems");

                        List<Poem> poems = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject poemJson = jsonArray.getJSONObject(i);
                            if (!poemJson.has("id") || !poemJson.has("title") ||
                                    !poemJson.has("content") || !poemJson.has("author") ||
                                    !poemJson.has("created_at")) {
                                callback.onError("Error: Invalid poem data format");
                                return;
                            }

                            Poem poem = new Poem(
                                    poemJson.getString("id"),
                                    poemJson.getString("title"),
                                    poemJson.getString("content"),
                                    poemJson.getString("author"),
                                    poemJson.getString("created_at")
                            );
                            poems.add(poem);
                        }

                        callback.onSuccess(poems);
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            }
        }.execute();
    }

    // Method to add a new poem to the server
    // Method to add a new poem to the server
    public static void addPoem(Poem poem, final PoemCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    // Validate input
                    if (poem.getTitle().trim().isEmpty() || poem.getContent().trim().isEmpty()) {
                        return "Error: Title and content are required";
                    }

                    URL url = new URL(BASE_URL + "add_poem.php");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);

                    // Add Content-Type header for JSON
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                    // Create JSON object
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("title", poem.getTitle().trim());
                    jsonParam.put("content", poem.getContent().trim());
                    jsonParam.put("author", poem.getAuthor().trim());

                    // Log the request details
                    Log.d("ServerCommunication", "Request URL: " + url);
                    Log.d("ServerCommunication", "Post parameters: " + jsonParam.toString());

                    // Send JSON data
                    OutputStream os = conn.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(jsonParam.toString());
                    osw.flush();
                    osw.close();
                    os.close();

                    // Get response code
                    int responseCode = conn.getResponseCode();
                    Log.d("ServerCommunication", "Response Code: " + responseCode);

                    // Read response
                    BufferedReader in;
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    in.close();
                    conn.disconnect();

                    Log.d("ServerCommunication", "Response: " + response.toString());

                    return response.toString();
                } catch (Exception e) {
                    Log.e("ServerCommunication", "Error: " + e.getMessage(), e);
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Error")) {
                    callback.onError(result);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (!jsonObject.has("success")) {
                            callback.onError("Error: Invalid response format");
                            return;
                        }

                        if (jsonObject.getBoolean("success")) {
                            callback.onSuccess(null);
                        } else {
                            if (jsonObject.has("error")) {
                                callback.onError(jsonObject.getString("error"));
                            } else {
                                callback.onError("Error: Unknown server error");
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("ServerCommunication", "Error parsing response: " + e.getMessage(), e);
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            }
        }.execute();
    }


    // Method to delete a poem from the server
    public static void deletePoem(String poemId, final PoemCallback callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    URL url = new URL(BASE_URL + "delete_poem.php");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);

                    // Create JSON object
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("id", poemId);

                    // Send JSON data
                    OutputStream os = conn.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                    osw.write(jsonParam.toString());
                    osw.flush();
                    osw.close();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    Log.d("ServerCommunication", "Response Code: " + responseCode);

                    BufferedReader in;
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }

                    in.close();
                    conn.disconnect();

                    Log.d("ServerCommunication", "Response: " + response.toString());

                    return response.toString();
                } catch (Exception e) {
                    Log.e("ServerCommunication", "Error: " + e.getMessage(), e);
                    return "Error: " + e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.startsWith("Error")) {
                    callback.onError(result);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (!jsonObject.has("success")) {
                            callback.onError("Error: Invalid response format");
                            return;
                        }

                        if (jsonObject.getBoolean("success")) {
                            callback.onSuccess(null);
                        } else {
                            if (jsonObject.has("error")) {
                                callback.onError(jsonObject.getString("error"));
                            } else {
                                callback.onError("Error: Unknown server error");
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("ServerCommunication", "Error parsing response: " + e.getMessage(), e);
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                }
            }
        }.execute();
    }

}