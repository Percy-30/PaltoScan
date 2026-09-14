plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.atpdev.paltoscan.core.ml"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    androidResources {
        noCompress += "tflite"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        mlModelBinding = true // Required for TFLite models if used
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    // Hilt para DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // LiteRT (Reemplazo de TFLite)
    implementation(libs.litert)
    implementation(libs.litert.support)
    implementation(libs.litert.gpu)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Timber
    implementation(libs.timber)
}
