package com.olunaa.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OlunaaMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "olunaa_messages";
    private static final String TOKEN_URL = "https://olunaa.vercel.app/api/devices/token";
    private static final long TOKEN_SYNC_INTERVAL_MS = 6L * 60L * 60L * 1000L;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        SharedPreferences prefs = getSharedPreferences("olunaa_session", MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();
        registerToken(getApplicationContext(), token);
    }

    /** Called when a session is created/restored, so tokens are registered even
     *  when Firebase generated the token before the user signed in. */
    public static void syncToken(Context context) {
        Context app = context.getApplicationContext();
        if (FirebaseApp.getApps(app).isEmpty()) return;
        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> registerToken(app, token))
                    .addOnFailureListener(error -> android.util.Log.w("OLUNAA-FCM", "Could not obtain FCM token", error));
        } catch (IllegalStateException error) {
            android.util.Log.w("OLUNAA-FCM", "Firebase is not initialized", error);
        }
    }

    private static void registerToken(Context context, String token) {
        if (token == null || token.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences("olunaa_session", Context.MODE_PRIVATE);
        String cookies = prefs.getString("cookies", "");
        if (cookies.isEmpty()) return;

        String lastToken = prefs.getString("fcm_registered_token", "");
        long lastSync = prefs.getLong("fcm_registered_at", 0L);
        if (token.equals(lastToken) && System.currentTimeMillis() - lastSync < TOKEN_SYNC_INTERVAL_MS) return;

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject payload = new JSONObject();
                payload.put("token", token);
                payload.put("platform", "android");
                connection = (HttpURLConnection) new URL(TOKEN_URL).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cookie", cookies);
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }
                int status = connection.getResponseCode();
                if (status >= 200 && status < 300) {
                    prefs.edit()
                            .putString("fcm_registered_token", token)
                            .putLong("fcm_registered_at", System.currentTimeMillis())
                            .apply();
                } else {
                    android.util.Log.w("OLUNAA-FCM", "Token registration returned HTTP " + status);
                }
            } catch (Exception error) {
                android.util.Log.w("OLUNAA-FCM", "Token registration failed", error);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "olunaa-fcm-register").start();
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : "OLUNAA";
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : "You have a new notification";
        showNotification(title, body, message.getData());
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "OLUNAA messages", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("New messages and incoming calls");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        PendingIntent pending = PendingIntent.getActivity(this, 7001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_olunaa)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_MESSAGE);
        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }
}
