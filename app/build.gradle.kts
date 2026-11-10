plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.neokeys.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neokeys.keyboard"
        minSdk = 23
        // Avoid forced edge-to-edge layout for this IME on Android 15.
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
