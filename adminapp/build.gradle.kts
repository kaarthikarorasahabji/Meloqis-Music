plugins {
    id("com.android.application")
}

android {
    namespace = "in.axenoraai.meloqis.admin"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.axenoraai.meloqis.admin"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.8"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("app/keystore/release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
