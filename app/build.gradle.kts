plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // AndroidUSBCamera (AUSBC) - USB camera library with native libuvc support
    val uvcCameraVersion = "3.2.7"
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc:$uvcCameraVersion")
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libuvc:$uvcCameraVersion")
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libnative:$uvcCameraVersion")
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libuvccommon:$uvcCameraVersion")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}