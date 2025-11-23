# Build Guide

Complete guide to building USB Camera Viewer from source.

## Prerequisites

### Required Software
- **Android Studio**: Arctic Fox or newer
- **JDK**: 11 or higher
- **Gradle**: 8.0+ (included with project)
- **Git**: For cloning repository

### Required Hardware
- **Development machine**: Windows, macOS, or Linux
- **Android device**: For testing (Android 7.0+)
- **USB cable**: For ADB connection
- **USB camera**: For testing (UVC-compliant)

## Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd WebViewCamberViewr
```

### 2. Open in Android Studio
1. Launch Android Studio
2. File → Open
3. Select project folder
4. Wait for Gradle sync

### 3. Configure SDK
1. File → Project Structure
2. SDK Location → Android SDK
3. Ensure SDK 34 is installed
4. Click OK

## Building

### Debug Build
```bash
# Command line
./gradlew assembleDebug

# Output
app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Command line
./gradlew assembleRelease

# Output
app/build/outputs/apk/release/app-release-unsigned.apk
```

### Both Builds
```bash
./gradlew assemble
```

## Installation

### Install Debug
```bash
# Via Gradle
./gradlew installDebug

# Via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install Release
```bash
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Check Code Quality
```bash
# Lint check
./gradlew lint

# Detekt (if configured)
./gradlew detekt
```

## Signing

### Create Keystore
```bash
keytool -genkey -v -keystore release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key
```

### Configure Signing
Add to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release-key.jks")
            storePassword = "your_password"
            keyAlias = "release-key"
            keyPassword = "your_password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ...
        }
    }
}
```

### Build Signed APK
```bash
./gradlew assembleRelease
```

## Troubleshooting

### Gradle Sync Failed
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### SDK Not Found
1. File → Project Structure
2. SDK Location
3. Set Android SDK path
4. Click OK

### Build Failed
```bash
# Check Java version
java -version

# Should be 11 or higher
# Update if needed
```

### Out of Memory
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
```

## Build Variants

### Debug
- **Purpose**: Development and testing
- **Features**: Logging enabled, debuggable
- **Size**: Larger (13 MB)
- **Performance**: Slower

### Release
- **Purpose**: Production deployment
- **Features**: ProGuard enabled, optimized
- **Size**: Smaller (2.5 MB)
- **Performance**: Faster

## Build Configuration

### Gradle Files
- `build.gradle.kts` (project)
- `app/build.gradle.kts` (app module)
- `settings.gradle.kts`
- `gradle.properties`

### Key Settings
```kotlin
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}
```

## Dependencies

### Core
- AndroidX Core KTX
- AppCompat
- Material Components
- ConstraintLayout

### Camera
- CameraX (for future use)

### Coroutines
- Kotlinx Coroutines Android

### Testing
- JUnit
- Espresso
- AndroidX Test

## ProGuard

### Configuration
File: `app/proguard-rules.pro`

### Rules
- Keep USB camera classes
- Keep Kotlin coroutines
- Keep app classes
- Remove logging in release

## Continuous Integration

### GitHub Actions Example
```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    - name: Run tests
      run: ./gradlew test
```

## Build Optimization

### Speed Up Builds
1. Enable Gradle daemon
2. Use parallel execution
3. Enable build cache
4. Use incremental compilation

### Reduce APK Size
1. Enable ProGuard
2. Enable resource shrinking
3. Use vector drawables
4. Compress images

## Common Commands

```bash
# Clean build
./gradlew clean

# Build debug
./gradlew assembleDebug

# Build release
./gradlew assembleRelease

# Install debug
./gradlew installDebug

# Run tests
./gradlew test

# Lint check
./gradlew lint

# Generate APK
./gradlew assemble

# Build and install
./gradlew installDebug
```

## Build Output

### Debug APK
- **Location**: `app/build/outputs/apk/debug/`
- **Name**: `app-debug.apk`
- **Size**: ~13 MB

### Release APK
- **Location**: `app/build/outputs/apk/release/`
- **Name**: `app-release-unsigned.apk` or `app-release.apk`
- **Size**: ~2.5 MB

---

**Need help?** Check the [Architecture Guide](ARCHITECTURE.md) or [API Reference](API_REFERENCE.md)
