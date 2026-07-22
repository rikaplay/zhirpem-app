import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 1. Определение среды
val isVercel = System.getenv("VERCEL") == "1"

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Применяем Google Services только если мы НЕ на Vercel
if (!isVercel) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "zhirpem-web.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.materialIconsExtended)
                implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("com.google.firebase:firebase-messaging:23.4.0")
                implementation("com.google.firebase:firebase-storage-ktx:21.0.1")
                implementation("com.google.firebase:firebase-database-ktx:21.0.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")
            }
        }
    }
}

// 2. Глобальный «обман» для Android SDK на Vercel
if (isVercel) {
    val dummySdk = file("${project.layout.buildDirectory.get().asFile}/dummy-sdk")
    dummySdk.mkdirs()
    System.setProperty("android.home", dummySdk.absolutePath)
}

android {
    namespace = "com.RIKAPLAY.zhirpem_app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.RIKAPLAY.zhirpem_app"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.5.2"
    }

    if (isVercel) {
        buildFeatures {
            compose = true
            viewBinding = false
            dataBinding = false
        }
    } else {
        buildFeatures {
            compose = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
