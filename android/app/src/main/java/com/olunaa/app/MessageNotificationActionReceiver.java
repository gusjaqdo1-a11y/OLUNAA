package com.olunaa.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.app.RemoteInput;
import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MessageNotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_REPLY = "com.olunaa.app.NOTIFICATION_REPLY";
    public static final String ACTION_MARK_READ = "com.olunaa.app.NOTIFICATION_MARK_READ";
    public static final String KEY_TEXT_REPLY = "olunaa_reply_text";
    private static final String API = "https://olunaa.vercel.app/api";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String conversationId = intent.getStringExtra("conversationId");
        if (conversationId == null || conversationId.trim().isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences("olunaa_session", Context.MODE_PRIVATE);
        String cookies = prefs.getString("cookies", "");
        if (cookies.isEmpty()) return;
        final BroadcastReceiver.PendingResult pending = goAsync();
        if (ACTION_REPLY.equals(intent.getAction())) {
            android.os.Bundle results = RemoteInput.getResultsFromIntent(intent);
            CharSequence reply = results == null ? null : results.getCharSequence(KEY_TEXT_REPLY);
            if (reply == null || reply.toString().trim().isEmpty()) { pending.finish(); return; }
            String text = reply.toString().trim();
            if (text.length() > 12000) { pending.finish(); return; }
            new Thread(() -> { try { postMessage(context, conversationId, text, cookies); }
                finally { pending.finish(); } }, "olunaa-notification-reply").start();
        } else if (ACTION_MARK_READ.equals(intent.getAction())) {
            new Thread(() -> { try { markConversationRead(context, conversationId, cookies); }
                finally { pending.finish(); } }, "olunaa-notification-read").start();
        } else pending.finish();
    }

    private void postMessage(Context context, String conversationId, String text, String cookies) {
        HttpURLConnection connection = null;
        try {
            JSONObject payload = new JSONObject();
            payload.put("conversationId", conversationId);
            payload.put("body", text);
            connection = (HttpURLConnection) new URL(API + "/messages").openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cookie", cookies);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                Log.w("OLUNAA-NOTIF", "Inline reply failed with HTTP " + status);
            } else {
                android.app.NotificationManager manager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(conversationId.hashCode());
            }
        } catch (Exception error) {
            Log.w("OLUNAA-NOTIF", "Could not send inline reply", error);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private void markConversationRead(Context context, String conversationId, String cookies) {
        HttpURLConnection connection = null;
        try {
            String encoded = java.net.URLEncoder.encode(conversationId, "UTF-8");
            connection = (HttpURLConnection) new URL(API + "/messages?conversationId=" + encoded).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Cookie", cookies);
            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) {
                android.app.NotificationManager manager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (manager != null) manager.cancel(conversationId.hashCode());
            } else {
                Log.w("OLUNAA-NOTIF", "Mark read failed with HTTP " + status);
            }
        } catch (Exception error) {
            Log.w("OLUNAA-NOTIF", "Could not mark conversation read", error);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
