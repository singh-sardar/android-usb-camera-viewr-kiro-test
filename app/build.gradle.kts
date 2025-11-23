plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.usbcameraviewer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.usbcameraviewer"
        minSdk = 21 // Android 5.0 for maximum compatibility
        targetSdk = 33 // Target Android 13 for better MIUI compatibility
        versionCode = 3
        versionName = "1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Support for different screen sizes and densities
        vectorDrawables.useSupportLibrary = true
        
        // Only include ARM architectures to reduce APK size
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Disable for better compatibility
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // For production, add signing config here
            // signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    

    
    packaging {
        resources {
            excludes += setOf(
                "META-INF/native-image/**",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {

    // --- Core Android Dependencies ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // --- Jetpack Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.runtime:runtime-livedata")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- CameraX for camera support ---
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // FINAL FIX: Rolling back to stable version 3.2.7 and using the fully-qualified 
    // Group ID that JitPack requires for internal modules. This should bypass the build failure 
    // and the 401 transitive resolution error.
    val uvcCameraVersion = "3.2.7"
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc:$uvcCameraVersion") // Main module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libuvc:$uvcCameraVersion") // Transitive module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libnative:$uvcCameraVersion") // Transitive module
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libuvccommon:$uvcCameraVersion") // Transitive module


    // --- Testing Dependencies ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}