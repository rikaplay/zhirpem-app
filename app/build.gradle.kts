plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinComposePlugin)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.RIKAPLAY.zhirpem_app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.RIKAPLAY.zhirpem_app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        externalNativeBuild {
            ndkBuild {
                arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
            cmake {
                arguments("-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
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
    // Обязательно для съемки видео:
    implementation("androidx.camera:camera-video:$cameraxVersion")

    // Библиотека от Google для удобного запроса разрешений в Compose
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // OneSignal Push Notifications
    implementation("com.onesignal:OneSignal:[5.0.0, 5.99.99]")
}
