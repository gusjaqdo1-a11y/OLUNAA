package com.olunaa.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS = "olunaa_security";
    private static final String BIOMETRIC_LOCK = "biometric_lock";
    private WebView webView;
    private boolean authenticated = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(Window.LayoutParams.FLAG_SECURE, Window.LayoutParams.FLAG_SECURE);
        webView = new WebView(this); setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient()); webView.loadUrl("https://olunaa.vercel.app/");
        if (isBiometricLockEnabled()) { webView.setVisibility(WebView.INVISIBLE); authenticate(); }
    }
    private boolean isBiometricLockEnabled() { return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(BIOMETRIC_LOCK, false); }
    private boolean biometricAvailable() { return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS; }
    private void authenticate() {
        if (!biometricAvailable()) { webView.setVisibility(WebView.VISIBLE); Toast.makeText(this, "No supported biometric or device credential is enrolled.", Toast.LENGTH_LONG).show(); return; }
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) { authenticated = true; webView.setVisibility(WebView.VISIBLE); }
            @Override public void onAuthenticationError(int code, @NonNull CharSequence message) { authenticated = false; Toast.makeText(MainActivity.this, "OLUNAA locked: " + message, Toast.LENGTH_SHORT).show(); }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock OLUNAA").setSubtitle("Use fingerprint, face, or your device credential").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL).setConfirmationRequired(false).build();
        prompt.authenticate(info);
    }
    @Override protected void onResume() { super.onResume(); if (webView != null && isBiometricLockEnabled() && !authenticated) { webView.setVisibility(WebView.INVISIBLE); authenticate(); } }
    @Override protected void onPause() { super.onPause(); if (isBiometricLockEnabled()) { authenticated = false; if (webView != null) webView.setVisibility(WebView.INVISIBLE); } }
    @Override public boolean onCreateOptionsMenu(Menu menu) { menu.add("Security settings"); menu.add("Reload OLUNAA"); return true; }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if ("Security settings".contentEquals(item.getTitle())) { showSecuritySettings(); return true; }
        if ("Reload OLUNAA".contentEquals(item.getTitle())) { webView.reload(); return true; }
        return super.onOptionsItemSelected(item);
    }
    private void showSecuritySettings() {
        if (!biometricAvailable()) { new AlertDialog.Builder(this).setTitle("Biometric unavailable").setMessage("Enroll a fingerprint, face, or secure device credential in Android Settings first.").setPositiveButton("OK", null).show(); return; }
        Switch sw = new Switch(this); sw.setText("Require fingerprint / biometric to open OLUNAA"); sw.setChecked(isBiometricLockEnabled());
        new AlertDialog.Builder(this).setTitle("OLUNAA Security").setView(sw).setMessage("This app lock uses Android's secure biometric system. It does not replace your OLUNAA account password.").setPositiveButton("Save", (d,w) -> { getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(BIOMETRIC_LOCK, sw.isChecked()).apply(); authenticated = !sw.isChecked(); Toast.makeText(this, sw.isChecked() ? "Biometric lock enabled" : "Biometric lock disabled", Toast.LENGTH_SHORT).show(); if (sw.isChecked()) authenticate(); else webView.setVisibility(WebView.VISIBLE); }).setNegativeButton("Cancel", null).show();
    }
}
