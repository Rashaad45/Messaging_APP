plugins {
    // AGP 9 compiles Kotlin itself ("built-in Kotlin"), so there is no
    // separate org.jetbrains.kotlin.android plugin here.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    // Reads app/google-services.json and generates Firebase config resources
    // (incl. R.string.default_web_client_id used for Google Sign-In).
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.rashaad.messenger"
    // compileSdk only controls which APIs the compiler can see; current AndroidX
    // releases require 37. targetSdk (runtime behaviour) stays at 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rashaad.messenger"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Robolectric needs merged Android resources to run Compose tests on the JVM.
            isIncludeAndroidResources = true
            // Windows Smart App Control blocks Robolectric's unsigned native DLLs (runtime, Conscrypt),
            // so on Windows we use its pure-Java implementations. CI (Linux) keeps the
            // default native mode, which is closer to a real device. See docs/DECISIONS.md D-012.
            if (System.getProperty("os.name").startsWith("Windows")) {
                all {
                    it.systemProperty("robolectric.graphicsMode", "LEGACY")
                    it.systemProperty("robolectric.sqliteMode", "LEGACY")
                    it.systemProperty("robolectric.conscryptMode", "OFF")
                }
            }
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = true
        // Newer-version checks would fail CI every time a library ships a release.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        // targetSdk 36 is deliberate (matches the test device, meets Play's requirement);
        // see docs/DECISIONS.md D-002.
        disable += "OldTargetApi"
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    parallel = true
}

ktlint {
    android.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
