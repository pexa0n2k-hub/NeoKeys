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
        targetSdk = 34
        versionCode = 12
        versionName = "2.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
