plugins {
    id("com.android.library")
    id("kotlin-android")
    id("maven-publish")
}

android {
    namespace = "com.app.screenshare"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("com.google.android.material:material:1.12.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("pub.devrel:easypermissions:3.0.0")
    implementation("com.opentok.android:opentok-android-sdk:2.29.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")


}
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.bharatKumawatds"
            artifactId = "screenShare"
            version = "0.0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}