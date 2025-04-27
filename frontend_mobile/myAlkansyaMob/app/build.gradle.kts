plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myalkansyamobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myalkansyamobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    
    // Add this packaging block to handle duplicate files
    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Facebook SDK
    implementation("com.facebook.android:facebook-login:16.2.0")
    implementation("com.facebook.android:facebook-android-sdk:16.2.0")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // DataStore (for session management)
    implementation("androidx.datastore:datastore-preferences:1.1.0")

    // Glide for image loading
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // Chart library for data visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Google Sheets API dependencies
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220927-2.0.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    
    // Keep CSV library as fallback
    implementation("com.github.doyaaaaaken:kotlin-csv:1.9.2")

    // UI components
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.google.android.material:material:1.9.0")
    
    // Add desugaring support for Java 8+ APIs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}