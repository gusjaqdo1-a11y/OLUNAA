plugins {
    id("com.android.application")
}

android {
    namespace = "com.olunaa.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.olunaa.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 30
        versionName = "1.3.0"
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
}
