plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

apply(plugin = "com.google.gms.google-services") // ✅ Ensure Google Services is applied

android {
    namespace = "com.app.secretmessenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.secretmessenger"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "2.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Material Components
    implementation(libs.material)

    // Firebase BOM (Manages Firebase versions)
    implementation(platform("com.google.firebase:firebase-bom:32.2.2"))

    // Firebase Dependencies
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation ("com.google.firebase:firebase-firestore-ktx:24.11.1")
    implementation ("com.google.android.material:material:1.6.0")
//    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
//    implementation ("com.arthenica:mobile-ffmpeg-full:6.0.3")

//    implementation("org.webrtc:google-webrtc:1.0.30000") // Check for the latest version
//    implementation ("io.socket:socket.io-client:2.0.0")

//    implementation ("live.videosdk:rtc-android-sdk:0.1.34") // Latest version as of now, check for updates
//    implementation ("com.amitshekhar.android:android-networking:1.0.2") // For API calls
//    implementation ("com.github.yalantis:ucrop:2.2.8")

    // RecyclerView & Glide for Image Loading
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // CircleImageView (for profile images)
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
