import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val propFile = rootProject.file("local.properties")
    if (propFile.exists()) {
        propFile.inputStream().use { load(it) }
    }
}
val openRouterKey: String =
    (localProps.getProperty("OPENROUTER_API_KEY") ?: System.getenv("OPENROUTER_API_KEY") ?: "").trim()
val lemonadeBaseUrl: String =
    (localProps.getProperty("LEMONADE_BASE_URL") ?: System.getenv("LEMONADE_BASE_URL")
        ?: "http://10.0.2.2:13305").trim()

android {
    namespace = "com.example.empati"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.empati"
        minSdk = 24
        targetSdk = 37
        versionCode = 8
        versionName = "4.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${openRouterKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"https://qnvcuhmvrrffzdxxdloc.supabase.co\""
        )
        buildConfigField(
            "String",
            "LEMONADE_BASE_URL",
            "\"${lemonadeBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // Required so Compose Preview rendering can resolve fonts, themes and other resources.
            isIncludeAndroidResources = true
        }
        // Compose Preview Screenshot Testing via AGP test suites (replaces the deprecated
        // `com.android.compose.screenshot` plugin). Sources live in app/src/screenshotTest/kotlin.
        screenshotTests.create("screenshotTest") {
            engineVersion = libs.versions.screenshot.get()
            targetVariants.add("debug")

            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.screenshot.validation.api)
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
