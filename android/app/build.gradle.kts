import java.security.SecureRandom

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
        versionCode = 36
        versionName = "1.3.6"
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

val mediaPack = file("$projectDir/src/main/assets/olunaa_media_pack.bin")
val generateMediaPack = tasks.register("generateMediaPack") {
    outputs.file(mediaPack)
    doLast {
        val targetBytes = 48L * 1024L * 1024L
        if (!mediaPack.exists() || mediaPack.length() != targetBytes) {
            mediaPack.parentFile.mkdirs()
            val random = SecureRandom()
            val buffer = ByteArray(1024 * 1024)
            var remaining = targetBytes
            mediaPack.outputStream().use { out ->
                while (remaining > 0) {
                    random.nextBytes(buffer)
                    val count = minOf(buffer.size.toLong(), remaining).toInt()
                    out.write(buffer, 0, count)
                    remaining -= count
                }
            }
        }
    }
}

tasks.named("preBuild") { dependsOn(generateMediaPack) }
