plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize") // Sin especificar versión, usa la del kotlin-android
    alias(libs.plugins.ksp)
    id("kotlin-kapt") // ✅ Agregamos KAPT para FFmpeg y dependencias problemáticas
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.androidx.navigation.safeargs.kotlin)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.atpdev.paltoscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.atpdev.paltoscan"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["android.nativeHeapPointerTagging"] = "false"
    }

    buildTypes {

        release {
            // isMinifyEnabled = true // Para reducir el tamaño del APK en producción
            // isShrinkResources = true
            // BuildConfig.DEBUG es automáticamente 'false' aquí
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug{
            // BuildConfig.DEBUG es automáticamente 'true' aquí
            // Si necesitas forzarlo por alguna razón, puedes añadir:
            // buildConfigField "boolean", "DEBUG", "true"
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
        viewBinding = true
        dataBinding = true
        mlModelBinding = true // Habilitar ML Model Binding
        buildConfig = true
    }
}

dependencies {
    // ====================== CAPA VIEW ======================

    // Reproductor de video (ExoPlayer)
    //implementation("com.google.android.exoplayer:exoplayer:2.18.1")
    // Redes Sociales (Facebook SDK)
    implementation(libs.facebook.android.sdk)

    // Componentes de UI básicos
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Componentes de UI avanzados
    implementation(libs.lottie)
    implementation(libs.styleabletoast)
    implementation(libs.circularprogressbar)
    //implementation("com.mikhaellopez:circularprogressbar:3.1.0") // <-- Aquí lo agregamos

    // Navegación
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Manejo de imágenes
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    implementation(libs.picasso)

    // Cámara
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.library.camerax)

    // ====================== CAPA VIEWMODEL ======================
    // ViewModel y LiveData
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Inyección de dependencias
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // ====================== CAPA MODEL ======================

    // Parseo de HTML (Jsoup)
    implementation(libs.jsoup)
    // Ads (Google Mobile Ads)
    //implementation(libs.play.services.ads)
    // Facturación (Google Play Billing)
    implementation(libs.play.billing.ktx)

    // Base de datos local (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking (Retrofit, OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // TensorFlow Lite (ML) - Módulo Core ML
    implementation(project(":core-ml"))

    // WorkManager (Tareas en segundo plano)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    
    // Gráficos (Dashboard)
    implementation(libs.mpandroidchart)

    // ====================== TESTING ======================
    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.truth)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Instrumentation tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ====================== UTILIDADES ======================
    // Desugaring para APIs modernas
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    
    // Timber para Logs
    implementation(libs.timber)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    // Pra leer pdf
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Location
    implementation(libs.play.services.location)
}