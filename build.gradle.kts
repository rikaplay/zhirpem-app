// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinComposePlugin) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}