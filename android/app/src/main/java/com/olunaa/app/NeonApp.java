package com.olunaa.app;

import android.app.*;
import android.content.SharedPreferences;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class NeonApp extends Application {
    private static final int BG = Color.rgb(7, 13, 28);
    private static final int FIELD = Color.rgb(27, 41, 70);
    private static final int WHITE = Color.rgb(242, 246, 255);
    private static final int MUTED = Color.rgb(145, 161, 190);
    private static final int PURPLE = Color.rgb(67, 139, 255);
    private static final int MAGENTA = Color.rgb(42, 91, 232);
    private static final int CYAN = Color.rgb(80, 190, 255);
    private SharedPreferences.OnSharedPreferenceChangeListener tokenListener;

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences session = getSharedPreferences("olunaa_session", MODE_PRIVATE);
        tokenListener = (preferences, key) -> {
            if ("cookies".equals(key)) OlunaaMessagingService.syncToken(this);
        };
        session.registerOnSharedPreferenceChangeListener(tokenListener);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            public void onActivityCreated(Activity activity, Bundle state) {
                style(activity);
                new Handler(Looper.getMainLooper()).postDelayed(() -> style(activity), 180);
            }
            public void onActivityResumed(Activity activity) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> style(activity), 80);
                OlunaaMessagingService.syncToken(activity);
            }
            public void onActivityStarted(Activity activity) {}
            public void onActivityPaused(Activity activity) {}
            public void onActivityStopped(Activity activity) {}
            public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            public void onActivityDestroyed(Activity activity) {}
        });
    }

    private GradientDrawable fieldBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{FIELD, Color.rgb(13, 22, 43)});
        drawable.setCornerRadius(24);
        drawable.setStroke(1, Color.rgb(55, 100, 170));
        return drawable;
    }

    private void style(Activity activity) {
        try {
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);
            walk(activity.getWindow().getDecorView());
        } catch (Exception ignored) { }
    }

    private void walk(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) walk(group.getChildAt(i));
        }
        if (view instanceof EditText) {
            EditText edit = (EditText) view;
            edit.setTextColor(WHITE);
            edit.setHintTextColor(MUTED);
            edit.setBackground(fieldBackground());
        } else if (view instanceof Button) {
            Button button = (Button) view;
            button.setTextColor(Color.WHITE);
            button.setAllCaps(false);
            GradientDrawable background = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, new int[]{PURPLE, MAGENTA, CYAN});
            background.setCornerRadius(24);
            background.setStroke(1, Color.rgb(73, 61, 160));
            button.setBackground(background);
        }
    }
}
