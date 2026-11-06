package com.olunaa.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

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
    private static final String CALL_CHANNEL = "olunaa_incoming_calls";
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
        Map<String, String> data = message.getData();
        String title = data.getOrDefault("title",
                message.getNotification() != null && message.getNotification().getTitle() != null
                        ? message.getNotification().getTitle() : "OLUNAA");
        String body = data.getOrDefault("body",
                message.getNotification() != null && message.getNotification().getBody() != null
                        ? message.getNotification().getBody() : "You have a new notification");
        showNotification(title, body, data);
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        boolean incomingCall = data != null && "incoming_call".equals(data.get("type"));
        String selectedChannel = incomingCall ? CALL_CHANNEL : CHANNEL;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel messages = new NotificationChannel(
                    CHANNEL, "OLUNAA messages", NotificationManager.IMPORTANCE_HIGH);
            messages.setDescription("New messages and account activity");
            messages.enableVibration(true);
            manager.createNotificationChannel(messages);
            NotificationChannel calls = new NotificationChannel(
                    CALL_CHANNEL, "OLUNAA incoming calls", NotificationManager.IMPORTANCE_HIGH);
            calls.setDescription("Ringing alerts for incoming OLUNAA voice and video calls");
            calls.enableVibration(true);
            calls.setVibrationPattern(new long[]{0, 500, 250, 500, 250, 800});
            android.net.Uri ring = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);
            android.media.AudioAttributes audio = new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            calls.setSound(ring, audio);
            manager.createNotificationChannel(calls);
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        String conversationId = data == null ? "" : data.getOrDefault("conversationId", "");
        String notificationType = data == null ? "" : data.getOrDefault("type", "");
        int requestCode = (conversationId + ":" + notificationType).hashCode();
        PendingIntent pending = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, selectedChannel) : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_stat_olunaa)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(!incomingCall)
                .setContentIntent(pending)
                .setPriority(incomingCall ? Notification.PRIORITY_MAX : Notification.PRIORITY_HIGH)
                .setCategory(incomingCall ? Notification.CATEGORY_CALL : Notification.CATEGORY_MESSAGE);
        if (!incomingCall && data != null && "message".equals(data.get("type"))) {
            String conversationId = data.getOrDefault("conversationId", "");
            if (!conversationId.isEmpty()) {
                int actionId = conversationId.hashCode();
                Intent replyIntent = new Intent(this, MessageNotificationActionReceiver.class);
                replyIntent.setAction(MessageNotificationActionReceiver.ACTION_REPLY);
                replyIntent.putExtra("conversationId", conversationId);
                PendingIntent replyPending = PendingIntent.getBroadcast(this, actionId + 11, replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_MUTABLE : 0));
                RemoteInput remoteInput = new RemoteInput.Builder(MessageNotificationActionReceiver.KEY_TEXT_REPLY)
                        .setLabel("Write a message…").build();
                Notification.Action replyAction = new Notification.Action.Builder(
                        R.drawable.ic_stat_olunaa, "Reply", replyPending)
                        .addRemoteInput(remoteInput).setAllowGeneratedReplies(true)
                        .setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY).build();
                builder.addAction(replyAction);

                Intent readIntent = new Intent(this, MessageNotificationActionReceiver.class);
                readIntent.setAction(MessageNotificationActionReceiver.ACTION_MARK_READ);
                readIntent.putExtra("conversationId", conversationId);
                PendingIntent readPending = PendingIntent.getBroadcast(this, actionId + 12, readIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
                builder.addAction(R.drawable.ic_stat_olunaa, "Mark as read", readPending);
                builder.setGroup("olunaa-chat-" + conversationId);
            }
        }
        if (incomingCall) {
            Intent acceptIntent = new Intent(this, MainActivity.class);
            acceptIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            acceptIntent.putExtra("type", "incoming_call");
            acceptIntent.putExtra("call_action", "accept");
            for (String key : new String[]{"conversationId", "mode", "callerName"}) {
                String value = data.get(key);
                if (value != null) acceptIntent.putExtra(key, value);
            }
            PendingIntent accept = PendingIntent.getActivity(this, requestCode + 1, acceptIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            Intent declineIntent = new Intent(this, MainActivity.class);
            declineIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            declineIntent.putExtra("type", "incoming_call");
            declineIntent.putExtra("call_action", "decline");
            declineIntent.putExtra("conversationId", conversationId);
            PendingIntent decline = PendingIntent.getActivity(this, requestCode + 2, declineIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            builder.addAction(R.drawable.ic_stat_olunaa, "Answer", accept);
            builder.addAction(R.drawable.ic_stat_olunaa, "Decline", decline);
            if (Build.VERSION.SDK_INT >= 31) builder.setFullScreenIntent(pending, true);
        }
        Bitmap senderAvatar = loadSenderAvatar(data == null ? "" : data.getOrDefault("senderAvatarUrl", ""));
        if (senderAvatar != null) builder.setLargeIcon(senderAvatar);
        int notificationId = data != null && !data.getOrDefault("conversationId", "").isEmpty() && !incomingCall
                ? data.get("conversationId").hashCode() : (int) (System.currentTimeMillis() & 0x7fffffff);
        manager.notify(notificationId, builder.build());
    }

    /** Loads a small sender photo for the Android notification when the server can safely include it. */
    private Bitmap loadSenderAvatar(String source) {
        if (source == null || source.isEmpty()) return null;
        try {
            byte[] bytes;
            if (source.startsWith("data:image/")) {
                int comma = source.indexOf(',');
                if (comma < 0 || source.length() > 3000) return null;
                bytes = Base64.decode(source.substring(comma + 1), Base64.DEFAULT);
            } else if (source.startsWith("https://") || source.startsWith("http://")) {
                HttpURLConnection connection = (HttpURLConnection) new URL(source).openConnection();
                connection.setConnectTimeout(2500);
                connection.setReadTimeout(2500);
                connection.setInstanceFollowRedirects(true);
                try (java.io.InputStream in = connection.getInputStream();
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (out.size() + count > 1024 * 1024) return null;
                        out.write(buffer, 0, count);
                    }
                    bytes = out.toByteArray();
                } finally {
                    connection.disconnect();
                }
            } else {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 192);
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Exception error) {
            android.util.Log.w("OLUNAA-FCM", "Could not load sender notification avatar", error);
            return null;
        }
    }
}
