plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_LOCAL_API_BASE_URL")
    .orElse(providers.gradleProperty("SHAREDHOUSE_DEBUG_API_BASE_URL"))
    .getOrElse("http://10.0.2.2:3000")
val productionApiBaseUrl = "https://houseapi.dohotstudio.com"
val publicApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_PUBLIC_API_BASE_URL")
    .orElse(providers.gradleProperty("SHAREDHOUSE_API_BASE_URL"))
    .orElse(providers.environmentVariable("SHAREDHOUSE_PUBLIC_API_BASE_URL"))
    .orElse(providers.environmentVariable("SHAREDHOUSE_API_BASE_URL"))
    .getOrElse(productionApiBaseUrl)

val releaseStoreFilePath = providers.environmentVariable("SHAREDHOUSE_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("SHAREDHOUSE_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("SHAREDHOUSE_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("SHAREDHOUSE_RELEASE_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val requestedTasks = gradle.startParameter.taskNames
val requestsPublicVariant = requestedTasks.any { it.contains("Public", ignoreCase = true) }
val requestsPublicRelease = requestedTasks.any {
    it.contains("Public", ignoreCase = true) && it.contains("Release", ignoreCase = true)
}

if (requestsPublicVariant) {
    require(publicApiBaseUrl.startsWith("https://")) {
        "Public Android builds require SHAREDHOUSE_PUBLIC_API_BASE_URL with a deployed HTTPS API."
    }
}
if (requestsPublicRelease) {
    require(publicApiBaseUrl == productionApiBaseUrl) {
        "Public release builds must use $productionApiBaseUrl."
    }
    require(hasReleaseSigning) {
        "Public release signing requires SHAREDHOUSE_RELEASE_STORE_FILE, " +
            "SHAREDHOUSE_RELEASE_STORE_PASSWORD, SHAREDHOUSE_RELEASE_KEY_ALIAS and " +
            "SHAREDHOUSE_RELEASE_KEY_PASSWORD environment variables."
    }
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

    signingConfigs {
        if (hasReleaseSigning) {
            create("publicRelease") {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("publicRelease")
            }
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("local") {
            dimension = "environment"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            buildConfigField("String", "API_BASE_URL", "\"$localApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            resValue("string", "app_name", "SharedHouse Local")
        }
        create("public") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$publicApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            resValue("string", "app_name", "SharedHouse")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        val isLocal = variantBuilder.productFlavors.contains("environment" to "local")
        val isPublic = variantBuilder.productFlavors.contains("environment" to "public")
        val hasPublicEndpoint = publicApiBaseUrl.startsWith("https://")

        if (
            (isLocal && variantBuilder.buildType == "release") ||
            (isPublic && !hasPublicEndpoint) ||
            (isPublic && variantBuilder.buildType == "release" && !hasReleaseSigning)
        ) {
            variantBuilder.enable = false
        }
    }
}

dependencies {
    implementation(project(":shared:domain"))
    implementation(project(":shared:network"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    // Lifecycle 2.11 requires compileSdk 37; this app intentionally remains on the approved SDK 36.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

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

tasks.register<Copy>("packageSignedTestingApk") {
    group = "build"
    description = "Compatibility alias for the named local testing APK."
    dependsOn("packageLocalTestingApk")
}

tasks.register<Copy>("packageLocalTestingApk") {
    group = "build"
    description = "Builds the debug-signed local SharedHouse APK."
    dependsOn("assembleLocalDebug")

    from(layout.buildDirectory.file("outputs/apk/local/debug/app-local-debug.apk"))
    into(layout.buildDirectory.dir("outputs/apk/testing"))
    rename("app-local-debug.apk", "SharedHouse-v0.1.0-local-testing-signed.apk")
}

tasks.register<Copy>("packagePublicTestingApk") {
    group = "build"
    description = "Builds the debug-signed SharedHouse APK against the configured public HTTPS API."
    dependsOn("assemblePublicDebug")

    from(layout.buildDirectory.file("outputs/apk/public/debug/app-public-debug.apk"))
    into(layout.buildDirectory.dir("outputs/apk/testing"))
    rename("app-public-debug.apk", "SharedHouse-v0.1.0-public-testing-signed.apk")
}

tasks.register<Copy>("packagePublicReleaseApk") {
    group = "build"
    description = "Builds the production-signed, optimized SharedHouse public release APK."
    dependsOn("assemblePublicRelease")

    from(layout.buildDirectory.file("outputs/apk/public/release/app-public-release.apk"))
    into(layout.buildDirectory.dir("outputs/apk/release"))
    rename("app-public-release.apk", "SharedHouse-v0.1.0-public-release-signed.apk")
}

tasks.register<Copy>("copyPublicReleaseBundle") {
    group = "build"
    description = "Builds the production-signed SharedHouse public Android App Bundle."
    dependsOn("bundlePublicRelease")

    from(layout.buildDirectory.file("outputs/bundle/publicRelease/app-public-release.aab"))
    into(layout.buildDirectory.dir("outputs/bundle/release"))
    rename("app-public-release.aab", "SharedHouse-v0.1.0-public-release-signed.aab")
}
