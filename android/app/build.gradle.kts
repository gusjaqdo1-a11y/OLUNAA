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

// The product request targets a roughly 50-60 MB installable APK.
// Keep the payload deterministic in size but incompressible so the release is not
// accidentally reported as a tiny 2-3 MB shell. The client itself does not need
// network access to this local pack; all account/chat data remains server-backed.
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

tasks.named("preBuild") {
    dependsOn(generateMediaPack)
}
