plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

android {
    namespace = "com.example.alarm_clock_2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.alarm_clock_2"
        // WheelPickerCompose requires API 23+
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "0.9.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val properties = Properties()
            val localPropertiesFile = project.rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                properties.load(localPropertiesFile.inputStream())
            }

            keyAlias = properties.getProperty("key.alias")
            keyPassword = properties.getProperty("key.password")
            storeFile = file(properties.getProperty("keystore.path"))
            storePassword = properties.getProperty("keystore.password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.foundation)
    implementation(libs.material3.window.size)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    // Hilt WorkManager
    implementation(libs.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // DataStore & WorkManager
    implementation(libs.datastore.preferences)
    implementation(libs.workmanager.ktx)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Lunar calendar
    implementation(libs.lunar)
    // Java 8+ Time API desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.serialization.json)
    // Jetpack Compose wheel time picker (CommandIron)
    implementation("com.github.commandiron:WheelPickerCompose:1.1.3")
}