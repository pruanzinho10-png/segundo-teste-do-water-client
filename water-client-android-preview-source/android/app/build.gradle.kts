plugins {
    id("com.android.application")
}

android {
    namespace = "dev.waterclient.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.waterclient.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0-feature"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".feature"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
