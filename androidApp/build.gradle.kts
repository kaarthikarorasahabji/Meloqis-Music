import java.util.Properties


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// The separately installed preview uses the existing Firebase project configuration.
androidComponents.onVariants(androidComponents.selector().withBuildType("performance")) {
    tasks.named<com.google.gms.googleservices.GoogleServicesTask>("processPerformanceGoogleServices") {
        applicationId.set("echo.music.iad1tya")
    }
}

android {
    val abis = arrayOf("armeabi-v7a", "arm64-v8a", "x86_64")
    val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
    val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
    val releaseStorePassword = System.getenv("RELEASE_STORE_PASSWORD")
    val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning =
        !releaseKeystorePath.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()

    namespace = "echo.music.iad1tya"
    compileSdk = 37

    defaultConfig {
        applicationId = "echo.music.iad1tya"
        minSdk = 26
        targetSdk = 36
        versionCode =
            libs.versions.version.code
                .get()
                .toInt()
        versionName =
            libs.versions.version.name
                .get()
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters +=
                listOf(
                    "en",
                    "vi",
                    "it",
                    "de",
                    "ru",
                    "tr",
                    "fi",
                    "pl",
                    "pt",
                    "fr",
                    "es",
                    "zh-rCN",
                    "id",
                    "in",
                    "ar",
                    "ja",
                    "zh-rTW",
                    "uk",
                    "iw",
                    "az",
                    "hi",
                    "th",
                    "nl",
                    "ko",
                    "ca",
                    "fa",
                    "bg",
                )
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("production") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("production")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            splits {
                abi {
                    isEnable = true
                    reset()
                    isUniversalApk = true
                    include(*abis)
                }
            }
        }
        debug {
            isMinifyEnabled = false
            // applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("performance") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".performance"
            versionNameSuffix = "-performance"
            matchingFallbacks += listOf("release")
            // A separate install keeps the user's existing library and login intact.
            resValue("string", "app_name", "Meloqis Preview")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // enable view binding
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes +=
            listOf(
                "META-INF/META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/asm-license.txt",
                "META-INF/notice",
                "META-INF/*.kotlin_module",
            )
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Local previews do not need to publish a development mapping to Crashlytics.
tasks.matching { it.name == "uploadCrashlyticsMappingFilePerformance" }.configureEach {
    enabled = false
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    val debugImplementation = "debugImplementation"
    debugImplementation(libs.ui.tooling)
    implementation(libs.activity.compose)

    // Custom Activity On Crash
    implementation(libs.customactivityoncrash)

    // Easy Permissions
    implementation(libs.easypermissions)

    // Legacy Support
    implementation(libs.legacy.support.v4)
    // Coroutines
    implementation(libs.coroutines.android)



    implementation(projects.composeApp)
    implementation(projects.data)
    
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation(projects.crashlytics)
}
