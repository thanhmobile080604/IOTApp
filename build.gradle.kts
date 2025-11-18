plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false

}

buildscript {

    extra["minSdkVersion"] = 24
    extra["compileSdkVersion"] = 35
    extra["targetSdkVersion"] = 35

    repositories {
        mavenCentral()
        google()
    }
}