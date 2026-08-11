plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_LOCAL_API_BASE_URL")
    .orElse(providers.gradleProperty("SHAREDHOUSE_DEBUG_API_BASE_URL"))
    .getOrElse("http://10.0.2.2:3000")
val productionApiBaseUrl = "https://houseapi.dohotstudio.com"
val sampleAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val sampleAdMobBannerId = "ca-app-pub-3940256099942544/9214589741"
val appVersionCode = providers.gradleProperty("SHAREDHOUSE_VERSION_CODE")
    .orElse(providers.environmentVariable("SHAREDHOUSE_VERSION_CODE"))
    .getOrElse("1")
    .toIntOrNull()
    ?.takeIf { it > 0 }
    ?: error("SHAREDHOUSE_VERSION_CODE must be a positive integer.")
val appVersionName = providers.gradleProperty("SHAREDHOUSE_VERSION_NAME")
    .orElse(providers.environmentVariable("SHAREDHOUSE_VERSION_NAME"))
    .getOrElse("0.1.0")
require(appVersionName.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.-]+)?$"))) {
    "SHAREDHOUSE_VERSION_NAME must use semantic version syntax, for example 1.0.0."
}
val publicApiBaseUrl = providers.gradleProperty("SHAREDHOUSE_PUBLIC_API_BASE_URL")
    .orElse(providers.gradleProperty("SHAREDHOUSE_API_BASE_URL"))
    .orElse(providers.environmentVariable("SHAREDHOUSE_PUBLIC_API_BASE_URL"))
    .orElse(providers.environmentVariable("SHAREDHOUSE_API_BASE_URL"))
    .getOrElse(productionApiBaseUrl)
val publicAdMobAppId = providers.gradleProperty("SHAREDHOUSE_ADMOB_APP_ID")
    .orElse(providers.environmentVariable("SHAREDHOUSE_ADMOB_APP_ID"))
    .getOrElse(sampleAdMobAppId)
val publicAdMobBannerId = providers.gradleProperty("SHAREDHOUSE_ADMOB_BANNER_ID")
    .orElse(providers.environmentVariable("SHAREDHOUSE_ADMOB_BANNER_ID"))
    .getOrElse(sampleAdMobBannerId)
val publicFirebaseConfig = listOf(
    file("src/public/google-services.json"),
    file("google-services.json"),
).firstOrNull { it.isFile }
val googleServicesEnabled = providers.gradleProperty("SHAREDHOUSE_ENABLE_GOOGLE_SERVICES")
    .orElse(providers.environmentVariable("SHAREDHOUSE_ENABLE_GOOGLE_SERVICES"))
    .getOrElse("false")
    .toBooleanStrictOrNull()
    ?: error("SHAREDHOUSE_ENABLE_GOOGLE_SERVICES must be true or false.")
val productionAdMobConfigured = googleServicesEnabled &&
    publicAdMobAppId != sampleAdMobAppId &&
    publicAdMobBannerId != sampleAdMobBannerId

if (googleServicesEnabled && publicFirebaseConfig != null) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

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
    if (googleServicesEnabled) {
        require(publicFirebaseConfig != null) {
            "Google-enabled public releases require apps/android/app/src/public/google-services.json."
        }
        require(
            publicAdMobAppId.matches(Regex("^ca-app-pub-[0-9]{16}~[0-9]{10}$")) &&
                publicAdMobAppId != sampleAdMobAppId,
        ) {
            "Google-enabled releases require a real SHAREDHOUSE_ADMOB_APP_ID."
        }
        require(
            publicAdMobBannerId.matches(Regex("^ca-app-pub-[0-9]{16}/[0-9]{10}$")) &&
                publicAdMobBannerId != sampleAdMobBannerId,
        ) {
            "Google-enabled releases require a real SHAREDHOUSE_ADMOB_BANNER_ID."
        }
    }
}

android {
    namespace = "com.sharedhouse.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sharedhouse.android"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

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
            manifestPlaceholders["adMobAppId"] = sampleAdMobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$sampleAdMobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$sampleAdMobBannerId\"")
            buildConfigField("boolean", "ADMOB_TEST_MODE", "true")
            buildConfigField("boolean", "ADMOB_CONFIGURED", "false")
            buildConfigField("boolean", "FIREBASE_CONFIGURED", "false")
            resValue("string", "app_name", "SharedHouse Local")
        }
        create("public") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$publicApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            manifestPlaceholders["adMobAppId"] = publicAdMobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$publicAdMobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$publicAdMobBannerId\"")
            buildConfigField(
                "boolean",
                "ADMOB_TEST_MODE",
                (publicAdMobAppId == sampleAdMobAppId || publicAdMobBannerId == sampleAdMobBannerId).toString(),
            )
            buildConfigField("boolean", "ADMOB_CONFIGURED", productionAdMobConfigured.toString())
            buildConfigField(
                "boolean",
                "FIREBASE_CONFIGURED",
                (googleServicesEnabled && publicFirebaseConfig != null).toString(),
            )
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
    // Mobile Ads 1.3.0 still requests WorkManager 2.7.0. Pin the current stable AndroidX runtime:
    // 2.7.0 loses the generated WorkDatabase constructor in optimized AGP 9/R8 builds and crashes
    // before MainActivity. Keep this direct dependency until the ads SDK raises its minimum.
    implementation(libs.androidx.work.runtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)

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

tasks.register<Copy>("packagePublicReleaseApk") {
    group = "build"
    description = "Builds the production-signed, optimized SharedHouse public release APK."
    dependsOn("assemblePublicRelease")

    from(layout.buildDirectory.file("outputs/apk/public/release/app-public-release.apk"))
    into(layout.buildDirectory.dir("outputs/apk/release"))
    rename("app-public-release.apk", "SharedHouse-v$appVersionName-public-release-signed.apk")
}

tasks.register<Copy>("copyPublicReleaseBundle") {
    group = "build"
    description = "Builds the production-signed SharedHouse public Android App Bundle."
    dependsOn("bundlePublicRelease")

    from(layout.buildDirectory.file("outputs/bundle/publicRelease/app-public-release.aab"))
    into(layout.buildDirectory.dir("outputs/bundle/release"))
    rename("app-public-release.aab", "SharedHouse-v$appVersionName-public-release-signed.aab")
}
