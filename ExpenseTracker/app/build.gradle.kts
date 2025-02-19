plugins {
    alias(libs.plugins.android.application)

    // Google Services Plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 31
        targetSdk = 35
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")

    // Google Material Design
    implementation("com.google.android.material:material:1.11.0")

    // ✅ Use the Firebase BoM to manage versions automatically
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))

    // ✅ Firebase Dependencies (without versions)
    implementation("com.google.firebase:firebase-auth") // Firebase Authentication
    implementation("com.google.firebase:firebase-firestore") // Firestore Database
    implementation("com.google.firebase:firebase-analytics") // Firebase Analytics

    // ✅ Google Sign-In SDK
    implementation("com.google.android.gms:play-services-auth:20.7.0")

}

// ✅ Ensure Google Services Plugin is applied at the bottom
apply(plugin = "com.google.gms.google-services")
