package com.olunaa.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class OlunaaMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL = "olunaa_messages";

    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        SharedPreferences prefs = getSharedPreferences("olunaa_session", MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();
        String cookies = prefs.getString("cookies", "");
        if (cookies.isEmpty()) return;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL("https://olunaa.vercel.app/api/devices/token").openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Cookie", cookies);
                byte[] body = ("{\"token\":\"" + token + "\",\"platform\":\"android\"}").getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(body); }
                connection.getResponseCode();
            } catch (Exception ignored) {
            } finally { if (connection != null) connection.disconnect(); }
        }).start();
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String title = message.getNotification() != null && message.getNotification().getTitle() != null
                ? message.getNotification().getTitle() : "OLUNAA";
        String body = message.getNotification() != null && message.getNotification().getBody() != null
                ? message.getNotification().getBody() : "You have a new notification";
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "OLUNAA messages", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("New messages and incoming calls");
            manager.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 7001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_olunaa)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setPriority(Notification.PRIORITY_HIGH);
        manager.notify((int) (System.currentTimeMillis() & 0x7fffffff), builder.build());
    }
}
