plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.atlasevents"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.atlasevents"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.runtime)
    implementation(libs.room.external.antlr)
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.tracing:tracing:1.2.0")
    implementation(libs.fragment)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-firestore")
    // Corrected line in build.gradle.kts
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    androidTestImplementation("org.mockito:mockito-android:5.11.0")
    androidTestImplementation ("androidx.test:runner:1.5.2")
    androidTestImplementation ("androidx.test:rules:1.5.0")
    androidTestImplementation ("androidx.test.ext:junit:1.1.5")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation ("com.google.firebase:firebase-firestore:24.9.1")
    androidTestImplementation ("androidx.test:core:1.5.0")
    testImplementation("com.google.android.gms:play-services-tasks:18.2.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("androidx.activity:activity-ktx:1.7.0")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:4.0.0")
}