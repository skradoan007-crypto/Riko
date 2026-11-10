plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.rikodev.music.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rikodev.music.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // No "-debug" in versionName — the shipped APK must not read as a
            // debug build (user ask; filename handled in CI rename step).
            isDebuggable = true
            // A totally unshrunk debug build (Media3 + Room + Hilt + Compose +
            // Coil) lands around 21MB — normal for debug, but there's no
            // reason a test APK needs to carry that. Minifying + shrinking
            // resources here still keeps every one of our own classes intact
            // (see proguard-rules.pro's broad keep rule below) so this is
            // low-risk: only third-party dependency dead-code and unused
            // resources actually get stripped.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Debug-sign release so macrobenchmark can install it. Production
            // CI signs with the real keystore (separate step). Without this,
            // benchmark runs fail with INSTALL_PARSE_FAILED_NO_CERTIFICATES
            // when AGP resolves the release variant instead of "benchmark".
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Wave 0 instrumentation: release-like (R8 + shrink) but debug-signed so
        // macrobenchmark can install it without a release keystore. Never shipped.
        // Run: ./gradlew :benchmark:connectedBenchmarkAndroidTest
        // Idempotent: the androidx.baselineprofile plugin may create this type
        // too depending on version — never clash.
        (findByName("benchmark") ?: create("benchmark")).apply {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.media3.common.util.UnstableApi"
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // DataStore
    implementation(libs.datastore.preferences)

    // Palette
    implementation(libs.palette)

    // Coil
    implementation(libs.coil.compose)




    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Baseline Profile (plan 1.6): profileinstaller ships the profile; the
    // baselineProfile configuration packages :benchmark's generated journeys.
    implementation(libs.androidx.profileinstaller)
    if (findProject(":benchmark") != null) baselineProfile(project(":benchmark"))

    // Note: Glass blur effect implemented via pure Compose (alpha overlays)

    // Test
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui)
    testImplementation(libs.androidx.ui.graphics)
    testImplementation(libs.androidx.ui.text)
    testImplementation(libs.androidx.ui.unit)
    testImplementation(libs.androidx.material3)
    testImplementation(libs.androidx.compose.foundation)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.test.manifest)
}
