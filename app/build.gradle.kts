plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    namespace = "com.invdiv.voidclient"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.invdiv.voidclient"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig  = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.network)
    implementation(libs.ktor.network.tls)
    implementation(libs.ktor.client.core)
    implementation(libs.lz4.java)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jackson.dataformat.msgpack)
    implementation(libs.jackson.databind)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.datetime)
    implementation(libs.haze)
    implementation("dev.chrisbanes.haze:haze-blur:2.0.0-alpha01")
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.lottie.compose)
    implementation("io.coil-kt.coil3:coil-video:3.4.0")
    implementation("net.engawapg.lib:zoomable:2.11.0")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.11.0")
    implementation("com.squareup.zstd:zstd-kmp-okio:0.4.0")
    implementation("com.squareup.okio:okio:3.15.0")
}