plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.mahesh.facedetection"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mahesh.facedetection"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.material3.android)

    //Compose navigation
    implementation(libs.androidx.navigation.compose)

    // Jetpack Compose Dependencies
    implementation(libs.androidx.activity.compose) // For ComponentActivity.setContent
    implementation(libs.compose.material)         // Compose Material Design components (Buttons, Cards, etc.)
    implementation(libs.androidx.compose.ui)             // Core Compose UI elements
    debugImplementation(libs.androidx.compose.ui.tooling)    // Tools for previewing and inspecting Compose UIs
    implementation(libs.androidx.ui.tooling.preview.android) // For @Preview annotation
    implementation(libs.androidx.ui.graphics) // For @Preview annotation
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // CameraX Dependencies
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view) // For PreviewView if using CameraX with Views
    implementation(libs.camera.mlkit.vision) // For integrating CameraX with ML Kit Vision

    // ML Kit Dependencies
    implementation(libs.face.detection)
    implementation(libs.vision.common)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}