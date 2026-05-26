plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.catedra.apporgartistas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.catedra.apporgartistas"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {

    implementation(
        platform(
            "com.google.firebase:firebase-bom:33.5.1"
        )
    )

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)



    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(
        "com.google.android.gms:play-services-auth:21.2.0"
    )

    implementation(
        "androidx.work:work-runtime-ktx:2.9.0"
    )

    implementation(
        "com.google.android.gms:play-services-maps:18.2.0"
    )

    implementation(
        "com.google.android.gms:play-services-location:21.0.1"
    )

    implementation(
        "androidx.camera:camera-camera2:1.2.3"
    )

    implementation(
        "androidx.camera:camera-lifecycle:1.2.3"
    )

    implementation(
        "androidx.camera:camera-view:1.2.3"
    )
}