plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinComposePlugin)
    id("com.google.gms.google-services")
}

kotlin {
    androidTarget()
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
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
        }
        
        val wasmJsMain by getting {
            dependencies {
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
