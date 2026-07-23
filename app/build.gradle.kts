plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinComposePlugin)
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.1.0"
}

kotlin {
    androidTarget()
    
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            
            // Firebase Multiplatform
            implementation("dev.gitlive:firebase-firestore:2.5.0")
            implementation("dev.gitlive:firebase-database:2.5.0")
            implementation("dev.gitlive:firebase-storage:2.5.0")
            implementation("dev.gitlive:firebase-auth:2.5.0")
            
            // Coil 3
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")
        }
        
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation("androidx.navigation:navigation-compose:2.8.0")
                implementation("io.coil-kt:coil-compose:2.6.0")
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("androidx.compose.material:material-icons-extended:1.6.0")
                implementation("com.google.firebase:firebase-messaging:23.4.0")
                implementation("com.google.firebase:firebase-storage-ktx:21.0.1")
                implementation("com.google.firebase:firebase-database-ktx:21.0.0")
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.cloudinary:cloudinary-android:3.1.2")
                implementation(libs.coil.gif)
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.androidx.media3.ui)
                implementation(libs.androidx.viewpager2)
                implementation(libs.google.material)
                implementation(libs.androidx.recyclerview)
                implementation(libs.androidx.appcompat)

                val cameraxVersion = "1.3.1"
                implementation("androidx.camera:camera-core:$cameraxVersion")
                implementation("androidx.camera:camera-camera2:$cameraxVersion")
                implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
                implementation("androidx.camera:camera-view:$cameraxVersion")
                implementation("androidx.camera:camera-video:$cameraxVersion")
                implementation("com.google.accompanist:accompanist-permissions:0.37.3")
                implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")
            }
        }
    }
}

// Новый формат для AGP 9.2+
extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.RIKAPLAY.zhirpem_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.RIKAPLAY.zhirpem_app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.5.2"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
