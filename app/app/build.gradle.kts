plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "nz.lonewolf.shark"
    compileSdk = 34

    defaultConfig {
        applicationId = "nz.lonewolf.shark"
        // Shark 6 DiLink 5 head unit is Android 11 (API 30). Keep targetSdk at 30 so
        // Android 12+ Bluetooth and foreground service rules never apply on the ute.
        minSdk = 29
        targetSdk = 30
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild { cmake { cppFlags += listOf("-std=c++17", "-Wall") } }
    }

    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    ndkVersion = "26.3.11579264"

    signingConfigs {
        val storeFilePath = System.getenv("SHARK_SIGNING_STORE_FILE")
        if (!storeFilePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("SHARK_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SHARK_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SHARK_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/NOTICE.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.kotlinx.coroutines.android)
    // dadb declares JUnit as a runtime dependency; keep it out of the APK.
    implementation(libs.dadb) {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "junit")
        exclude(group = "org.opentest4j")
        exclude(group = "org.apiguardian")
    }
}
