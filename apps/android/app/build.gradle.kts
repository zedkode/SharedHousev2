plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.sharedhouse.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sharedhouse.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val debugApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_DEBUG_API_BASE_URL")
                .getOrElse("http://10.0.2.2:3000")
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            val releaseApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_API_BASE_URL")
                .getOrElse("https://api.sharedhouse.invalid")
            require(releaseApiBaseUrl.startsWith("https://")) {
                "SHAREDHOUSE_API_BASE_URL must use HTTPS for release builds."
            }
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":shared:domain"))
    implementation(project(":shared:network"))
    implementation(libs.androidx.activity.compose)
    // Lifecycle 2.11 requires compileSdk 37; this app intentionally remains on the approved SDK 36.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.ktor.client.okhttp)
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
