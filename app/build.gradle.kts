plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

apply(plugin = "kotlin-kapt")

android {
    namespace = "com.example.akiportal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.akiportal"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 🔧 Jetpack Compose
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui:1.8.1")
    implementation("androidx.compose.ui:ui-text:1.8.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.8.1")
    implementation("androidx.compose.foundation:foundation:1.8.1")
    implementation("androidx.compose.foundation:foundation-layout:1.8.1")
    implementation("androidx.compose.material:material:1.8.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.accompanist:accompanist-pager:0.30.1")
    implementation ("androidx.compose.animation:animation:1.8.1")

    // Animasyonlar
    // 🧠 Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // 🗄️ Room
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

    // 🔐 Core
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha07")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")


    // ☁️ Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx") // <-- STORAGE EKLENDİ!
    implementation("com.google.firebase:firebase-analytics")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3") // <-- await() için


    // P2P HTTP sunucu (NanoHTTPD)
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    // HTTP istemcisi (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Kotlin Coroutines (zaten varsa tekrar ekleme)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // 📍 Harita & ML Kit
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.maps.android:maps-compose:2.11.0")

    // 📦 Diğer
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.opencsv:opencsv:5.7.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")

    // 🧪 Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.8.1")
}
