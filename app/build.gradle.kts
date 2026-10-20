plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
}

android {
    namespace = "ru.tyupik.sharemylocation"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ru.tyupik.sharemylocation"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // BOM 34.19: firebase-auth 24.2.0 собран Kotlin 2.3 (метаданные читаются компилятором 2.2.x);
    // артефакты BOM 35 собраны Kotlin 2.4 — вернуться на 35 после перехода тулчейна на Kotlin 2.3+
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // Firebase Authentication.
    // ВАЖНО: AAR firebase-auth из maven выпадает из compile classpath пайплайна AGP 9.4
    // (остальные firebase-артефакты при этом работают), поэтому classes.jar подключён напрямую
    // из app/libs, а весь код, трогающий Firebase, вынесен в FirebaseBackend.java.
    // Транзитивные зависимости (firebase-common, play-services-tasks и т.д.) идут через firestore.
    // При обновлении auth — положить новый classes.jar в app/libs.
    implementation(files("libs/firebase-auth-24.2.0-classes.jar"))

    // Firestore: документ пользователя users/{uid}
    implementation(libs.firebase.firestore)

    // Транзитивные зависимости firebase-auth из его POM: files()-jar POM не приносит,
    // без recaptcha падает конструктор FirebaseAuth, остальные нужны firebase-auth в рантайме.
    implementation(libs.recaptcha)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.play.services.integrity)

    // Google Sign-In через Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Tasks.await() для FirebaseBackend
    implementation(libs.kotlinx.coroutines.play.services)

    // Hilt (KSP) + hiltViewModel() для Compose
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation 3
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // collectAsStateWithLifecycle + сериализация ключей Nav3
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
}
