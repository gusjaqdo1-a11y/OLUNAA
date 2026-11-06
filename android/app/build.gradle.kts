plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    lint {
        disable += "NullSafeMutableLiveData"
    }
    namespace = "com.olunaa.app"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    defaultConfig {
        applicationId = "com.olunaa.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 43
        versionName = "1.4.3"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.biometric:biometric:1.4.0-alpha04")
    implementation("io.livekit:livekit-android:2.30.0")
    implementation("io.ably:ably-java:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-messaging")
}

tasks.register("normalizeInsetsSource") {
    doLast {
        val source = file("$projectDir/src/main/java/com/olunaa/app/MainActivity.java")
        if (source.exists()) {
            val original = source.readText()
            val fixed = original
                .replace("android.graphics.Insets x=", "androidx.core.graphics.Insets x=")
                .replace("WindowCompat.enableEdgeToEdge(getWindow());", "getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);")
            if (fixed != original) source.writeText(fixed)
        }
    }
}

tasks.named("preBuild") { dependsOn("normalizeInsetsSource") }

// Do not generate dummy media files during builds. The previous 48 MB random
// asset inflated every APK without providing any product functionality.

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
