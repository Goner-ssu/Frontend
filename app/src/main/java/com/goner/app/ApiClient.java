package com.goner.app;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    public static final String CHECK_FRIEND_CODE_URL = "http://localhost:8081";
    public static final String SAVE_PROFILE_URL = "http://localhost:8082";
    public static final String COMPLETE_ONBOARDING_URL = "http://localhost:8083";

    public interface Callback {
        void onResult(int statusCode, String body);
        void onError(Exception e);
    }

    public static void request(String method, String url, String body, Callback callback) {
        FirebaseUser auth = FirebaseAuth.getInstance().getCurrentUser();
        if(auth == null) {
            callback.onError(new IllegalStateException("로그인 필요"));
            return;
        }

        auth.getIdToken(false).addOnSuccessListener(result -> {
                    String token = result.getToken();
                    new Thread(() -> call(method, url, body, token, callback)).start();
                })
            .addOnFailureListener(e -> callback.onError(e));
        }

        private static void call(String method, String url, String body, String token, Callback callback) {
            Handler main = new Handler(Looper.getMainLooper());
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Authorization", "Bearer " + token);

                if (body != null) {
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }

                int status = conn.getResponseCode();
                InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
                String responseBody = readAll(stream);
                main.post(() -> callback.onResult(status, responseBody));

            } catch (Exception e) {
                main.post(() -> callback.onError(e));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}